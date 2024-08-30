package io.jenkins.plugins.configops.model.req;

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

    private String nacosId;

    private String namespace;

    private String group;

    private String dataId;

    private String patchContent;

    private String fullContent;

    private String content;

    private String format;
}
