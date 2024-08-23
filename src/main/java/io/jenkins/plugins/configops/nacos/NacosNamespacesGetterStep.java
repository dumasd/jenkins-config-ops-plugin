package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import io.jenkins.plugins.configops.model.dto.NacosNamespaceDTO;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@Setter
@Getter
public class NacosNamespacesGetterStep extends Step implements Serializable {

    private static final long serialVersionUID = -91756507826981799L;
    private String toolUrl;
    private final String nacosId;

    @DataBoundConstructor
    public NacosNamespacesGetterStep(String toolUrl, @NonNull String nacosId) {
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.nacosId = nacosId;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousStepExecution<List<NacosNamespaceDTO>> {

        private static final long serialVersionUID = 4085998441975555091L;
        private final NacosNamespacesGetterStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosNamespacesGetterStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<NacosNamespaceDTO> run() throws Exception {
            // TaskListener taskListener = getContext().get(TaskListener.class);
            // Logger logger = new Logger("NacosNamespaceGetterStep", taskListener);
            Launcher launcher = getContext().get(Launcher.class);
            return Utils.getChannel(launcher).call(new RemoteExecutionCallable(step));
        }
    }

    public static class RemoteExecutionCallable implements Callable<List<NacosNamespaceDTO>, Exception> {

        private static final long serialVersionUID = -3481025969224250274L;
        private final NacosNamespacesGetterStep step;

        public RemoteExecutionCallable(NacosNamespacesGetterStep step) {
            this.step = step;
        }

        @Override
        public List<NacosNamespaceDTO> call() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            return client.getNacosNamespaces(step.getNacosId());
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
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
            return "Nacos Namespace Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosNamespacesGet";
        }
    }
}
