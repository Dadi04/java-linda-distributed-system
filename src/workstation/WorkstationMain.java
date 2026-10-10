package workstation;

import common.Constants;

import java.io.File;

public class WorkstationMain {
    public static void main(String[] args) {
        String serverHost = Constants.DEFAULT_SERVER_HOST;
        int serverPort = Constants.DEFAULT_SERVER_PORT;
        int workstationPort = Constants.DEFAULT_WORKSTATION_PORT;
        int maxParallelJobs = Constants.DEFAULT_MAX_PARALLEL_JOBS;
        String lindaJarPath = Constants.DEFAULT_LINDA_JAR_PATH;

        if (args.length >= 1) {
            serverHost = args[0];
        }
        if (args.length >= 2) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ", using default port: " + serverPort);
            }
        }
        if (args.length >= 3) {
            try {
                workstationPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[2] + ", using default port: " + workstationPort);
            }
        }
        if (args.length >= 4) {
            try {
                maxParallelJobs = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid max parallel: " + args[3] + ", using default " + maxParallelJobs);
            }
        }
        if (args.length >= 5) {
            lindaJarPath = args[4];
        }

        File storageDir = new File(Constants.DEFAULT_WORKSTATION_STORAGE_PREFIX + workstationPort);
        File lindaJarFile = new File(lindaJarPath);

        System.out.println("Workstation ID: " + Constants.WORKSTATION_ID_PREFIX + workstationPort);
        System.out.println("Central server address: " + serverHost + ":" + serverPort);
        System.out.println("Local port: " + workstationPort);
        System.out.println("Parallel jobs capacity: " + maxParallelJobs);
        System.out.println("Folder for jobs: " + storageDir.getAbsolutePath());
        System.out.println("Linda jar path: " + lindaJarFile.getAbsolutePath());

        WorkstationNode node = new WorkstationNode(
                serverHost,
                serverPort,
                workstationPort,
                maxParallelJobs,
                storageDir,
                lindaJarFile
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down workstation node");
            node.stop();
            System.out.println("Workstation node successfully stopped");
        }));

        node.run();
    }
}
