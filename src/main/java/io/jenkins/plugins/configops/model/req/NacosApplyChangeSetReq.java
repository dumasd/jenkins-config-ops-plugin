package io.jenkins.plugins.configops.model.req;

import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;

import java.io.Serializable;
import java.util.List;

/**
 * @author Bruce.Wu
 * @date 2024-08-30
 */
public class NacosApplyChangeSetReq implements Serializable {
    private static final long serialVersionUID = -801656933583501739L;

    private String nacosId;

    private String changeSetId;

    private List<NacosConfigDTO> changes;

    public String getNacosId() {
        return nacosId;
    }

    public void setNacosId(String nacosId) {
        this.nacosId = nacosId;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public List<NacosConfigDTO> getChanges() {
        return changes;
    }

    public void setChanges(List<NacosConfigDTO> changes) {
        this.changes = changes;
    }
}
