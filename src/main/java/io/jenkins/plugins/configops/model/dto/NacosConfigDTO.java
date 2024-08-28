package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NacosConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenant;
    private String group;
    private String dataId;
    private String content;
    private String type;
    private String appName;
    private String md5;

    /**
     * 追加内容
     */
    private String patchContent = "";
    /**
     * 删除内容
     */
    private String deleteContent = "";

    public String patchContent() {
        return Objects.requireNonNullElse(patchContent, "");
    }

    public String deleteContent() {
        return Objects.requireNonNullElse(deleteContent, "");
    }

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
