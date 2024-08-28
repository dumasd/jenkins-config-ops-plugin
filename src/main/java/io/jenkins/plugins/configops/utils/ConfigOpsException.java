package io.jenkins.plugins.configops.utils;

public class ConfigOpsException extends RuntimeException {
    private static final long serialVersionUID = -1947217524989066537L;

    public ConfigOpsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigOpsException(String message) {
        super(message);
    }

    public ConfigOpsException(Throwable cause) {
        super(cause);
    }
}
