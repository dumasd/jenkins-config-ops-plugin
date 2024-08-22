package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
        String ns;
        if (tenant == null || tenant.isBlank()) {
            ns = "public";
        } else {
            ns = tenant;
        }
        return String.format("%s/%s/%s", ns, group, dataId);
    }
}
