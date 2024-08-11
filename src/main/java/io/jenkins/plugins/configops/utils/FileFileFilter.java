package io.jenkins.plugins.configops.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

/**
 * @author Bruce.Wu
 * @date 2024-08-08
 */
public class FileFileFilter implements FileFilter, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean accept(File pathname) {
        return pathname.isFile();
    }
}
