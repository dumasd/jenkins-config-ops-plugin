package io.jenkins.plugins.configops.utils;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.configops.model.dto.NacosServerDTO;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;

import java.io.IOException;
import java.util.List;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;

/**
 * @author Bruce.Wu
 * @date 2024-08-09
 */
public class ConfigOpsClient {

    private String url;

    public ConfigOpsClient(String url) {
        this.url = url;
    }

    public List<NacosServerDTO> getNacosServers() {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/list");
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            return httpClient.execute(httpGet, new AbstractHttpClientResponseHandler<>() {
                @Override
                public List<NacosServerDTO> handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseArray(bs, NacosServerDTO.class);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public NacosConfigModifyPreviewResp nacosConfigModifyPreview(NacosConfigReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            return httpClient.execute(httpPost, new AbstractHttpClientResponseHandler<>() {
                @Override
                public NacosConfigModifyPreviewResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, NacosConfigModifyPreviewResp.class);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String nacosConfigModifyApply(NacosConfigReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPut httpPut = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            return httpClient.execute(httpPut, new BasicHttpClientResponseHandler());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseConfigApplyResp databaseConfigApply(DatabaseConfigReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/database/v1/run-sql");
            HttpPut httpPut = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            return httpClient.execute(httpPut, new AbstractHttpClientResponseHandler<>() {
                @Override
                public DatabaseConfigApplyResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, DatabaseConfigApplyResp.class);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
