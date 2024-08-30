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
public class DatabaseConfigReq implements Serializable {

    private static final long serialVersionUID = 3319133105875479508L;

    private String dbId;

    private String sql;
    /**
     * 数据库名称
     */
    private String database;
}
