package io.jenkins.plugins.configops.model.req;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-21
 */
@Setter
@Getter
@ToString
@Builder
public class CommonEditContentReq implements Serializable {

    private static final long serialVersionUID = -5334064749190268920L;

    private String content;

    private String edit;

    private String format;
}
