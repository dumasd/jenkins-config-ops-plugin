package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import io.jenkins.plugins.configops.model.dto.DatabaseConfigOptionDTO;
import io.jenkins.plugins.configops.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruce.Wu
 * @date 2024-08-20
 */
@Setter
@Getter
@Log
public class DatabaseConfigChoicesParameterDefinition extends ParameterDefinition {

    private final List<DatabaseConfigOptionDTO> choices;

    @DataBoundConstructor
    public DatabaseConfigChoicesParameterDefinition(String name, @NonNull List<DatabaseConfigOptionDTO> choices) {
        super(StringUtils.defaultIfBlank(name, "DATABASE_CHOICE"));
        Utils.requireNotEmpty(choices, "Choices must not empty");
        this.choices = choices;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
//        log.log(Level.INFO, "Create value with jo. {0}", jo);
        JSONObject selectedValue = jo.getJSONObject("value");
        List<DatabaseConfigOptionDTO> result = new ArrayList<>();
        for (Object item : selectedValue.values()) {
            if (item instanceof JSONObject) {
                JSONObject itemObj = (JSONObject) item;
                boolean checked = itemObj.getBoolean("check");
                if (checked) {
                    String database = itemObj.getString("database");
                    String sqlFileName = itemObj.getString("sqlFileName");
                    result.add(new DatabaseConfigOptionDTO(database, Collections.singletonList(sqlFileName)));
                }
            }
        }
        Utils.requireNotEmpty(result, "Not select database sql file");
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

        private final List<DatabaseConfigOptionDTO> choices;

        public DatabaseConfigChoicesParameterValue(String name, List<DatabaseConfigOptionDTO> choices) {
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
    @Symbol("databaseConfigChoices")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(DatabaseConfigChoicesParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Database Config Choices Parameter";
        }
    }
}
