package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import io.jenkins.plugins.configops.model.req.CommonEditContentReq;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Log
@Setter
@Getter
public class NacosConfigAlterParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = -6182842383814244217L;

    private List<NacosConfigDTO> items;
    private boolean selectAll = true;

    @DataBoundConstructor
    public NacosConfigAlterParameterDefinition(String name, @NonNull List<NacosConfigDTO> items) {
        super(StringUtils.defaultIfBlank(name, "NACOS_CONFIG_ALTER"));
        this.items = new ArrayList<>(items);
        this.items.sort((o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            return o1.fullName().compareTo(o2.fullName());
        });
    }

    @DataBoundSetter
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public int getItemSelectSize() {
        return Math.min(10, items.size());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        JSONObject value = jo.getJSONObject("value");
        JSONArray ids = value.names();
        List<NacosConfigModifyDTO> result = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.getString(i);
            JSONObject item = value.getJSONObject(id);
            boolean checked = item.getBoolean("checked");
            if (checked) {
                NacosConfigDTO ncd = findConfig(id);
                if (Objects.isNull(ncd)) {
                    throw new IllegalArgumentException("Nacos config not found: id:" + id);
                }
                String format = item.getOrDefault("format", ncd.getFormat()).toString();
                NacosConfigModifyDTO dto = new NacosConfigModifyDTO();
                dto.setNamespace(ncd.getNamespace());
                dto.setGroup(ncd.getGroup());
                dto.setDataId(ncd.getDataId());
                dto.setContent(ncd.getContent());
                dto.setFormat(format);
                dto.setNextContent(item.getString("content"));
                result.add(dto);
            }
        }
        return new NacosConfigAlterParameterValue(getName(), result);
    }

    private NacosConfigDTO findConfig(String id) {
        for (NacosConfigDTO dto : items) {
            if (Objects.equals(dto.getId(), id)) {
                return dto;
            }
        }
        return null;
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
    public static class NacosConfigAlterParameterValue extends ParameterValue {

        private static final long serialVersionUID = 4313454723473437740L;
        private final List<NacosConfigModifyDTO> result;

        public NacosConfigAlterParameterValue(String name, List<NacosConfigModifyDTO> result) {
            super(name);
            this.result = result;
        }

        @Override
        public Object getValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("values", result);
            return map;
        }
    }

    @Extension
    @Symbol("nacosConfigAlter")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(NacosConfigAlterParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Alter Parameter";
        }

        private String getJenkinsConfigOpsUrl() {
            String jenkinsConfigOpsUrl = null;
            for (NodeProperty<?> property : Jenkins.get().getGlobalNodeProperties()) {
                if (property instanceof EnvironmentVariablesNodeProperty) {
                    EnvironmentVariablesNodeProperty envVarsProperty = (EnvironmentVariablesNodeProperty) property;
                    EnvVars envVars = envVarsProperty.getEnvVars();
                    jenkinsConfigOpsUrl = envVars.get(Constants.ENV_JENKINS_CONFIG_OPS_URL);
                    break;
                }
            }
            if (Objects.isNull(jenkinsConfigOpsUrl)) {
                throw new IllegalArgumentException("JENKINS_CONFIG_OPS_URL env not found");
            }
            return jenkinsConfigOpsUrl;
        }

        @JavaScriptMethod(name = "patchContent")
        public NacosConfigModifyPreviewResp doPatchContent(
                @QueryParameter("content") String content,
                @QueryParameter("edit") String edit,
                @QueryParameter("format") String format)
                throws Exception {
            String jenkinsConfigOpsUrl = getJenkinsConfigOpsUrl();
            ConfigOpsClient client = new ConfigOpsClient(jenkinsConfigOpsUrl);
            CommonEditContentReq commonEditContentReq = CommonEditContentReq.builder()
                    .edit(edit)
                    .content(content)
                    .format(format)
                    .build();
            return client.commonPatchContent(commonEditContentReq);
        }

        @JavaScriptMethod(name = "deleteContent")
        public NacosConfigModifyPreviewResp doDeleteContent(
                @QueryParameter("content") String content,
                @QueryParameter("edit") String edit,
                @QueryParameter("format") String format)
                throws Exception {
            String jenkinsConfigOpsUrl = getJenkinsConfigOpsUrl();
            ConfigOpsClient client = new ConfigOpsClient(jenkinsConfigOpsUrl);
            CommonEditContentReq commonEditContentReq = CommonEditContentReq.builder()
                    .edit(edit)
                    .content(content)
                    .format(format)
                    .build();
            return client.commonDeleteContent(commonEditContentReq);
        }

        @JavaScriptMethod(name = "deleteAndPatchContent")
        public NacosConfigModifyPreviewResp doDeleteAndPatchContent(
                @QueryParameter("content") String content,
                @QueryParameter("patch") String patch,
                @QueryParameter("delete") String delete,
                @QueryParameter("format") String format)
                throws Exception {
            String jenkinsConfigOpsUrl = getJenkinsConfigOpsUrl();
            ConfigOpsClient client = new ConfigOpsClient(jenkinsConfigOpsUrl);
            CommonEditContentReq commonEditContentReq = CommonEditContentReq.builder()
                    .edit(delete)
                    .content(content)
                    .format(format)
                    .build();
            NacosConfigModifyPreviewResp deleteResp = client.commonDeleteContent(commonEditContentReq);
            commonEditContentReq = CommonEditContentReq.builder()
                    .edit(patch)
                    .content(deleteResp.getNextContent())
                    .format(format)
                    .build();
            return client.commonPatchContent(commonEditContentReq);
        }
    }
}
