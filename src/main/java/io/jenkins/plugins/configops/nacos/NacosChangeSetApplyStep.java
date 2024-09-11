package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import io.jenkins.plugins.configops.model.req.NacosApplyChangeSetReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Bruce.Wu
 * @date 2024-08-30
 */
@Setter
@Getter
@ToString
public class NacosChangeSetApplyStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nacosId;

    private final String toolUrl;

    private final List<String> changeSetIds;

    private final List<NacosConfigModifyDTO> items;

    @DataBoundConstructor
    public NacosChangeSetApplyStep(
            @NonNull String nacosId,
            String toolUrl,
            @NonNull List<String> changeSetIds,
            @NonNull List<NacosConfigModifyDTO> items) {
        this.nacosId = nacosId;
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.changeSetIds = changeSetIds;
        this.items = items;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
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
            return "Nacos Change Set Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "nacosChangeSetApply";
        }
    }

    public static class StepExecutionImpl extends SynchronousStepExecution<Map<String, Object>> {

        private static final long serialVersionUID = -1372992911615088880L;
        private final NacosChangeSetApplyStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosChangeSetApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("NacosConfigApply", taskListener);
            Launcher launcher = getContext().get(Launcher.class);
            List<NacosConfigDTO> changes = step.getItems().stream()
                    .map(e -> {
                        NacosConfigDTO nc = new NacosConfigDTO();
                        nc.setNamespace(e.getNamespace());
                        nc.setGroup(e.getGroup());
                        nc.setDataId(e.getDataId());
                        nc.setContent(e.getNextContent());
                        nc.setFormat(e.getFormat());
                        return nc;
                    })
                    .collect(Collectors.toList());
            VirtualChannel channel = Utils.getChannel(launcher);
            logger.log(
                    "Applying change log config. toolUrl:%s, nacosId:%s, changeSetIds:%s",
                    step.getToolUrl(), step.getNacosId(), step.getChangeSetIds());

            channel.call(
                    new RemoteExecutionCallable(step.getToolUrl(), step.getNacosId(), step.getChangeSetIds(), changes));

            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable implements Callable<String, Exception> {

        private static final long serialVersionUID = 4711346178005514552L;
        private final String toolUrl;
        private final String nacosId;
        private final List<String> changeSetIds;
        private final List<NacosConfigDTO> changes;

        public RemoteExecutionCallable(
                String toolUrl, String nacosId, List<String> changeSetIds, List<NacosConfigDTO> changes) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
            this.changeSetIds = changeSetIds;
            this.changes = changes;
        }

        @Override
        public String call() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            NacosApplyChangeSetReq req = new NacosApplyChangeSetReq();
            req.setNacosId(nacosId);
            req.setChangeSetIds(changeSetIds);
            req.setChanges(changes);
            return client.applyChangeSet(req);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }
}
