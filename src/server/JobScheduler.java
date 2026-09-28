package server;

import common.*;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobScheduler implements Runnable {
    private final JobManager jobManager;
    private final WorkstationManager workstationManager;
    private final ExecutorService dispatchPool = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public JobScheduler(JobManager jobManager, WorkstationManager workstationManager) {
        this.jobManager = jobManager;
        this.workstationManager = workstationManager;
    }

    @Override
    public void run() {
        System.out.println("JobScheduler: Scheduler started");

        while (running) {
            try {
                WorkstationInfo freeWorkstation = workstationManager.findAvailableWorkstation();

                if (freeWorkstation != null) {
                    Job job = jobManager.pollNextReadyJob();
                    if (job != null) {
                        dispatchJobToWorkstation(job, freeWorkstation);
                    } else {
                        Thread.sleep(1000);
                    }
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("JobScheduler: Exception occurred: " + e.getMessage());
            }
        }
    }

    private void dispatchJobToWorkstation(Job job, WorkstationInfo workstation) {
        workstation.incrementActiveJobsCount();
        jobManager.updateJobStatus(job.getJobId(), JobStatus.SCHEDULED, workstation.getId());

        dispatchPool.submit(() -> {
            try (Socket socket = new Socket(workstation.getHost(), workstation.getPort());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out.writeObject(new Message(MessageType.DISPATCH_JOB, job));
                out.flush();

                File inputDir = jobManager.getJobInputDirectory(job.getJobId());
                File jarFile = new File(inputDir, job.getJarFileName());
                FileTransferUtil.sendFile(jarFile, out);

                for (String inputFileName : job.getInputFiles()) {
                    File inputFile = new File(inputDir, inputFileName);
                    FileTransferUtil.sendFile(inputFile, out);
                }

                Message ack = (Message) in.readObject();
                if (ack != null && ack.getType() == MessageType.JOB_STARTED) {
                    jobManager.updateJobStatus(job.getJobId(), JobStatus.RUNNING, workstation.getId());
                } else {
                    throw new Exception("JobScheduler: Workstation didn't acknowledge that the job was started");
                }
            } catch (Exception e) {
                System.err.println("JobScheduler: Failed to send job " + job.getJobId() + " to workstation " + workstation.getId() + ": " + e.getMessage());
                workstation.decrementActiveJobsCount();
                jobManager.requeueJob(job);
            }
        });
    }

    public void stop() {
        running = false;
        dispatchPool.shutdownNow();
    }
}
