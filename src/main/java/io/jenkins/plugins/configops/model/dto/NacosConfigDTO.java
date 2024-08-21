package io.jenkins.plugins.configops.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
public class NacosConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appName;
    private String id;
    private String dataId;
    private String group;
    private String content;
    private String tenant;
    private String md5;
    private String type;

    public String fullName() {
        return group + "/" + dataId;
    }

}
