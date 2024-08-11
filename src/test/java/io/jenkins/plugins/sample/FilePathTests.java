package io.jenkins.plugins.sample;

import hudson.FilePath;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;

@Log
public class FilePathTests {

    // @Test
    public void test() throws Exception {

        FilePath filePath =
                new FilePath(new File("/Users/wukai/apps/jenkins-agent/workspace/test-pipe/test/blue/group"));
        FilePath[] filePaths = filePath.list("*/config.yaml");
        Arrays.stream(filePaths).forEach(e -> {
            log.info(e.getRemote());
        });

        List<String> vs = Arrays.stream(filePaths)
                .map(e -> {
                    String path = e.getRemote();
                    String[] ss = StringUtils.split(path, File.separatorChar);
                    return ss[ss.length - 2];
                })
                .collect(Collectors.toList());
        log.info(vs.toString());
    }
}
