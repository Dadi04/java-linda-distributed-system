package client;

import common.Constants;
import common.Job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigFileParser {
    public static class ParsedConfig {
        public final Job job;
        public final File jarFile;
        public final List<File> inputFiles;

        public ParsedConfig(Job job, File jarFile, List<File> inputFiles) {
            this.job = job;
            this.jarFile = jarFile;
            this.inputFiles = inputFiles;
        }
    }

    public static ParsedConfig parse(File configFile, String clientId) throws Exception {
        if (!configFile.exists() || !configFile.isFile()) {
            throw new FileNotFoundException("Config file " + configFile.getAbsolutePath() + " does not exist or is not a file");
        }

        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            props.load(reader);
        }

        String jarPath = props.getProperty("jar");
        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Jar property is not set");
        }
        File jarFile = new File(jarPath.trim());
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new FileNotFoundException("Jar file " + jarFile.getAbsolutePath() + " does not exist or is not a file");
        }

        String command = props.getProperty("command", "").trim();

        String inputsStr = props.getProperty("inputs", "").trim();
        List<String> inputNames = new ArrayList<>();
        List<File> inputFiles = new ArrayList<>();

        if (!inputsStr.isEmpty()) {
            String[] parts = inputsStr.split(",");
            if (parts.length > Constants.MAX_INPUT_FILES) {
                throw new IllegalArgumentException("Maximum allowed number of input files is " + Constants.MAX_INPUT_FILES);
            }

            for (String p : parts) {
                String path = p.trim();
                if (!path.isEmpty()) {
                    File f = new File(path);
                    if (!f.exists() || !f.isFile()) {
                        throw new FileNotFoundException("Input file " + path + " does not exist or is not a file");
                    }
                    inputFiles.add(f);
                    inputNames.add(f.getName());
                }
            }
        }

        String outputsStr = props.getProperty("outputs", "").trim();
        List<String> outputNames = new ArrayList<>();

        if (!outputsStr.isEmpty()) {
            String[] parts = outputsStr.split(",");
            if (parts.length > Constants.MAX_OUTPUT_FILES) {
                throw new IllegalArgumentException("Maximum allowed number of output files is " + Constants.MAX_OUTPUT_FILES);
            }

            for (String p : parts) {
                String name = p.trim();
                if (!name.isEmpty()) {
                    outputNames.add(new File(name).getName());
                }
            }
        }

        String jobId = "JOB-" + System.currentTimeMillis();

        Job job = new Job(jobId, clientId, command, jarFile.getName(), inputNames, outputNames);

        return new ParsedConfig(job, jarFile, inputFiles);
    }
}
