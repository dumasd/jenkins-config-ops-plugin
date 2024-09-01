package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.req.NacosGetChangeSetReq;
import io.jenkins.plugins.configops.model.resp.NacosGetChangeSetResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jenkins.MasterToSlaveFileCallable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

@Setter
@Getter
@ToString
public class NacosChangeSetGetStep extends Step implements Serializable {
    private static final long serialVersionUID = 5373505661508888051L;

    private final String nacosId;

    private final String toolUrl;

    private final String changeLogFile;

    @DataBoundConstructor
    public NacosChangeSetGetStep(String nacosId, String toolUrl, String changeLogFile) {
        this.nacosId = nacosId;
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.changeLogFile = changeLogFile;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<NacosGetChangeSetResp> {

        private static final long serialVersionUID = -7586842755857966658L;

        private final NacosChangeSetGetStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosChangeSetGetStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected NacosGetChangeSetResp run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("NacosChangeSetGet", taskListener);
            FilePath workspace = getContext().get(FilePath.class);
            if (Objects.isNull(workspace)) {
                throw new IllegalArgumentException("Step workspace is null");
            }
            FilePath changeLog = workspace.child(step.getChangeLogFile());
            if (!changeLog.exists()) {
                throw new IllegalArgumentException("Change log file not found");
            }
            NacosGetChangeSetResp resp = changeLog.act(new RemoteCallable(step.getToolUrl(), step.getNacosId()));
            logger.log("Get ChangeSet from file: %s", step.getChangeLogFile());
            if (CollectionUtils.isNotEmpty(resp.getChanges())) {
                for (NacosConfigDTO nc : resp.getChanges()) {
                    if (StringUtils.isBlank(nc.getId())) {
                        nc.setId(RandomStringUtils.randomAlphanumeric(20));
                    }
                }
            }
            logger.log("Found ChangeSet. id:%s", resp.getId());
            return resp;
        }
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<NacosGetChangeSetResp> {

        private static final long serialVersionUID = -5550176413772105947L;
        private final String toolUrl;
        private final String nacosId;

        private RemoteCallable(String toolUrl, String nacosId) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
        }

        @Override
        public NacosGetChangeSetResp invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            NacosGetChangeSetReq nacosGetChangeSetReq =
                    new NacosGetChangeSetReq().setNacosId(nacosId).setChangeLogFile(f.getAbsolutePath());
            return client.getChangeSet(nacosGetChangeSetReq);
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
            return "Nacos Change Set Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosChangeSetGet";
        }
    }
}
