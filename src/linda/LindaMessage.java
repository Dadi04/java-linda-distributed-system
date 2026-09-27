package linda;

import java.io.Serializable;

public class LindaMessage implements Serializable {
    public enum Type {
        OUT, IN, INP, RD, RDP, EVAL, RESPONSE_OK, RESPONSE_FAIL
    }

    private final Type type;
    private final String[] tuple;
    private final String evalName;
    private final Runnable evalRunnable;
    private final boolean success;

    // OUT, IN, INP, RD, RDP
    public LindaMessage(Type type, String[] tuple) {
        this(type, tuple, null, null, false);
    }

    // EVAL
    public LindaMessage(String evalName, Runnable evalRunnable) {
        this(Type.EVAL, null, evalName, evalRunnable, false);
    }

    // server response
    public LindaMessage(Type type, String[] tuple, boolean success) {
        this(type, tuple, null, null, success);
    }

    public LindaMessage(Type type, String[] tuple, String evalName, Runnable evalRunnable, boolean success) {
        this.type = type;
        this.tuple = tuple;
        this.evalName = evalName;
        this.evalRunnable = evalRunnable;
        this.success = success;
    }

    public Type getType() { return type; }
    public String[] getTuple() { return tuple; }
    public String getEvalName() { return evalName; }
    public Runnable getEvalRunnable() { return evalRunnable; }
    public boolean isSuccess() { return success; }
}
