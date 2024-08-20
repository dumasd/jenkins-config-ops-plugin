package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-10
 */
@Getter
@Setter
@ToString
public class NacosConfigFileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String namespace;

    private String group;

    private String dataId;

    private List<String> versions;

    public NacosConfigFileDTO() {}

    public NacosConfigFileDTO(String namespace, String group, String dataId) {
        this.namespace = namespace;
        this.group = group;
        this.dataId = dataId;
    }

    public String spliceNamespaceGroup() {
        return namespace + "/" + group;
    }

    public String spliceNamespaceGroupDataId() {
        return String.format("%s/%s/%s", namespace, group, dataId);
    }
}
