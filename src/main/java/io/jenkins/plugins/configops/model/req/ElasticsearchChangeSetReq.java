package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-12-03
 */
@Setter
@Getter
@ToString
public class ElasticsearchChangeSetReq implements Serializable {
    private static final long serialVersionUID = 726838861730710776L;

    private String esId;

    private String changeLogFile;
    /**
     * 变量
     */
    private HashMap<String, String> vars;
    /**
     * 指定上下文
     */
    private String contexts;

    private Integer count;
}
