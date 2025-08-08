package io.jenkins.plugins.configops.model.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2025-07-15
 */
@Setter
@Getter
@ToString
public class DatabaseProvisionResp implements Serializable {
    private static final long serialVersionUID = 2403018903143091158L;

    private List<String> messages;

    private String password;
}
