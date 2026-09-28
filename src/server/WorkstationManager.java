package server;

import common.Constants;
import common.Message;
import common.MessageType;
import common.WorkstationInfo;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkstationManager {
    private final Map<String, WorkstationInfo> workstations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public interface WorkstationFailureListener {
        void onWorkstationFailed(WorkstationInfo workstation);
    }
    private final WorkstationFailureListener failureListener;

    public WorkstationManager(WorkstationFailureListener failureListener) {
        this.failureListener = failureListener;
        startHeartbeatMonitor(Constants.DEFAULT_HEARTBEAT_INTERVAL_SEC);
    }

    public void registerWorkstation(WorkstationInfo info) {
        info.setAlive(true);
        info.setLastHeartbeatTime(System.currentTimeMillis());
        workstations.put(info.getId(), info);
        System.out.println("WorkstationManager: Workstation " + info.getId() + " registered");
    }

    public synchronized WorkstationInfo findAvailableWorkstation() {
        for (WorkstationInfo wsi : workstations.values()) {
            if (wsi.hasAvailableCapacity()) {
                return wsi;
            }
        }
        return null;
    }

    public List<WorkstationInfo> getAllWorkstations() { return new ArrayList<>(workstations.values()); }
    public WorkstationInfo getWorkstation(String id) { return workstations.get(id); }

    private void startHeartbeatMonitor(int intervalSec) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                checkAllWorkstations();
            } catch (Exception e) {
                System.out.println("WorkstationManager: Error while checking workstations: " + e.getMessage());
            }
        }, intervalSec, intervalSec, TimeUnit.SECONDS);
    }

    private void checkAllWorkstations() {
        for (WorkstationInfo wsi : workstations.values()) {
            if (!wsi.isAlive()) {
                continue;
            }

            boolean alive = pingWorkstation(wsi);
            if (!alive) {
                System.err.println("WorkstationManager: Workstation " + wsi.getId() + " is dead");
                wsi.setAlive(false);

                if (failureListener != null) {
                    failureListener.onWorkstationFailed(wsi);
                }
            } else {
                wsi.setLastHeartbeatTime(System.currentTimeMillis());
            }
        }
    }

    private boolean pingWorkstation(WorkstationInfo wsi) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(wsi.getHost(), wsi.getPort()), Constants.HEARTBEAT_TIMEOUT_MS);
            socket.setSoTimeout(Constants.HEARTBEAT_TIMEOUT_MS);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new Message(MessageType.HEARTBEAT_PING, null));
            out.flush();

            Message response = (Message) in.readObject();
            return response != null && response.getType() == MessageType.WORKSTATION_HEARTBEAT_ACK;
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        heartbeatScheduler.shutdownNow();
    }
}
