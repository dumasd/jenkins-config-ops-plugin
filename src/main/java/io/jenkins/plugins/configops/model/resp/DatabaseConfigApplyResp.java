package io.jenkins.plugins.configops.model.resp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class DatabaseConfigApplyResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String database;

    private List<SqlResult> result = new ArrayList<>();

    @Setter
    @Getter
    @ToString
    public static class SqlResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String sql;
        private Long rowcount = 0L;
        private List<LinkedHashMap<String, Object>> rows;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(3, 1.0F);
            map.put("sql", sql);
            map.put("rowcount", rowcount);
            map.put("rows", rows);
            return map;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(2, 1.0F);
        map.put("database", database);
        map.put("result", result.stream().map(SqlResult::toMap).collect(Collectors.toList()));
        return map;
    }
}
