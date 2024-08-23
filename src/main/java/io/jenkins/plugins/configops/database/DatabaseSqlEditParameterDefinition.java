package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import io.jenkins.plugins.configops.model.dto.DatabaseSqlDTO;
import io.jenkins.plugins.configops.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Bruce.Wu
 * @date 2024-08-20
 */
@Setter
@Getter
@Log
public class DatabaseSqlEditParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = -2141712008531755931L;

    private final List<String> dbs;

    @DataBoundConstructor
    public DatabaseSqlEditParameterDefinition(String name, @NonNull List<String> dbs) {
        super(StringUtils.defaultIfBlank(name, "DATABASE_SQL"));
        Utils.requireNotEmpty(dbs, "Choices must not empty");
        this.dbs = dbs;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        JSONObject selectedValue = jo.getJSONObject("value");
        List<DatabaseSqlDTO> result = new ArrayList<>();
        JSONArray dbNames = selectedValue.names();
        for (int i = 0; i < dbNames.size(); i++) {
            String db = dbNames.getString(i);
            JSONObject item = selectedValue.getJSONObject(db);
            boolean checked = item.getBoolean("checked");
            if (checked) {
                DatabaseSqlDTO dto = new DatabaseSqlDTO();
                dto.setDatabase(db);
                dto.setSql(item.getString("content"));
                result.add(dto);
            }
        }
        return new DatabaseConfigChoicesParameterValue(getName(), result);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        try {
            JSONObject jo = req.getSubmittedForm();
            return createValue(req, jo);
        } catch (Exception e) {
            throw new IllegalStateException("Create value error.", e);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class DatabaseConfigChoicesParameterValue extends ParameterValue {

        private final List<DatabaseSqlDTO> choices;

        public DatabaseConfigChoicesParameterValue(String name, List<DatabaseSqlDTO> choices) {
            super(name);
            this.choices = choices;
        }

        @Override
        public Object getValue() {
            Map<String, Object> map = new HashMap<>();
            map.put("values", choices);
            return map;
        }
    }

    @Extension
    @Symbol("databaseSqlEdit")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(DatabaseSqlEditParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Database SQL Edit Parameter";
        }
    }
}
