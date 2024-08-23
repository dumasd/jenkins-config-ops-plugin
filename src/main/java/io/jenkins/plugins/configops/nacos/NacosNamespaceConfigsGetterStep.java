package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.req.NacosGetConfigsReq;
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
public class NacosNamespaceConfigsGetterStep extends Step implements Serializable {

    private static final long serialVersionUID = 3343339503818030544L;
    private String toolUrl;
    private final String nacosId;
    private final List<String> namespaces;

    @DataBoundConstructor
    public NacosNamespaceConfigsGetterStep(String toolUrl, @NonNull String nacosId, @NonNull List<String> namespaces) {
        Utils.requireNotEmpty(namespaces, "Namespaces must not empty");
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.nacosId = nacosId;
        this.namespaces = namespaces;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousStepExecution<List<NacosConfigDTO>> {

        private static final long serialVersionUID = -2478979948634104040L;
        private final NacosNamespaceConfigsGetterStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosNamespaceConfigsGetterStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<NacosConfigDTO> run() throws Exception {
            Launcher launcher = getContext().get(Launcher.class);
            return Utils.getChannel(launcher).call(new RemoteExecutionCallable(step));
        }
    }

    public static class RemoteExecutionCallable implements Callable<List<NacosConfigDTO>, Exception> {

        private static final long serialVersionUID = -6581134205208786663L;
        private final NacosNamespaceConfigsGetterStep step;

        public RemoteExecutionCallable(NacosNamespaceConfigsGetterStep step) {
            this.step = step;
        }

        @Override
        public List<NacosConfigDTO> call() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            NacosGetConfigsReq req = new NacosGetConfigsReq(step.getNacosId(), step.getNamespaces());
            return client.getNacosConfigs(req);
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
            classes.add(Launcher.class);
            return classes;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Configs Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigsGet";
        }
    }
}
