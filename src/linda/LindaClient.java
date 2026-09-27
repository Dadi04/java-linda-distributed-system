package linda;

import rs.ac.bg.etf.kdp.Linda;

import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LindaClient implements Linda {

    private final String serverHost;
    private final int serverPort;

    public LindaClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    private LindaMessage sendRequest(LindaMessage request) {
        try (Socket socket = new Socket(serverHost, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request);
            out.flush();

            return (LindaMessage) in.readObject();
        } catch (Exception e) {
            System.err.println("LindaClient: Error in communication with Linda server: " + e.getMessage());
            return new LindaMessage(LindaMessage.Type.RESPONSE_FAIL, null, false);
        }
    }

    @Override
    public void out(String[] tuple) {
        if (tuple == null) throw new IllegalArgumentException("Tuple cannot be null");
        for (String s : tuple) {
            if (s == null) throw new IllegalArgumentException("Tuple fields cannot be null");
        }

        LindaMessage req = new LindaMessage(LindaMessage.Type.OUT, tuple);
        sendRequest(req);
    }

    @Override
    public void in(String[] tuple) {
        if (tuple == null) throw new IllegalArgumentException("Tuple cannot be null");

        LindaMessage req = new LindaMessage(LindaMessage.Type.IN, tuple);
        LindaMessage response = sendRequest(req);

        if (response != null && response.getTuple() != null) {
            String[] result = response.getTuple();
            System.arraycopy(result, 0, tuple, 0, Math.min(result.length, tuple.length));
        }
    }

    @Override
    public boolean inp(String[] tuple) {
        if (tuple == null) throw new IllegalArgumentException("Tuple cannot be null");

        LindaMessage req = new LindaMessage(LindaMessage.Type.INP, tuple);
        LindaMessage response = sendRequest(req);

        if (response != null && response.isSuccess() && response.getTuple() != null) {
            String[] result = response.getTuple();
            System.arraycopy(result, 0, tuple, 0, Math.min(result.length, tuple.length));
            return true;
        }
        return false;
    }

    @Override
    public void rd(String[] tuple) {
        if (tuple == null) throw new IllegalArgumentException("Tuple cannot be null");

        LindaMessage req = new LindaMessage(LindaMessage.Type.RD, tuple);
        LindaMessage response = sendRequest(req);

        if (response != null && response.getTuple() != null) {
            String[] result = response.getTuple();
            System.arraycopy(result, 0, tuple, 0, Math.min(result.length, tuple.length));
        }
    }

    @Override
    public boolean rdp(String[] tuple) {
        if (tuple == null) throw new IllegalArgumentException("Tuple cannot be null");

        LindaMessage req = new LindaMessage(LindaMessage.Type.RDP, tuple);
        LindaMessage response = sendRequest(req);

        if (response != null && response.isSuccess() && response.getTuple() != null) {
            String[] result = response.getTuple();
            System.arraycopy(result, 0, tuple, 0, Math.min(result.length, tuple.length));
            return true;
        }
        return false;
    }

    @Override
    public void eval(String name, Runnable thread) {
        if (thread == null) throw new IllegalArgumentException("Thread cannot be null");

        LindaMessage req = new LindaMessage(name, thread);
        sendRequest(req);
    }
}
