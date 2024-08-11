package io.jenkins.plugins.configops.model.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NacosServerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JSONField(name = "nacos_id")
    private String nacosId;

    private String url;
}
