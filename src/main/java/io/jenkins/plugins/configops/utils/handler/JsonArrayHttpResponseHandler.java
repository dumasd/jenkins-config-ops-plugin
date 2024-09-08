package io.jenkins.plugins.configops.utils.handler;

import com.alibaba.fastjson2.JSON;
import java.io.IOException;
import java.util.List;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class JsonArrayHttpResponseHandler<T> extends AbstractHttpClientResponseHandler<List<T>> {

    private final Class<T> clazz;

    public JsonArrayHttpResponseHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Handle the response entity and transform it into the actual response
     * object.
     */
    public List<T> handleEntity(HttpEntity entity) throws IOException {
        byte[] bs = EntityUtils.toByteArray(entity);
        EntityUtils.consume(entity);
        return JSON.parseArray(bs, clazz);
    }
}
