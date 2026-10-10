package server;

import common.Constants;

import java.io.File;

public class ServerMain {
    public static void main(String[] args) {
        int serverPort = Constants.DEFAULT_SERVER_PORT;
        int lindaPort = Constants.DEFAULT_LINDA_PORT;
        String storagePath = Constants.DEFAULT_SERVER_STORAGE_DIR;

        if (args.length >= 1) {
            try {
                serverPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid server port number, using default: " + serverPort);
            }
        }
        if  (args.length >= 2) {
            try {
                lindaPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid Linda port number, using default: " + lindaPort);
            }
        }
        if  (args.length >= 3) {
            storagePath = args[2];
        }

        File storageDir = new File(storagePath);
        System.out.println("Server port: " + serverPort);
        System.out.println("Linda port for tuple space: " + lindaPort);
        System.out.println("Folder for file storage: " + storageDir.getAbsolutePath());

        CentralServer server = new CentralServer(serverPort, lindaPort, storageDir);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down central server");
            server.stop();
            System.out.println("Central server successfully stopped");
        }));

        server.run();
    }
}
