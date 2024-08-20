package io.jenkins.plugins.configops.nacos;

import com.alibaba.fastjson2.JSON;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

@Log
@Setter
@Getter
public class NacosConfigEditParameterDefinition extends ParameterDefinition {

    private List<NacosConfigModifyDTO> items;

    @DataBoundConstructor
    public NacosConfigEditParameterDefinition(String name, List<NacosConfigModifyDTO> items) {
        super(StringUtils.defaultIfBlank(name, "NACOS_EDIT"));
        this.items = items;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        JSONObject value = jo.getJSONObject("value");
        JSONArray keys = value.names();
        List<NacosConfigModifyDTO> result = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            if (StringUtils.isNotBlank(key)) {
                try {
                    Integer.parseInt(key);
                    JSONObject item = value.getJSONObject(key);
                    NacosConfigModifyDTO dto = new NacosConfigModifyDTO();
                    dto.setNamespace(item.getString("namespace"));
                    dto.setGroup(item.getString("group"));
                    dto.setDataId(item.getString("dataId"));
                    dto.setFormat(item.getString("format"));
                    dto.setContent(item.getString("content"));
                    dto.setNextContent(item.getString("nextContent"));
                    result.add(dto);
                } catch (NumberFormatException ignored) {

                }
            }
        }

        return new NacosConfigEditParameterValue(getName(), result);
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
    public static class NacosConfigEditParameterValue extends ParameterValue {

        private final List<NacosConfigModifyDTO> result;

        public NacosConfigEditParameterValue(String name, List<NacosConfigModifyDTO> result) {
            super(name);
            this.result = result;
        }

        @Override
        public void buildEnvironment(Run<?, ?> build, EnvVars env) {
            env.put(name, JSON.toJSONString(result));
        }

        @Override
        public Object getValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("values", result);
            return map;
        }
    }

    @Extension
    @Symbol("nacosConfigEdit")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(NacosConfigEditParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Edit Parameter";
        }
    }
}
