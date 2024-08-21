package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.NacosNamespaceDTO;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
public class NacosNamespacesGetterStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

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

    public static class StepExecutionImpl
            extends SynchronousNonBlockingStepExecution<List<NacosNamespaceDTO>> {

        private final NacosNamespacesGetterStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosNamespacesGetterStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<NacosNamespaceDTO> run() throws Exception {
            //TaskListener taskListener = getContext().get(TaskListener.class);
            //Logger logger = new Logger("NacosNamespaceGetterStep", taskListener);
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            return client.getNacosNamespaces(step.getNacosId());
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
            return "Nacos Namespace Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosNamespacesGet";
        }
    }
}
