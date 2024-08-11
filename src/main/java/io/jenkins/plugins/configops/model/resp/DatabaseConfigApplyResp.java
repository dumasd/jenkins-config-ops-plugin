package io.jenkins.plugins.configops.model.resp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
public class DatabaseConfigApplyResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String database;

    private List<SqlResult> result;

    @Setter
    @Getter
    @ToString
    public static class SqlResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String sql;
        private Long rowcount = 0L;
    }
}
