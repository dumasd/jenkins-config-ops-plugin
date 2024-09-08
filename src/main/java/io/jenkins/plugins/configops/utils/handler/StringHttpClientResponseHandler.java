package io.jenkins.plugins.configops.utils.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class StringHttpClientResponseHandler extends AbstractHttpClientResponseHandler<String> {
    @Override
    public String handleEntity(HttpEntity entity) throws IOException {
        byte[] bs = EntityUtils.toByteArray(entity);
        EntityUtils.consume(entity);
        return new String(bs, StandardCharsets.UTF_8);
    }
}
