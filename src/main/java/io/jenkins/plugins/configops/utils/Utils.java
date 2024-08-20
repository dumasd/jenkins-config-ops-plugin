package io.jenkins.plugins.configops.utils;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;

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

    public static void requireNotBlank(String s, String message) {
        if (StringUtils.isBlank(s)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> void requireNotEmpty(Collection<T> coll, String message) {
        if (coll == null || coll.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
