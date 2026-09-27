package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Job implements Serializable {
    private final String jobId;
    private final String clientId;
    private final String javaCommand;
    private final String jarFileName;
    private final List<String> inputFiles;
    private final List<String> expectedOutputs;

    private JobStatus status;
    private String assignedWorkstationId;

    private final long arrivalTime;
    private long startTime;
    private long finishTime;

    private String executionLog;
    private int exitCode;

    public Job(String jobId, String clientId, String javaCommand, String jarFileName, List<String> inputFiles, List<String> expectedOutputs) {
        this.jobId = jobId;
        this.clientId = clientId;
        this.javaCommand = javaCommand;
        this.jarFileName = jarFileName;
        this.inputFiles = inputFiles != null ? new ArrayList<>(inputFiles) : new ArrayList<>();
        this.expectedOutputs = expectedOutputs != null ? new ArrayList<>(expectedOutputs) : new ArrayList<>();

        this.status = JobStatus.READY;
        this.assignedWorkstationId = "N/A";
        this.arrivalTime = System.currentTimeMillis();
        this.startTime = 0;
        this.finishTime = 0;
        this.executionLog = "";
        this.exitCode = -1;
    }

    public static String formatTimestamp(long time) {
        if (time <= 0) return "-";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
    }

    public String getJobId() { return jobId; }
    public String getClientId() { return clientId; }
    public String getJavaCommand() { return javaCommand; }
    public String getJarFileName() { return jarFileName; }
    public List<String> getInputFiles() { return inputFiles; }
    public List<String> getExpectedOutputs() { return expectedOutputs; }

    public synchronized JobStatus getStatus() { return status; }
    public synchronized void setStatus(JobStatus status) { this.status = status; }

    public synchronized String getAssignedWorkstationId() { return assignedWorkstationId; }
    public synchronized void setAssignedWorkstationId(String id) { this.assignedWorkstationId = id; }

    public long getArrivalTime() { return arrivalTime; }

    public synchronized long getStartTime() { return startTime; }
    public synchronized void  setStartTime(long startTime) { this.startTime = startTime; }

    public synchronized long getFinishTime() { return finishTime; }
    public synchronized void setFinishTime(long finishTime) { this.finishTime = finishTime; }

    public synchronized String getExecutionLog() { return executionLog; }
    public synchronized void setExecutionLog(String log) { this.executionLog = log; }

    public synchronized int getExitCode() { return exitCode; }
    public synchronized void setExitCode(int exitCode) { this.exitCode = exitCode; }
}
