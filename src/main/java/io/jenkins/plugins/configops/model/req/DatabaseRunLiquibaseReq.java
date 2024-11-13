package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Builder
public class DatabaseRunLiquibaseReq implements Serializable {
    private static final long serialVersionUID = -2239214531976180856L;

    private String changeLogFile;

    private String dbId;

    private String cwd;

    private String command;

    private String args;
}
