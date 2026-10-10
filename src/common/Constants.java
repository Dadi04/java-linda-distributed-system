package common;

public final class Constants {
    private Constants() {}

    public static final int DEFAULT_SERVER_PORT = 9000;
    public static final int DEFAULT_LINDA_PORT = 9001;
    public static final int DEFAULT_WORKSTATION_PORT = 9100;

    public static final String DEFAULT_SERVER_HOST = "localhost";
    public static final String DEFAULT_SERVER_STORAGE_DIR = "server_storage";

    public static final int DEFAULT_MAX_PARALLEL_JOBS = 2;
    public static final String DEFAULT_LINDA_JAR_PATH = "lib/linda.jar";
    public static final String DEFAULT_WORKSTATION_STORAGE_PREFIX = "workstation_storage_";
    public static final String WORKSTATION_ID_PREFIX = "WS-";

    public static final String DEFAULT_CLIENT_DOWNLOAD_DIR = "downloads";
    public static final String CLIENT_ID_PREFIX = "Client-";

    public static final String JOB_ID_PREFIX = "JOB-";

    public static final int DEFAULT_HEARTBEAT_INTERVAL_SEC = 5;
    public static final int HEARTBEAT_TIMEOUT_MS = 4000;

    public static final int CLIENT_CONNECT_TIMEOUT_MS = 5000;
    public static final int CLIENT_SO_TIMEOUT_MS = 10000;
    public static final int SCHEDULER_POLL_INTERVAL_MS = 1000;

    public static final int MAX_INPUT_FILES = 6;
    public static final int MAX_OUTPUT_FILES = 6;

    public static final int FILE_BUFFER_SIZE = 64 * 1024;
}
