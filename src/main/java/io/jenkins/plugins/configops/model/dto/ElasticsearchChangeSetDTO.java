package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ElasticsearchChangeSetDTO implements Serializable {
    private static final long serialVersionUID = -2996418766103980478L;

    private String id;
    private String author;
    private String comment;

    private List<ElasticsearchChangeDTO> changes;
}
