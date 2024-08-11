package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@Setter
@Getter
@Log
public class NacosConfigModifyPreviewStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String workingDir;

    private final String nacosId;

    private final String toolUrl;

    private final String namespaceGroup;

    private final String dataId;

    private String version;

    @DataBoundConstructor
    public NacosConfigModifyPreviewStep(
            String workingDir, String nacosId, String toolUrl, String namespaceGroup, String dataId) {
        this.workingDir = workingDir;
        this.nacosId = nacosId;
        this.toolUrl = toolUrl;
        this.namespaceGroup = namespaceGroup;
        this.dataId = dataId;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new NacosConfigsGetterStepExecution(context, this);
    }

    public static class NacosConfigsGetterStepExecution
            extends SynchronousNonBlockingStepExecution<Map<String, Object>> {

        private final NacosConfigModifyPreviewStep step;

        protected NacosConfigsGetterStepExecution(@NonNull StepContext context, NacosConfigModifyPreviewStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            FilePath workspace = this.getContext().get(FilePath.class);
            if (Objects.isNull(workspace)) {
                throw new NullPointerException("Workspace not found");
            }
            FilePath fullDataIdFile = workspace.child(String.format("%s/%s/%s", step.getWorkingDir(), step.getNamespaceGroup(), step.getDataId()));
            String fullCnt = FileUtils.readFileToString(new File(fullDataIdFile.getRemote()), StandardCharsets.UTF_8);
            String patchCnt = null;
            if (StringUtils.isNotBlank(step.getVersion())) {
                FilePath patchDataIdFile = workspace.child(String.format(
                        "%s/%s/%s/%s",
                        step.getWorkingDir(), step.getNamespaceGroup(), step.getVersion(), step.getDataId()));
                patchCnt = FileUtils.readFileToString(new File(patchDataIdFile.getRemote()), StandardCharsets.UTF_8);
            }
            String[] ng = step.getNamespaceGroup().split("/");
            NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                    .nacosId(step.getNacosId())
                    .namespaceId(ng[0])
                    .group(ng[1])
                    .dataId(step.getDataId())
                    .fullContent(fullCnt)
                    .patchContent(patchCnt)
                    .build();
            NacosConfigModifyPreviewResp resp = client.nacosConfigModifyPreview(nacosConfigReq);
            return resp.toMap();
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(Run.class);
            classes.add(TaskListener.class);
            classes.add(FilePath.class);
            return classes;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Modify Preview Step";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigModifyPreview";
        }
    }
}
