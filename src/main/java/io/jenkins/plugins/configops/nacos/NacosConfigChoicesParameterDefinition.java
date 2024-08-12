package io.jenkins.plugins.configops.nacos;

import com.alibaba.fastjson2.JSON;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.configops.model.dto.NacosFileDTO;

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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

@Log
@Setter
@Getter
public class NacosConfigChoicesParameterDefinition extends ParameterDefinition {

    private final String json;

    @DataBoundConstructor
    public NacosConfigChoicesParameterDefinition(@NonNull String name, String json) {
        super(name);
        this.json = json;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        log.log(Level.INFO, "Create value with jo. {0}", jo);
        JSONObject versionMap = jo.getJSONObject("value");
        boolean showPreview =
                Boolean.parseBoolean(versionMap.getOrDefault("showPreview", true).toString());
        return new NacosConfigChoicesParameterValue(
                getName(),
                versionMap.getString("namespaceGroup"),
                versionMap.getString("dataId"),
                versionMap.getOrDefault("version", "").toString(),
                showPreview);
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

        private String namespaceGroup;
        private String dataId;
        private String version;
        private boolean showPreview;

        public NacosConfigChoicesParameterValue(String name, String namespaceGroup, String dataId, String version, boolean showPreview) {
            super(name);
            this.namespaceGroup = namespaceGroup;
            this.dataId = dataId;
            this.version = Util.fixNull(version);
            this.showPreview = showPreview;
        }

        @Override
        public void buildEnvironment(Run<?, ?> build, EnvVars env) {
            env.put(name + "_NG", namespaceGroup);
            env.put(name + "_DATE_ID", dataId);
            env.put(name + "_VERSION", version);
            env.put(name + "_PREVIEW", Boolean.toString(showPreview));
        }

        @Override
        public Object getValue() {
            Map<String, Object> value = new HashMap<>();
            value.put("namespaceGroup", namespaceGroup);
            value.put("dataId", dataId);
            value.put("version", version);
            value.put("showPreview", showPreview);
            return value;
        }
    }

    @Extension
    @Symbol("nacosConfigChoices")
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
            List<NacosFileDTO> list = JSON.parseArray(json, NacosFileDTO.class);
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
            List<NacosFileDTO> list = JSON.parseArray(json, NacosFileDTO.class);
            ListBoxModel result = new ListBoxModel();

            for (NacosFileDTO file : list) {
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
            List<NacosFileDTO> list = JSON.parseArray(json, NacosFileDTO.class);
            ListBoxModel result = new ListBoxModel();
            result.add(new ListBoxModel.Option("Not Select", "", true));
            for (NacosFileDTO file : list) {
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
