package io.jenkins.plugins.configops.model.req;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NacosGetConfigsReq implements Serializable {
    private static final long serialVersionUID = 1L;
    @JSONField(name = "nacos_id")
    private String nacosId;
    private List<String> namespaces;
}
