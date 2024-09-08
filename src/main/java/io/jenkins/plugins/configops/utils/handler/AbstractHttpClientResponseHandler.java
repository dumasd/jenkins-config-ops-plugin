package io.jenkins.plugins.configops.utils.handler;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public abstract class AbstractHttpClientResponseHandler<T> implements HttpClientResponseHandler<T> {
    @Override
    public T handleResponse(final ClassicHttpResponse response) throws IOException {
        final HttpEntity entity = response.getEntity();
        if (response.getCode() >= HttpStatus.SC_REDIRECTION) {
            String msg = null;
            try {
                msg = EntityUtils.toString(entity);
            } catch (Exception ignored) {
            }
            msg = StringUtils.defaultIfBlank(msg, response.getReasonPhrase());
            EntityUtils.consume(entity);
            throw new HttpResponseException(response.getCode(), msg);
        }
        return entity == null ? null : handleEntity(entity);
    }

    public abstract T handleEntity(HttpEntity entity) throws IOException;
}
