package server;

import common.*;
import linda.LindaService;
import linda.TupleSpace;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CentralServer implements Runnable {
    private final int serverPort;

    private final LindaService lindaService;
    private final JobManager jobManager;
    private final WorkstationManager workstationManager;
    private final JobScheduler jobScheduler;

    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public CentralServer(int serverPort, int lindaPort, File storageDir) {
        this.serverPort = serverPort;

        this.lindaService = new LindaService(lindaPort, new TupleSpace());

        this.workstationManager = new WorkstationManager(this::handleWorkstationFailure);
        this.jobManager = new JobManager(storageDir);
        this.jobScheduler = new JobScheduler(jobManager, workstationManager);
    }

    @Override
    public void run() {
        new Thread(lindaService, "LindaServiceThread").start();
        new Thread(jobScheduler, "JobSchedulerThread").start();

        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("CentralServer: Server listening on port " + serverPort);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    clientPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (!running) break;
                    System.err.println("CentralServer: Error accepting client connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("CentralServer: Could not listen on port " + serverPort + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) in.readObject();
            if (msg == null) return;

            switch (msg.getType()) {
                case WORKSTATION_REGISTER: {
                    WorkstationInfo wsi = (WorkstationInfo) msg.getData();
                    workstationManager.registerWorkstation(wsi);
                    out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, "Registration successful"));
                    out.flush();
                    break;
                }
                case JOB_COMPLETED: {
                    handleJobCompleted(in, out, (Job) msg.getData());
                    break;
                }
                case JOB_EXECUTION_FAILED: {
                    Job failedJob = (Job) msg.getData();
                    jobManager.updateJobStatus(failedJob.getJobId(), JobStatus.FAILED, failedJob.getAssignedWorkstationId());
                    WorkstationInfo wsFailed = workstationManager.getWorkstation(failedJob.getAssignedWorkstationId());
                    if (wsFailed != null) wsFailed.decrementActiveJobsCount();

                    out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, "Failure recorded"));
                    out.flush();
                    break;
                }
                case CLIENT_SUBMIT_JOB: {
                    handleClientSubmitJob(in, out, (Job) msg.getData());
                    break;
                }
                case CLIENT_GET_STATUS: {
                    String statusJobId = (String) msg.getData();
                    Job requestedJob = jobManager.getJob(statusJobId);
                    out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, requestedJob));
                    out.flush();
                    break;
                }
                case CLIENT_GET_RESULTS: {
                    handleClientGetResults(in, out, (String) msg.getData());
                    break;
                }
                case CLIENT_ABORT_JOB: {
                    String abortedJobId = (String) msg.getData();
                    jobManager.updateJobStatus(abortedJobId, JobStatus.ABORTED, null);
                    out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, "Job aborted"));
                    out.flush();
                    break;
                }
                default: {
                    out.writeObject(new Message(MessageType.SERVER_RESPONSE_ERROR, "Invalid message type: " + msg.getType()));
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("CentralServer: Error handling client connection: " + e.getMessage());
        }
    }

    private void handleWorkstationFailure(WorkstationInfo failedWorkstation) {
        System.err.println("CentralServer: Workstation " + failedWorkstation.getId() + " crashed");

        List<Job> affectedJobs = jobManager.getJobsRunningOnWorkstation(failedWorkstation.getId());
        for (Job job : affectedJobs) {
            System.err.println("CentralServer: Job " + job.getJobId() + " stopped because workstation " + job.getAssignedWorkstationId() + " failed");
            jobManager.requeueJob(job);
        }
    }

    private void handleClientSubmitJob(ObjectInputStream in, ObjectOutputStream out, Job job) throws IOException {
        File inputDir = jobManager.getJobInputDirectory(job.getJobId());
        FileTransferUtil.receiveFile(in, inputDir);

        for (int i = 0; i < job.getInputFiles().size(); i++) {
            FileTransferUtil.receiveFile(in, inputDir);
        }

        jobManager.submitJob(job);

        out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, job.getJobId(), "Job successfully received"));
        out.flush();
    }

    private void handleJobCompleted(ObjectInputStream in, ObjectOutputStream out, Job finishedJob) throws IOException {
        File outputDir = jobManager.getJobOutputDirectory(finishedJob.getJobId());

        for (int i = 0; i < finishedJob.getExpectedOutputs().size(); i++) {
            FileTransferUtil.receiveFile(in, outputDir);
        }

        jobManager.updateJobStatus(finishedJob.getJobId(), JobStatus.DONE, finishedJob.getAssignedWorkstationId());
        Job localJob = jobManager.getJob(finishedJob.getJobId());
        if (localJob != null) {
            localJob.setExecutionLog(finishedJob.getExecutionLog());
            localJob.setExitCode(finishedJob.getExitCode());
        }

        WorkstationInfo workstation = workstationManager.getWorkstation(finishedJob.getAssignedWorkstationId());
        if (workstation != null) {
            workstation.decrementActiveJobsCount();
        }

        out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, "Results successfully saved"));
        out.flush();
    }

    private void handleClientGetResults(ObjectInputStream in, ObjectOutputStream out, String jobId) throws IOException {
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            out.writeObject(new Message(MessageType.SERVER_RESPONSE_ERROR, "Job with ID " + jobId + " doesn't exist"));
            out.flush();
            return;
        }

        if (job.getStatus() != JobStatus.DONE) {
            out.writeObject(new Message(MessageType.SERVER_RESPONSE_ERROR, "Job with ID " + jobId + " is not done, status: " + job.getStatus()));
            out.flush();
            return;
        }

        out.writeObject(new Message(MessageType.SERVER_RESPONSE_OK, job));
        out.flush();

        File outputDir = jobManager.getJobOutputDirectory(jobId);
        for (String outputFileName : job.getExpectedOutputs()) {
            File outFile = new File(outputDir, outputFileName);
            if (outFile.exists()) { // potential problem if output file doesn't exist
                FileTransferUtil.sendFile(outFile, out);
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        jobScheduler.stop();
        workstationManager.shutdown();
        lindaService.stop();
        clientPool.shutdown();
    }
}