package common;

public final class Constants {
    private Constants() {}

    public static final int DEFAULT_SERVER_PORT = 9000;

    public static final int DEFAULT_HEARTBEAT_INTERVAL_SEC = 5;
    public static final int HEARTBEAT_TIMEOUT_MS = 4000;

    public static final int MAX_INPUT_FILES = 6;
    public static final int MAX_OUTPUT_FILES = 6;

    public static final int FILE_BUFFER_SIZE = 64 * 1024;
}
