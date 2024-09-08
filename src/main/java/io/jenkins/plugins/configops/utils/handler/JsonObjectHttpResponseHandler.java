package io.jenkins.plugins.configops.utils.handler;

import com.alibaba.fastjson2.JSON;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class JsonObjectHttpResponseHandler<T> extends AbstractHttpClientResponseHandler<T> {

    private final Class<T> clazz;

    public JsonObjectHttpResponseHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Handle the response entity and transform it into the actual response
     * object.
     */
    public T handleEntity(HttpEntity entity) throws IOException {
        byte[] bs = EntityUtils.toByteArray(entity);
        EntityUtils.consume(entity);
        if (clazz.isAssignableFrom(String.class)) {
            return (T) new String(bs, StandardCharsets.UTF_8);
        }
        return JSON.parseObject(bs, clazz);
    }
}
