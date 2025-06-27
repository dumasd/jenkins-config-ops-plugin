package io.jenkins.plugins.configops.model.req;

import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-30
 */
@Setter
@Getter
@ToString
public class NacosApplyChangeSetReq implements Serializable {
    private static final long serialVersionUID = -801656933583501739L;

    private String nacosId;

    private List<String> changeSetIds;

    private List<NacosConfigDTO> changes;

    private List<NacosConfigDTO> deleteChanges;
}
