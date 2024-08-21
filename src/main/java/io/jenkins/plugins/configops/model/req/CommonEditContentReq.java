package io.jenkins.plugins.configops.model.req;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Bruce.Wu
 * @date 2024-08-21
 */
@Setter
@Getter
@ToString
@Builder
public class CommonEditContentReq implements Serializable {

    private String content;

    private String edit;

    private String format;
}
