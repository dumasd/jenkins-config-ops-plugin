package io.jenkins.plugins.configops.model.req;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
@Builder
public class DatabaseConfigReq implements Serializable {

    @JSONField(name = "db_id")
    private String dbId;

    private String sql;

}
