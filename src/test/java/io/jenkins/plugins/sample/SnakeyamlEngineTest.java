package io.jenkins.plugins.sample;

import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.springframework.core.io.ClassPathResource;

@Log
public class SnakeyamlEngineTest {

    @Test
    public void test() throws Exception {
        ClassPathResource resource = new ClassPathResource("test.yaml");
        LoadSettings loadSettings = LoadSettings.builder().build();
        Load load = new Load(loadSettings);

        Object obj = load.loadFromInputStream(resource.getInputStream());

        log.info("end");
    }
}
