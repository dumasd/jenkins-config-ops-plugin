package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-19
 */
@Setter
@Getter
@ToString
public class NacosConfigModifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String namespace;

    private String group;

    private String dataId;
    /**
     * 内容格式
     */
    private String format;
    /**
     * 当前内容
     */
    private String content;
    /**
     * 修改后的值
     */
    private String nextContent;
}
