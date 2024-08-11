package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
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

    private final String namespaceGroup;

    private final String dataId;

    private final String content;

    @DataBoundConstructor
    public NacosConfigModifyApplyStep(
            String nacosId, String toolUrl, String namespaceGroup, String dataId, String content) {
        this.nacosId = nacosId;
        this.toolUrl = toolUrl;
        this.namespaceGroup = namespaceGroup;
        this.dataId = dataId;
        this.content = content;
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
            String[] ng = step.getNamespaceGroup().split("/");

            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("NacosConfigModifyApplyStep", taskListener);
            logger.log(
                    "Applying nacos config. toolUrl:%s, nacosId:%s, namespace:%s, group:%s, dataId:%s",
                    step.getToolUrl(), step.getNacosId(), ng[0], ng[1], step.getDataId());

            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            if (StringUtils.isBlank(step.getContent())) {
                throw new IllegalArgumentException("Content is blank");
            }

            NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                    .nacosId(step.getNacosId())
                    .namespaceId(ng[0])
                    .group(ng[1])
                    .dataId(step.getDataId())
                    .content(step.getContent())
                    .build();
            String result = client.nacosConfigModifyApply(nacosConfigReq);
            Map<String, Object> map = new HashMap<>();
            map.put("result", result);
            return map;
        }
    }
}
