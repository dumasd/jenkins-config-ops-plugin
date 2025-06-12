package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GraphdbChangeSetReq implements Serializable {
    private static final long serialVersionUID = -7299148077868171509L;

    private String systemId;

    private String changeLogFile;

    private HashMap<String, String> vars;

    private String contexts;

    private Integer count;
}
