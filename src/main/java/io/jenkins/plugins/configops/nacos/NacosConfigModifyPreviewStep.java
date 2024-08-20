package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.NacosChoiceOptionDTO;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.resp.ListNacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

@Setter
@Getter
@Log
public class NacosConfigModifyPreviewStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String workingDir;

    private final String nacosId;

    private final String toolUrl;

    private final List<NacosChoiceOptionDTO> items;

    @DataBoundConstructor
    public NacosConfigModifyPreviewStep(
            String workingDir, String nacosId, String toolUrl, List<NacosChoiceOptionDTO> items) {
        this.workingDir = workingDir;
        this.nacosId = nacosId;
        this.toolUrl = toolUrl;
        this.items = items;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new NacosConfigsGetterStepExecution(context, this);
    }

    public static class NacosConfigsGetterStepExecution
            extends SynchronousNonBlockingStepExecution<ListNacosConfigModifyPreviewResp> {

        private final NacosConfigModifyPreviewStep step;

        public NacosConfigsGetterStepExecution(@NonNull StepContext context, NacosConfigModifyPreviewStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected ListNacosConfigModifyPreviewResp run() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            FilePath workspace = this.getContext().get(FilePath.class);
            Objects.requireNonNull(workspace, "Workspace not found");
            List<NacosConfigModifyDTO> previewDTOList =
                    new ArrayList<>(step.getItems().size());
            Objects.requireNonNull(workspace, "Workspace not found");
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(step.getWorkingDir())) {
                workingDirPath = workspace.child(step.getWorkingDir());
            }

            for (NacosChoiceOptionDTO item : step.getItems()) {
                FilePath fullDataIdFile = workingDirPath.child(
                        String.format("%s/%s/%s", item.getNamespace(), item.getGroup(), item.getDataId()));
                String fullCnt =
                        FileUtils.readFileToString(new File(fullDataIdFile.getRemote()), StandardCharsets.UTF_8);
                String patchCnt = null;
                if (StringUtils.isNotBlank(item.getVersion())) {
                    FilePath patchDataIdFile = workingDirPath.child(String.format(
                            "%s/%s/%s/%s", item.getNamespace(), item.getGroup(), item.getVersion(), item.getDataId()));
                    patchCnt =
                            FileUtils.readFileToString(new File(patchDataIdFile.getRemote()), StandardCharsets.UTF_8);
                }

                NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                        .nacosId(step.getNacosId())
                        .namespaceId(item.getNamespace())
                        .group(item.getGroup())
                        .dataId(item.getDataId())
                        .fullContent(fullCnt)
                        .patchContent(patchCnt)
                        .build();
                NacosConfigModifyPreviewResp resp = client.nacosConfigModifyPreview(nacosConfigReq);
                NacosConfigModifyDTO dto = new NacosConfigModifyDTO();
                dto.setNamespace(item.getNamespace());
                dto.setGroup(item.getGroup());
                dto.setDataId(item.getDataId());
                dto.setFormat(resp.getFormat());
                dto.setContent(resp.getContent());
                dto.setNextContent(resp.getNextContent());
                previewDTOList.add(dto);
            }
            return new ListNacosConfigModifyPreviewResp(previewDTOList);
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
