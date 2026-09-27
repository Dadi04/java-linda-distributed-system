package common;

import java.io.Serializable;

public enum JobStatus implements Serializable {
    READY,
    SCHEDULED,
    RUNNING,
    DONE,
    FAILED,
    ABORTED
}
