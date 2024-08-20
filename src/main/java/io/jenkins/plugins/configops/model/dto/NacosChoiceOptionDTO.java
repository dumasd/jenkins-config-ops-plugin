package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-16
 */
@Setter
@Getter
@ToString
public class NacosChoiceOptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String namespace;

    private String group;

    private String dataId;
    /**
     * 可为空
     */
    private String version;

    public NacosChoiceOptionDTO(String namespace, String group, String dataId, String version) {
        this.namespace = namespace;
        this.group = group;
        this.dataId = dataId;
        this.version = version;
    }

    public NacosChoiceOptionDTO() {}
}
