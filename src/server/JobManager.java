package server;

import common.Job;
import common.JobStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JobManager {
    private final Map<String, Job> allJobs = new ConcurrentHashMap<>();
    private final Queue<Job> readyJobsQueue = new ConcurrentLinkedQueue<>();
    private final File storageDir;

    public interface JobLogListener {
        void onJobLog(String logEntry);
    }
    private JobLogListener logListener;

    public JobManager(File storageDir) {
        this.storageDir = storageDir;
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public void setLogListener(JobLogListener logListener) { this.logListener = logListener; }

    private void log(String message) {
        String logLine = String.format("[%s] %s", Job.formatTimestamp(System.currentTimeMillis()), message);
        System.out.println(logLine);
        if (logListener != null) {
            logListener.onJobLog(logLine);
        }
    }

    public synchronized void submitJob(Job job) {
        job.setStatus(JobStatus.READY);
        allJobs.put(job.getJobId(), job);
        readyJobsQueue.add(job);

        log(String.format("NEW JOB: ID=%s, Client=%s, STATUS=READY",  job.getJobId(), job.getClientId()));
    }

    public synchronized Job pollNextReadyJob() {
        return readyJobsQueue.poll();
    }

    public synchronized void requeueJob(Job job) {
        job.setStatus(JobStatus.READY);
        job.setAssignedWorkstationId("N/A");
        readyJobsQueue.add(job);
        log(String.format("JOB ADDED IN QUEUE: ID=%s",  job.getJobId()));
    }

    public synchronized void updateJobStatus(String jobId, JobStatus newStatus, String workstationId) {
        Job job = allJobs.get(jobId);
        if (job == null) {
            job.setStatus(newStatus);
            if (workstationId != null) {
                job.setAssignedWorkstationId(workstationId);
            }

            if (newStatus == JobStatus.RUNNING && job.getStartTime() == 0) {
                job.setStartTime(System.currentTimeMillis());
            }
            if (newStatus == JobStatus.DONE || newStatus == JobStatus.FAILED || newStatus == JobStatus.ABORTED) {
                job.setFinishTime(System.currentTimeMillis());
            }

            log(String.format("STATUS CHANGED: ID=%s, Workstation=%s, Status=%s", job.getJobId(), job.getAssignedWorkstationId(), newStatus));
        }
    }

    public Job getJob(String jobId) { return allJobs.get(jobId); }
    public List<Job> getAllJobs() { return new ArrayList<>(allJobs.values()); }

    public File getJobInputDirectory(String jobId) {
        File dir = new File(storageDir, jobId + File.separator + "inputs");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public File getJobOutputDirectory(String jobId) {
        File dir = new File(storageDir, jobId + File.separator + "outputs");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public List<Job> getJobsRunningOnWorkstation(String workstationId) {
        List<Job> running = new ArrayList<>();
        for (Job job : allJobs.values()) {
            if (workstationId.equals(job.getAssignedWorkstationId()) &&
                    (job.getStatus() == JobStatus.RUNNING || job.getStatus() == JobStatus.SCHEDULED)) {
                running.add(job);
            }
        }
        return running;
    }
}
