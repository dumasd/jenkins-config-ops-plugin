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
import io.jenkins.plugins.configops.model.req.NacosApplyChangeSetReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.kohsuke.stapler.DataBoundSetter;

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

    private String toolUrl = Constants.DEFAULT_TOOL_URL;

    private final List<String> changeSetIds;

    private final List<NacosConfigDTO> items;

    private List<NacosConfigDTO> deleteItems;

    @DataBoundConstructor
    public NacosChangeSetApplyStep(
            @NonNull String nacosId, @NonNull List<String> changeSetIds, @NonNull List<NacosConfigDTO> items) {
        this.nacosId = nacosId;
        this.changeSetIds = changeSetIds;
        this.items = items;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setDeleteItems(List<NacosConfigDTO> deleteItems) {
        this.deleteItems = deleteItems;
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
            Launcher launcher = getContext().get(Launcher.class);
            Logger logger = new Logger("NacosConfigApply", taskListener);
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

            List<NacosConfigDTO> deleteChanges = new ArrayList<>();
            if (step.getDeleteItems() != null) {
                deleteChanges = step.getDeleteItems().stream()
                        .map(e -> {
                            NacosConfigDTO nc = new NacosConfigDTO();
                            nc.setNamespace(e.getNamespace());
                            nc.setGroup(e.getGroup());
                            nc.setDataId(e.getDataId());
                            nc.setFormat(e.getFormat());
                            return nc;
                        })
                        .collect(Collectors.toList());
            }

            VirtualChannel channel = Utils.getChannel(launcher);
            logger.log(
                    "Applying change log config. toolUrl:%s, nacosId:%s, changeSetIds:%s",
                    step.getToolUrl(), step.getNacosId(), step.getChangeSetIds());

            channel.call(new RemoteExecutionCallable(
                    step.getToolUrl(), step.getNacosId(), step.getChangeSetIds(), changes, deleteChanges));

            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable implements Callable<String, Exception> {

        private static final long serialVersionUID = 4711346178005514552L;
        private final String toolUrl;
        private final String nacosId;
        private final List<String> changeSetIds;
        private final List<NacosConfigDTO> changes;
        private final List<NacosConfigDTO> deleteChanges;

        public RemoteExecutionCallable(
                String toolUrl,
                String nacosId,
                List<String> changeSetIds,
                List<NacosConfigDTO> changes,
                List<NacosConfigDTO> deleteChanges) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
            this.changeSetIds = changeSetIds;
            this.changes = changes;
            this.deleteChanges = deleteChanges;
        }

        @Override
        public String call() throws Exception {
            ConfigOpsClient client =
                    new ConfigOpsClient(StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL));
            NacosApplyChangeSetReq req = new NacosApplyChangeSetReq();
            req.setNacosId(nacosId);
            req.setChangeSetIds(changeSetIds);
            req.setChanges(changes);
            req.setDeleteChanges(deleteChanges);
            return client.applyChangeSet(req);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }
}
