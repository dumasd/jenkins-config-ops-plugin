package io.jenkins.plugins.configops.model.req;

import com.alibaba.fastjson2.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-09
 */
@Setter
@Getter
@ToString
@Builder
public class NacosConfigReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @JSONField(name = "nacos_id")
    private String nacosId;

    @JSONField(name = "namespace_id")
    private String namespaceId;

    private String group;

    @JSONField(name = "data_id")
    private String dataId;

    @JSONField(name = "patch_content")
    private String patchContent;

    @JSONField(name = "full_content")
    private String fullContent;

    @JSONField(name = "content")
    private String content;
}
