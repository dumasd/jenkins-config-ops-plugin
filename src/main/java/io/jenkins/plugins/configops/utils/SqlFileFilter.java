package io.jenkins.plugins.configops.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

public class SqlFileFilter implements FileFilter, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".sql");
    }
}
