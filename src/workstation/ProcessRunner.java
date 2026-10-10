package workstation;

import common.Constants;
import common.Job;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessRunner {
    private final File jobDir;
    private final File lindaJarFile;

    public ProcessRunner(File jobDir, File lindaJarFile) {
        this.jobDir = jobDir;
        this.lindaJarFile = lindaJarFile;
    }

    public int execute(Job job) throws IOException, InterruptedException {
        List<String> commandList = new ArrayList<>();

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        commandList.add(javaBin);

        File userJar = new File(jobDir, job.getJarFileName());
        String classpath = userJar.getAbsolutePath();
        if (lindaJarFile != null && lindaJarFile.exists()) {
            classpath += File.separator + lindaJarFile.getAbsolutePath();
        }
        commandList.add("-cp");
        commandList.add(classpath);

        if (job.getJavaCommand() != null && !job.getJavaCommand().trim().isEmpty()) {
            String[] userArgs = job.getJavaCommand().trim().split("\\s+");
            for (String arg : userArgs) {
                commandList.add(arg);
            }
        }

        if (lindaJarFile != null && lindaJarFile.exists()) {
            commandList.add(lindaJarFile.getAbsolutePath());
        }

        System.out.println("Executing command: " + String.join(" ", commandList));

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(jobDir);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder logBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logBuilder.append(line).append("\n");
                System.out.println(Constants.JOB_ID_PREFIX + job.getJobId() + ": " + line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println(Constants.JOB_ID_PREFIX + job.getJobId() + " finished with exit code: " + exitCode);

        job.setExecutionLog(logBuilder.toString());
        job.setExitCode(exitCode);

        return exitCode;
    }
}
