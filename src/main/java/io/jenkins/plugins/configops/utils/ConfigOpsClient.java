package io.jenkins.plugins.configops.utils;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.configops.model.dto.NacosServerDTO;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;

/**
 * @author Bruce.Wu
 * @date 2024-08-09
 */
public class ConfigOpsClient {

    private final String url;

    public ConfigOpsClient(String url) {
        this.url = url;
    }

    public List<NacosServerDTO> getNacosServers() throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/list");
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            return httpClient.execute(httpGet, new InnerHttpClientResponseHandler<>() {
                @Override
                public List<NacosServerDTO> handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseArray(bs, NacosServerDTO.class);
                }
            });
        }
    }

    public NacosConfigModifyPreviewResp nacosConfigModifyPreview(NacosConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            return httpClient.execute(httpPost, new InnerHttpClientResponseHandler<>() {
                @Override
                public NacosConfigModifyPreviewResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, NacosConfigModifyPreviewResp.class);
                }
            });
        }
    }

    public String nacosConfigModifyApply(NacosConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPut httpPut = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            return httpClient.execute(httpPut, new InnerHttpClientResponseHandler<>() {
                @Override
                public String handleEntity(HttpEntity entity) throws IOException {
                    try {
                        return EntityUtils.toString(entity);
                    } catch (final ParseException ex) {
                        throw new ClientProtocolException(ex);
                    }
                }
            });
        }
    }

    public DatabaseConfigApplyResp databaseConfigApply(DatabaseConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/database/v1/run-sql");
            HttpPut httpPut = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            return httpClient.execute(httpPut, new InnerHttpClientResponseHandler<>() {
                @Override
                public DatabaseConfigApplyResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, DatabaseConfigApplyResp.class);
                }
            });
        }
    }

    public abstract static class InnerHttpClientResponseHandler<T> implements HttpClientResponseHandler<T> {

        @Override
        public T handleResponse(final ClassicHttpResponse response) throws IOException {
            final HttpEntity entity = response.getEntity();
            if (response.getCode() >= HttpStatus.SC_REDIRECTION) {
                String msg = null;
                try {
                    msg = EntityUtils.toString(entity);
                } catch (Exception ignored) {
                }
                if (StringUtils.isBlank(msg)) {
                    msg = response.getReasonPhrase();
                }
                EntityUtils.consume(entity);
                throw new HttpResponseException(response.getCode(), msg);
            }
            return entity == null ? null : handleEntity(entity);
        }

        /**
         * Handle the response entity and transform it into the actual response
         * object.
         */
        public abstract T handleEntity(HttpEntity entity) throws IOException;
    }
}
