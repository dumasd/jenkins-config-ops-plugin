package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Setter
@Getter
@ToString
@Accessors(chain = true)
public class NacosGetChangeSetReq implements Serializable {
    private static final long serialVersionUID = 4283199405130883585L;

    private String nacosId;

    private String changeLogFile;
}
