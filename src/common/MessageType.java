package common;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // workstation -> server
    WORKSTATION_REGISTER,
    WORKSTATION_HEARTBEAT_ACK,
    JOB_STARTED,
    JOB_COMPLETED,
    JOB_EXECUTION_FAILED,

    // server -> workstation
    HEARTBEAT_PING,
    DISPATCH_JOB,

    // client -> server
    CLIENT_SUBMIT_JOB,
    CLIENT_GET_STATUS,
    CLIENT_GET_RESULTS,
    CLIENT_ABORT_JOB,
    CLIENT_DECISION_FAILOVER,

    // server -> client
    SERVER_RESPONSE_OK,
    SERVER_RESPONSE_ERROR,
    WORKSTATION_FAILED_ALERT,

    LINDA_REQUEST,
    LINDA_RESPONSE
}
