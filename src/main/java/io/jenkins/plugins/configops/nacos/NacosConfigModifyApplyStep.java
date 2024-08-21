package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 修改确认
 *
 * @author Bruce.Wu
 * @date 2024-08-11
 */
@Setter
@Getter
@ToString
public class NacosConfigModifyApplyStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nacosId;

    private final String toolUrl;

    private final List<NacosConfigModifyDTO> items;

    @DataBoundConstructor
    public NacosConfigModifyApplyStep(
            @NonNull String nacosId, String toolUrl, @NonNull List<NacosConfigModifyDTO> items) {
        this.nacosId = nacosId;
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.items = items;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new NacosConfigModifyApplyStepExecution(context, this);
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
            return "Nacos Config Modify Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigModifyApply";
        }
    }

    public static class NacosConfigModifyApplyStepExecution
            extends SynchronousNonBlockingStepExecution<Map<String, Object>> {

        private final NacosConfigModifyApplyStep step;

        public NacosConfigModifyApplyStepExecution(@NonNull StepContext context, NacosConfigModifyApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("NacosConfigModifyApplyStep", taskListener);
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            for (NacosConfigModifyDTO item : step.getItems()) {
                Utils.requireNotBlank(item.getNamespace(), "Namespace is blank");
                Utils.requireNotBlank(item.getGroup(), "Group is blank");
                Utils.requireNotBlank(item.getDataId(), "DataId is blank");
                Utils.requireNotBlank(item.getNextContent(), "Next Content is blank");
            }

            Map<String, Object> map = new HashMap<>();
            for (NacosConfigModifyDTO item : step.getItems()) {
                logger.log(
                        "Applying nacos config. toolUrl:%s, nacosId:%s, namespace:%s, group:%s, dataId:%s",
                        step.getToolUrl(), step.getNacosId(), item.getNamespace(), item.getGroup(), item.getDataId());
                NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                        .nacosId(step.getNacosId())
                        .namespaceId(item.getNamespace())
                        .group(item.getGroup())
                        .dataId(item.getDataId())
                        .content(item.getNextContent())
                        .build();
                String result = client.nacosConfigModifyApply(nacosConfigReq);
            }
            return map;
        }
    }
}
