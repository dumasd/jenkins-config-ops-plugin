package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NacosGetConfigsReq implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nacosId;

    private List<String> namespaces;
}
