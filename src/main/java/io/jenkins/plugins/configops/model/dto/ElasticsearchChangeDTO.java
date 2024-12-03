package io.jenkins.plugins.configops.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ElasticsearchChangeDTO implements Serializable {
    private static final long serialVersionUID = -7638958892607223645L;

    private String method;
    private String path;
    private String body;

    private Boolean success;
    private String message;
}
