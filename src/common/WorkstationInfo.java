package common;

import java.io.Serializable;

public class WorkstationInfo implements Serializable {
    private final String id;
    private final String host;
    private final int port;
    private final String osName;
    private final String javaVersion;
    private final int maxParallelJobs;

    private int activeJobsCount;
    private long lastHeartbeatTime;
    private boolean isAlive;

    public WorkstationInfo(String id, String host, int port, String osName, String javaVersion, int maxParallelJobs) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.osName = osName;
        this.javaVersion = javaVersion;
        this.maxParallelJobs = maxParallelJobs;
    }

    public synchronized boolean hasAvailableCapacity() {
        return isAlive && activeJobsCount < maxParallelJobs;
    }

    public synchronized void incrementActiveJobsCount() {
        activeJobsCount++;
    }
    public synchronized void decrementActiveJobsCount() {
        if (activeJobsCount > 0) {
            activeJobsCount--;
        }
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getOsName() { return osName; }
    public String getJavaVersion() { return javaVersion; }
    public int getMaxParallelJobs() { return maxParallelJobs; }

    public synchronized int getActiveJobsCount() { return activeJobsCount; }
    public synchronized long getLastHeartbeatTime() { return lastHeartbeatTime; }
    public synchronized void setHeartbeatTime(long time) { this.lastHeartbeatTime = time; }
    public synchronized boolean isAlive() { return isAlive; }
    public synchronized void setAlive(boolean alive) { this.isAlive = alive; }

    @Override
    public String toString() {
        return String.format("%s [%s, Java %s, Capacity: %d/%d, Active: %s]",
                id, osName, javaVersion, activeJobsCount, maxParallelJobs, isAlive);
    }
}
