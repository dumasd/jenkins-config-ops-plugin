package io.jenkins.plugins.configops.utils;

import java.io.File;

/**
 * @author Bruce.Wu
 * @date 2024-08-09
 */
public class Utils {

    public static boolean isNullOrEmpty(final String name) {
        return name == null || name.matches("\\s*");
    }

    public static boolean isNotEmpty(final String name) {
        return !isNullOrEmpty(name);
    }

    public static String getFileExt(File file) {
        String name = file.getName();
        int idx = name.indexOf('.');
        if (idx > 0) {
            return name.substring(idx + 1);
        } else {
            return null;
        }
    }
}
