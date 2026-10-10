package client;

import common.Constants;
import common.Job;

import java.io.File;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        String serverHost = Constants.DEFAULT_SERVER_HOST;
        int serverPort = Constants.DEFAULT_SERVER_PORT;

        if (args.length >= 2) {
            serverHost = args[0];
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ", using default port: " + serverPort);
            }
        }

        ClientService clientService = new ClientService(serverHost, serverPort);
        String clientId = Constants.CLIENT_ID_PREFIX + System.currentTimeMillis() % 10000;

        if (args.length >= 3) {
            handleBatchCommands(args, clientService, clientId);
            return;
        }

        runInteractiveMenu(clientService, clientId, serverHost, serverPort);
    }

    private static void runInteractiveMenu(ClientService clientService, String clientId, String host, int port) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Connecting to the server: " + host + ":" + port);
        System.out.println("Client ID: " + clientId);

        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Start a new job from config file");
            System.out.println("2. Check job status");
            System.out.println("3. Download results and output files");
            System.out.println("4. Stop (abort) the job");
            System.out.println("5. Quit");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter the path to the config file: ");
                    String configPath = scanner.nextLine().trim();
                    try {
                        ConfigFileParser.ParsedConfig config = ConfigFileParser.parse(new File(configPath), clientId);
                        String jobId = clientService.submitJob(config.job, config.jarFile, config.inputFiles);
                        System.out.println("Job " + jobId + " successfully submitted");
                    } catch (Exception e) {
                        System.err.println("Failed to parse config file: " + e.getMessage());
                    }
                    break;
                case "2":
                    System.out.print("Enter ID of the job you wish to check status on: ");
                    String statusId = scanner.nextLine().trim();
                    try {
                        Job job = clientService.getJobStatus(statusId);
                        System.out.println("Job ID: " + job.getJobId());
                        System.out.println("Status: " + job.getStatus());
                        System.out.println("Workstation: " + job.getAssignedWorkstationId());
                        System.out.println("Arrival time: " + job.getArrivalTime());
                        System.out.println("Start time: " + job.getStartTime());
                        System.out.println("Finish time: " + job.getFinishTime());
                    } catch (Exception e) {
                        System.err.println("Failed to get the status of the job: " + e.getMessage());
                    }
                    break;
                case "3":
                    System.out.print("Enter ID of the job you wish to download: ");
                    String resultId = scanner.nextLine().trim();
                    System.out.print("Folder for downloads (default: downloads): ");
                    String downloadDirStr = scanner.nextLine().trim();
                    if (downloadDirStr.isEmpty()) downloadDirStr = Constants.DEFAULT_CLIENT_DOWNLOAD_DIR;

                    try {
                        File downloadDir = new File(downloadDirStr);
                        Job job = clientService.downloadResults(resultId, downloadDir);
                        System.out.println("Job " + resultId + " successfully downloaded to: " + downloadDir.getAbsolutePath());
                        System.out.println("Exit code: " + job.getExitCode());
                        System.out.println("Job execution log");
                        System.out.println(downloadDir.getAbsolutePath());
                    } catch (Exception e) {
                        System.err.println("Failed to download results: " + e.getMessage());
                    }
                    break;
                case "4":
                    System.out.print("Enter ID of the job you wish to abort: ");
                    String abortId = scanner.nextLine().trim();
                    try {
                        boolean ok = clientService.abortJob(abortId);
                        System.out.println(ok ? "Job aborted successfully" : "Job abort failed");
                    } catch (Exception e) {
                        System.err.println("Failed to abort job: " + e.getMessage());
                    }
                    break;
                case "5":
                    System.out.println("Goodbye");
                    return;
                default:
                    System.out.println("Invalid choice, try again");
            }
        }
    }

    private static void handleBatchCommands(String[] args, ClientService clientService, String clientId) {
        String command = args[2];
        try {
            if ("submit".equalsIgnoreCase(command) && args.length >= 4) {
                File configFile = new File(args[3]);
                ConfigFileParser.ParsedConfig config = ConfigFileParser.parse(configFile, clientId);
                String jobId = clientService.submitJob(config.job, config.jarFile, config.inputFiles);
                System.out.println("Job " + jobId + " successfully submitted");
            } else if ("status".equalsIgnoreCase(command) && args.length >= 4) {
                Job job = clientService.getJobStatus(args[3]);
                System.out.println("Status of job " + job.getJobId() + ": " + job.getStatus());
            } else if ("results".equalsIgnoreCase(command) && args.length >= 4) {
                String jobId = args[3];
                String dir = args.length >= 5 ? args[4] : Constants.DEFAULT_CLIENT_DOWNLOAD_DIR;
                Job job = clientService.downloadResults(jobId, new File(dir));
                System.out.println("Job " + jobId + " successfully downloaded to: " + dir);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse config file: " + e.getMessage());
            System.exit(1);
        }
    }
}
