package common;

import java.io.*;

public final class FileTransferUtil {
    private FileTransferUtil() {}

    public static void sendFile(File file, OutputStream out) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File doesn't exist: " + file.getAbsolutePath());
        }

        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(file.getName());
        dos.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[Constants.FILE_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        dos.flush();
    }

    public static File receiveFile(InputStream in, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        DataInputStream dis = new DataInputStream(in);
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        fileName = new File(fileName).getName(); // path traversal attack

        File outputFile = new File(targetDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[Constants.FILE_BUFFER_SIZE];
            long totalRead = 0;
            int bytesRead;

            while (totalRead < fileSize &&
                    (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
        return outputFile;
    }
}
