package workstation;

import common.*;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkstationNode implements Runnable {
    private final String workstationId;
    private final String serverHost;
    private final int serverPort;
    private final int workstationPort;
    private final int maxParallelJobs;
    private final File storageDir;
    private final File lindaJarFile;

    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService jobThreadPool;

    public WorkstationNode(String serverHost, int serverPort, int workstationPort, int maxParallelJobs, File storageDir, File lindaJarFile) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.workstationPort = workstationPort;
        this.maxParallelJobs = maxParallelJobs;
        this.storageDir = storageDir;
        this.lindaJarFile = lindaJarFile;

        this.workstationId = "WS-" + workstationPort;
        this.jobThreadPool = Executors.newFixedThreadPool(maxParallelJobs);

        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
    }

    @Override
    public void run() {
        registerOnCentralServer();

        try {
            serverSocket = new ServerSocket(workstationPort);
            System.out.println("WorkstationNode: Listening on port " + workstationPort);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    handleIncomingConnection(socket);
                } catch (IOException e) {
                    if (!running) break;
                    System.err.println("WorkstationNode: Error while listening on port " + workstationPort);
                }
            }
        } catch (IOException e) {
            System.err.println("WorkstationNode: Could not listen on port " + workstationPort + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void registerOnCentralServer() {
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            String osName = System.getProperty("os.name");
            String javaVersion = System.getProperty("java.version");

            WorkstationInfo info = new WorkstationInfo(
                    workstationId,
                    "localhost",
                    workstationPort,
                    osName,
                    javaVersion,
                    maxParallelJobs
            );

            out.writeObject(new Message(MessageType.WORKSTATION_REGISTER, info));
            out.flush();

            Message ack = (Message) in.readObject();
            System.out.println("WorkstationNode: Received ACK from Central Server: " + ack.getDescription());
        } catch (Exception e) {
            System.err.println("WorkstationNode: Error while registering on central server: " + e.getMessage());
        }
    }

    private void handleIncomingConnection(Socket socket) {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            Message msg = (Message) in.readObject();
            if (msg == null) return;

            if (msg.getType() == MessageType.HEARTBEAT_PING) {
                out.writeObject(new Message(MessageType.WORKSTATION_HEARTBEAT_ACK, null));
                out.flush();
                socket.close();
            } else if (msg.getType() == MessageType.DISPATCH_JOB) {
                Job job = (Job) in.readObject();
                System.out.println("WorkstationNode: Received job: " + job.getJobId());

                File jobDir = new File(storageDir, "job_" + job.getJobId());
                if (!jobDir.exists()) jobDir.mkdir();

                FileTransferUtil.receiveFile(in, jobDir);

                for (int i = 0; i < job.getInputFiles().size(); i++) {
                    FileTransferUtil.receiveFile(in, jobDir);
                }

                out.writeObject(new Message(MessageType.JOB_STARTED, job.getJobId()));
                out.flush();
                socket.close();

                jobThreadPool.submit(() -> executeJob(job, jobDir));
            }
        } catch (Exception e) {
            System.err.println("WorkstationNode: Error while handling Incoming Connection: " + e.getMessage());
        }
    }

    private void executeJob(Job job, File jobDir) {
        try {
            ProcessRunner runner = new ProcessRunner(jobDir, lindaJarFile);
            int exitCode = runner.execute(job);

            reportJobCompletion(job, jobDir, exitCode == 0);
        } catch (Exception e) {
            System.err.println("WorkstationNode: Error while executing job: " + e.getMessage());
            reportJobFailure(job, e.getMessage());
        }
    }

    private void reportJobCompletion(Job job, File jobDir, boolean success) {
        try (Socket socket = new Socket(serverHost, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            job.setAssignedWorkstationId(workstationId);

            if (success) {
                out.writeObject(new Message(MessageType.JOB_COMPLETED, job));
                out.flush();

                for (String outputFileName : job.getExpectedOutputs()) {
                    File outFile = new File(jobDir, outputFileName);
                    if (outFile.exists()) {
                        FileTransferUtil.sendFile(outFile, out);
                    } else {
                        System.err.println("WorkstationNode: Could not find output file: " + outputFileName);
                    }
                }
            } else {
                out.writeObject(new Message(MessageType.JOB_EXECUTION_FAILED, job));
                out.flush();
            }

            Message ack = (Message) in.readObject();
            System.out.println("WorkstationNode: Received ACK from Central Server: " + ack.getDescription());
        } catch (Exception e) {
            System.err.println("WorkstationNode: Error while reporting job completion: " + e.getMessage());
        }
    }

    private void reportJobFailure(Job job, String message) {
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            job.setAssignedWorkstationId(workstationId);
            job.setExecutionLog("Error in workstation: " + message);
            job.setExitCode(-1);

            out.writeObject(new Message(MessageType.JOB_EXECUTION_FAILED, job));
            out.flush();
        } catch (Exception ignored) {}
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        jobThreadPool.shutdownNow();
    }
}
