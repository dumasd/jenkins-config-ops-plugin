package io.jenkins.plugins.configops.nacos;

import com.alibaba.fastjson2.JSON;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.configops.model.dto.NacosChoiceOptionDTO;
import io.jenkins.plugins.configops.model.dto.NacosConfigFileDTO;
import io.jenkins.plugins.configops.utils.Utils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

@Deprecated
@Log
@Setter
@Getter
public class NacosConfigChoicesParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = -7576372260248282963L;

    private final List<NacosConfigFileDTO> choices;

    @DataBoundConstructor
    public NacosConfigChoicesParameterDefinition(String name, List<NacosConfigFileDTO> choices) {
        super(StringUtils.defaultIfBlank(name, "NACOS_CHOICE"));
        Utils.requireNotEmpty(choices, "Choices must not empty");
        this.choices = choices;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        log.log(Level.INFO, "Create value with jo. {0}", jo);
        JSONObject selectedValue = jo.getJSONObject("value");
        List<NacosChoiceOptionDTO> list = new ArrayList<>();
        for (Object item : selectedValue.values()) {
            if (item instanceof JSONObject) {
                JSONObject itemObj = (JSONObject) item;
                boolean checked = itemObj.getBoolean("check");
                if (checked) {
                    String namespace = itemObj.getString("namespace");
                    String group = itemObj.getString("group");
                    String dataId = itemObj.getString("dataId");
                    String version = itemObj.getOrDefault("version", "").toString();
                    list.add(new NacosChoiceOptionDTO(namespace, group, dataId, version));
                }
            }
        }
        boolean showPreview =
                Boolean.parseBoolean(jo.getOrDefault("showPreview", true).toString());
        return new NacosConfigChoicesParameterValue(getName(), list, showPreview);
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
    public static class NacosConfigChoicesParameterValue extends ParameterValue {

        private static final long serialVersionUID = -3674127270786698110L;

        private List<NacosChoiceOptionDTO> choices;

        private boolean showPreview;

        public NacosConfigChoicesParameterValue(String name, List<NacosChoiceOptionDTO> choices, boolean showPreview) {
            super(name);
            this.choices = choices;
            this.showPreview = showPreview;
        }

        @Override
        public void buildEnvironment(Run<?, ?> build, EnvVars env) {
            env.put(name + "_ITEMS", JSON.toJSONString(choices));
            env.put(name + "_PREVIEW", Boolean.toString(showPreview));
        }

        @Override
        public Object getValue() {
            Map<String, Object> value = new HashMap<>();
            value.put("values", choices);
            value.put("showPreview", showPreview);
            return value;
        }
    }

    // @Extension
    // @Symbol("nacosConfigChoices")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(NacosConfigChoicesParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Choices Parameter";
        }

        @POST
        public ListBoxModel doFillNamespaceGroupItems(@QueryParameter(value = "json", required = true) String json) {
            List<NacosConfigFileDTO> list = JSON.parseArray(json, NacosConfigFileDTO.class);
            Set<String> set = new HashSet<>();
            list.forEach(e -> set.add(e.getNamespace() + "/" + e.getGroup()));
            List<String> ngs = new ArrayList<>(set);
            ngs.sort(Comparator.naturalOrder());
            ListBoxModel result = new ListBoxModel();
            ngs.forEach(result::add);
            if (!result.isEmpty()) {
                result.get(0).selected = true;
            }
            return result;
        }

        public ListBoxModel doFillDataIdItems(
                @QueryParameter(value = "json", required = true) String json,
                @QueryParameter(value = "namespaceGroup", required = true) String namespaceGroup) {
            List<NacosConfigFileDTO> list = JSON.parseArray(json, NacosConfigFileDTO.class);
            ListBoxModel result = new ListBoxModel();

            for (NacosConfigFileDTO file : list) {
                if (Objects.equals(namespaceGroup, file.getNamespace() + "/" + file.getGroup())) {
                    result.add(file.getDataId());
                }
            }

            if (!result.isEmpty()) {
                result.get(0).selected = true;
            }
            return result;
        }

        public ListBoxModel doFillVersionItems(
                @QueryParameter(value = "json", required = true) String json,
                @QueryParameter(value = "namespaceGroup", required = true) String namespaceGroup,
                @QueryParameter(value = "dataId", required = true) String dataId) {
            List<NacosConfigFileDTO> list = JSON.parseArray(json, NacosConfigFileDTO.class);
            ListBoxModel result = new ListBoxModel();
            result.add(new ListBoxModel.Option("Not Select", "", true));
            for (NacosConfigFileDTO file : list) {
                if (Objects.equals(namespaceGroup, file.spliceNamespaceGroup())
                        && Objects.equals(file.getDataId(), dataId)) {
                    if (CollectionUtils.isNotEmpty(file.getVersions())) {
                        file.getVersions().forEach(result::add);
                    }
                    break;
                }
            }
            return result;
        }
    }
}
