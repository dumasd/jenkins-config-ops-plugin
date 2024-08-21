package io.jenkins.plugins.sample;

import hudson.util.VersionNumber;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

@Log
public class VersionNumberTest {

    @Test
    public void test() {
        VersionNumber vn1 = new VersionNumber("v2.1.32");
        VersionNumber vn2 = new VersionNumber("v2.1.1");
        log.info(vn1.compareTo(vn2) + "");

        vn1 = new VersionNumber("hotkey_db_v2.0.1.sql");
        vn2 = new VersionNumber("hotkey_db_v2.0.2.sql");
        log.info(vn1.compareTo(vn2) + "");
    }
}
