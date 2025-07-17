package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class DatabaseProvisionReq implements Serializable {
    private static final long serialVersionUID = -2800620786637917787L;

    private String dbId;

    private String dbName;

    private String user;

    private String ipsource;

    private List<String> permissions;
}
