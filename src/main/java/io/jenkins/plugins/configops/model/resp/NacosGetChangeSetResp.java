package io.jenkins.plugins.configops.model.resp;

import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NacosGetChangeSetResp implements Serializable {
    private static final long serialVersionUID = 7173185023189172803L;

    private String id;

    private String author;

    private String comment;

    private List<NacosConfigDTO> changes;
}
