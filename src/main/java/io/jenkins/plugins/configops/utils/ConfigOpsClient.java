package io.jenkins.plugins.configops.utils;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.configops.model.dto.ElasticsearchChangeSetDTO;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.dto.NacosNamespaceDTO;
import io.jenkins.plugins.configops.model.req.CommonEditContentReq;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.req.DatabaseRunLiquibaseReq;
import io.jenkins.plugins.configops.model.req.ElasticsearchChangeSetReq;
import io.jenkins.plugins.configops.model.req.NacosApplyChangeSetReq;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.req.NacosGetChangeSetReq;
import io.jenkins.plugins.configops.model.req.NacosGetConfigsReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.model.resp.DatabaseRunLiquibaseResp;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.model.resp.NacosGetChangeSetResp;
import io.jenkins.plugins.configops.utils.handler.JsonArrayHttpResponseHandler;
import io.jenkins.plugins.configops.utils.handler.JsonObjectHttpResponseHandler;
import io.jenkins.plugins.configops.utils.handler.StringHttpClientResponseHandler;
import java.io.Serializable;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;

/**
 * @author Bruce.Wu
 * @date 2024-08-09
 */
public class ConfigOpsClient implements Serializable {

    private static final long serialVersionUID = -4481577247801508720L;

    private final String url;

    public ConfigOpsClient(String url) {
        this.url = url;
    }

    public List<NacosNamespaceDTO> getNacosNamespaces(String nacosId) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/namespaces");
            uriBuilder.addParameter("nacos_id", nacosId);
            HttpGet httpReq = new HttpGet(uriBuilder.build());
            return httpClient.execute(httpReq, new JsonArrayHttpResponseHandler<>(NacosNamespaceDTO.class));
        }
    }

    public NacosConfigDTO getNacosConfig(String nacosId, String namespace, String group, String dataId) {
        if ("public".equals(namespace)) {
            namespace = "";
        }
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config");
            uriBuilder
                    .addParameter("nacos_id", nacosId)
                    .addParameter("namespace_id", namespace)
                    .addParameter("group", group)
                    .addParameter("data_id", dataId);
            HttpGet httpReq = new HttpGet(uriBuilder.build());
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(NacosConfigDTO.class));
        } catch (Exception e) {
            throw new ConfigOpsException(e);
        }
    }

    public List<NacosConfigDTO> getNacosConfigs(NacosGetConfigsReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/configs");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonArrayHttpResponseHandler<>(NacosConfigDTO.class));
        }
    }

    public NacosConfigModifyPreviewResp nacosConfigModifyPreview(NacosConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            return httpClient.execute(
                    httpPost, new JsonObjectHttpResponseHandler<>(NacosConfigModifyPreviewResp.class));
        }
    }

    public String nacosConfigModifyApply(NacosConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/config/modify");
            HttpPut httpReq = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new StringHttpClientResponseHandler());
        }
    }

    public DatabaseConfigApplyResp databaseConfigApply(DatabaseConfigReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/database/v1/run-sql");
            HttpPut httpReq = new HttpPut(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(DatabaseConfigApplyResp.class));
        }
    }

    public NacosConfigModifyPreviewResp commonPatchContent(CommonEditContentReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/common/v1/patch_content");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(NacosConfigModifyPreviewResp.class));
        }
    }

    public NacosConfigModifyPreviewResp commonDeleteContent(CommonEditContentReq req) throws Exception {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/common/v1/delete_content");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(NacosConfigModifyPreviewResp.class));
        }
    }

    public NacosGetChangeSetResp getChangeSet(NacosGetChangeSetReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/get_change_set");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(NacosGetChangeSetResp.class));
        } catch (Exception e) {
            throw new ConfigOpsException(e);
        }
    }

    public String applyChangeSet(NacosApplyChangeSetReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/nacos/v1/apply_change_set");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new StringHttpClientResponseHandler());
        } catch (Exception e) {
            throw new ConfigOpsException(e);
        }
    }

    public DatabaseRunLiquibaseResp databaseRunLiquibase(DatabaseRunLiquibaseReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/database/v1/run-liquibase");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonObjectHttpResponseHandler<>(DatabaseRunLiquibaseResp.class));
        } catch (Exception e) {
            throw new ConfigOpsException(e);
        }
    }

    public List<ElasticsearchChangeSetDTO> applyElasticsearchChangeSet(ElasticsearchChangeSetReq req) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/elasticsearch/v1/apply_change_set");
            HttpPost httpReq = new HttpPost(uriBuilder.build());
            HttpEntity entity = HttpEntities.create(JSON.toJSONString(req), ContentType.APPLICATION_JSON);
            httpReq.setEntity(entity);
            return httpClient.execute(httpReq, new JsonArrayHttpResponseHandler<>(ElasticsearchChangeSetDTO.class));
        } catch (Exception e) {
            throw new ConfigOpsException(e);
        }
    }
}
