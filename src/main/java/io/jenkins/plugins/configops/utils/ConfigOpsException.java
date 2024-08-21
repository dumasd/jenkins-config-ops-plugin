package io.jenkins.plugins.configops.utils;

public class ConfigOpsException extends RuntimeException {
    public ConfigOpsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigOpsException(String message) {
        super(message);
    }
}
