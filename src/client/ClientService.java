package client;

import common.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ClientService {
    private final String serverHost;
    private final int serverPort;

    public ClientService(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket();

        socket.connect(new InetSocketAddress(serverHost, serverPort), 5000);
        socket.setSoTimeout(10000);
        return socket;
    }

    public String submitJob(Job job, File jarFile, List<File> inputFiles) throws Exception {
        try (Socket socket = createSocket();
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(new Message(MessageType.CLIENT_SUBMIT_JOB, job));
            out.flush();

            FileTransferUtil.sendFile(jarFile, out);

            for (File inputFile : inputFiles) {
                FileTransferUtil.sendFile(inputFile, out);
            }

            Message response = (Message) in.readObject();
            if (response != null && response.getType() == MessageType.SERVER_RESPONSE_OK) {
                return (String) response.getData();
            } else {
                throw new Exception("Central server denied job: " + (response != null ? response.getDescription() : "No answer"));
            }
        } catch (StreamCorruptedException e) {
            throw new Exception("The connected server does not use correct protocol");
        }
    }

    public Job getJobStatus(String jobId) throws Exception {
        try (Socket socket = createSocket();
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(new Message(MessageType.CLIENT_GET_STATUS, jobId));
            out.flush();

            Message response = (Message) in.readObject();
            if (response != null && response.getType() == MessageType.SERVER_RESPONSE_OK) {
                return (Job) response.getData();
            } else {
                throw new Exception("Error while getting job status: " + (response != null ? response.getDescription() : "No answer"));
            }
        } catch (StreamCorruptedException e) {
            throw new Exception("The connected server does not use correct protocol");
        }
    }

    public Job downloadResults(String jobId, File downloadDir) throws Exception {
        if (!downloadDir.exists()) downloadDir.mkdirs();

        try (Socket socket = createSocket();
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(new Message(MessageType.CLIENT_GET_RESULTS, jobId));
            out.flush();

            Message response = (Message) in.readObject();
            if (response == null || response.getType() != MessageType.SERVER_RESPONSE_OK) {
                throw new Exception("Server did not send the results: " + (response != null ? response.getDescription() : "No answer"));
            }

            Job completedJob = (Job) response.getData();

            for (int i = 0; i < completedJob.getExpectedOutputs().size(); i++) {
                File savedFile = FileTransferUtil.receiveFile(in, downloadDir);
                System.out.println("ClientService: Output file downloaded: " + savedFile.getName());
            }

            return completedJob;
        } catch (StreamCorruptedException e) {
            throw new Exception("The connected server does not use correct protocol");
        }
    }

    public boolean abortJob(String jobId) throws Exception {
        try (Socket socket = createSocket();
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(new Message(MessageType.CLIENT_ABORT_JOB, jobId));
            out.flush();

            Message response = (Message) in.readObject();
            return response != null && response.getType() == MessageType.SERVER_RESPONSE_OK;
        } catch (StreamCorruptedException e) {
            throw new Exception("The connected server does not use correct protocol");
        }
    }
}
