package io.jenkins.plugins.configops.model.resp;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Builder
public class DatabaseRunLiquibaseResp implements Serializable {
    private static final long serialVersionUID = -2239214531976180856L;

    private String retcode;

    private String stdout;

    private String stderr;

    public boolean isSuccess() {
        return retcode == null || "0".equals(retcode);
    }
}
