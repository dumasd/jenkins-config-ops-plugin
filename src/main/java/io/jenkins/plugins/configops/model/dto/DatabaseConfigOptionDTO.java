package io.jenkins.plugins.configops.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfigOptionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String database;

    private List<String> sqlFileNames;


}
