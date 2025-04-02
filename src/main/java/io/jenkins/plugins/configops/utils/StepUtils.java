package io.jenkins.plugins.configops.utils;

import hudson.EnvVars;
import hudson.model.TaskListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StepUtils {

    public static <T extends Class<?>> Set<T> requires(T... classes) {
        return new HashSet<>(Arrays.asList(classes));
    }

    @SuppressWarnings("unchecked")
    public static Set<? extends Class<?>> requiresDefault() {
        return requires(EnvVars.class, TaskListener.class);
    }

    private StepUtils() {
        // Hide utility class constructor
    }
}
