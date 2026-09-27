package linda;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LindaService implements Runnable{
    private final int port;
    private final TupleSpace tupleSpace;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public LindaService(int port, TupleSpace tupleSpace) {
        this.port = port;
        this.tupleSpace = tupleSpace;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("LindaService is listening on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (!running) break;
                    System.err.println("LindaService: Error accepting client connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("LindaService: Could not listen on port: " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            LindaMessage request = (LindaMessage) in.readObject();
            if (request == null) return;

            LindaMessage response = null;
            switch (request.getType()) {
                case OUT: {
                    tupleSpace.out(request.getTuple());
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, null, true);
                    break;
                }
                case IN: {
                    String[] inTemplate = request.getTuple();
                    tupleSpace.in(inTemplate);
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, inTemplate, true);
                    break;
                }
                case INP: {
                    String[] inpTemplate = request.getTuple();
                    boolean foundInp = tupleSpace.inp(inpTemplate);
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, inpTemplate, foundInp);
                    break;
                }
                case RD: {
                    String[] rdTemplate = request.getTuple();
                    tupleSpace.rd(rdTemplate);
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, rdTemplate, true);
                    break;
                }
                case RDP: {
                    String[] rdpTemplate = request.getTuple();
                    boolean foundRdp = tupleSpace.rdp(rdpTemplate);
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, rdpTemplate, foundRdp);
                    break;
                }
                case EVAL: {
                    System.out.println("LindaService: Handling EVAL: " + request.getEvalName());
                    handleEval(request.getEvalName(), request.getEvalRunnable());
                    response = new LindaMessage(LindaMessage.Type.RESPONSE_OK, null, true);
                    break;
                }
                default: response = new LindaMessage(LindaMessage.Type.RESPONSE_FAIL, null, false);
            }

            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
            System.err.println("LindaService: Error handling client connection: " + e.getMessage());
        }
    }

    private void handleEval(String name, Runnable runnable) {
         new Thread(runnable, "Eval-" + name).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        threadPool.shutdownNow();
    }
}
