package common;

import java.io.Serializable;

public class EvalTask implements Serializable {
    private final String taskId;
    private final String taskName;
    private final Runnable runnable;

    public EvalTask(String taskId, String taskName, Runnable runnable) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.runnable = runnable;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public Runnable getRunnable() {
        return runnable;
    }
}
