package io.jenkins.plugins.configops.utils;

import hudson.Launcher;
import hudson.remoting.VirtualChannel;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    public static void requireTrue(Boolean b, String message) {
        if (b == null || Boolean.FALSE.equals(b)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static VirtualChannel getChannel(Launcher launcher) {
        if (launcher == null) {
            throw new IllegalArgumentException("Launcher is null");
        }
        if (launcher.getChannel() == null) {
            throw new IllegalArgumentException("Launcher channel is null");
        }
        return launcher.getChannel();
    }

    /**
     * 版本号比较
     *
     * @param v1
     * @param v2
     * @return 0代表相等，1代表左边大，-1代表右边大
     * Utils.compareVersion("1.0.358_20180820090554","1.0.358_20180820090553")=1
     */
    public static int compareVersion(String v1, String v2) {
        if (v1.equals(v2)) {
            return 0;
        }
        VersionNumber vn1 = new VersionNumber(v1);
        VersionNumber vn2 = new VersionNumber(v2);
        return vn1.compareTo(vn2);
    }

    public static void printTableData(PrintStream ps, List<LinkedHashMap<String, Object>> list, int maxCount) {
        if (list == null || list.isEmpty()) {
            return;
        }
        LinkedHashMap<String, Object> first = list.get(0);

        // 打印头
        Set<String> headers = first.keySet();
        for (String cell : headers) {
            ps.printf("%-15s", cell);
        }
        ps.println();

        // 打印分割行
        for (int i = 0; i < headers.size(); i++) {
            ps.print("----------------");
        }
        ps.println();

        // 打印数据
        int count = 0;
        for (LinkedHashMap<String, Object> row : list) {
            Collection<Object> values = row.values();
            for (Object cell : values) {
                String str = Objects.requireNonNullElse(cell, "NULL").toString();
                ps.printf("%-15s", str);
            }
            ps.println();
            count++;
            if (maxCount > 0 && count >= maxCount) {
                break;
            }
        }
    }
}
