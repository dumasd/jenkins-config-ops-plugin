package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NacosNamespaceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String namespace;
    private String namespaceShowName;
    private String namespaceDesc;
    private Integer configCount;
    private Integer quota;
    private Integer type;
}
