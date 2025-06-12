package io.jenkins.plugins.configops.graphdb;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import io.jenkins.plugins.configops.model.req.GraphdbChangeSetReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jenkins.MasterToSlaveFileCallable;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @since 2025-06-12
 */
@Getter
public class GraphdbChangeSetApplyStep extends Step implements Serializable {

    private static final long serialVersionUID = 6034461270454367052L;

    private final String systemId;
    private final String changeLogFile;
    private HashMap<String, String> vars;
    private String contexts;
    private Integer count;
    private String toolUrl;

    @DataBoundConstructor
    public GraphdbChangeSetApplyStep(String systemId, String changeLogFile) {
        this.systemId = systemId;
        this.changeLogFile = changeLogFile;
        this.toolUrl = Constants.DEFAULT_TOOL_URL;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    @DataBoundSetter
    public void setCount(Integer count) {
        this.count = count;
    }

    @DataBoundSetter
    public void setVars(HashMap<String, String> vars) {
        this.vars = vars;
    }

    public void setVars(String vars) {
        this.vars = Utils.parseChangeLogVars(vars);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<String> {

        private static final long serialVersionUID = 3816054477256605623L;
        private final GraphdbChangeSetApplyStep step;

        public StepExecutionImpl(@NonNull StepContext context, GraphdbChangeSetApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("GraphdbChangeSetApply", taskListener);
            FilePath workspace = Objects.requireNonNull(getContext().get(FilePath.class), "Step workspace is null");
            FilePath changeLog = workspace.child(step.getChangeLogFile());
            if (!changeLog.exists()) {
                throw new IllegalArgumentException("Change log file not found");
            }
            logger.log("Apply changeset. systemId: %s, changeLogFile: %s", step.getSystemId(), step.getChangeLogFile());
            GraphdbChangeSetReq req = new GraphdbChangeSetReq();
            req.setSystemId(step.getSystemId());
            req.setVars(step.getVars());
            req.setContexts(step.getContexts());
            req.setCount(step.getCount());
            changeLog.act(new RemoteCallable(step.getToolUrl(), req));
            return null;
        }
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<String> {
        private static final long serialVersionUID = 8111623700467016526L;
        private final String toolUrl;
        private final GraphdbChangeSetReq req;

        public RemoteCallable(String toolUrl, GraphdbChangeSetReq req) {
            this.toolUrl = toolUrl;
            this.req = req;
        }

        @Override
        public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            req.setChangeLogFile(f.getAbsolutePath());
            return client.applyGraphdbChangeSet(req);
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(TaskListener.class);
            classes.add(FilePath.class);
            return classes;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Graphdb Change Set Apply";
        }

        @Override
        public String getFunctionName() {
            return "graphdbChangeSetApply";
        }

        @POST
        public FormValidation doCheckEsId(@QueryParameter("systemId") String esId) {
            if (Utils.isNullOrEmpty(esId)) {
                return FormValidation.error("Graphdb system id is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckChangeLogFile(@QueryParameter("changeLogFile") String changeLogFile) {
            if (Utils.isNullOrEmpty(changeLogFile)) {
                return FormValidation.error("ChangeLogFile is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckVars(@QueryParameter("vars") String vars) {
            if (!Utils.isValidChangeLogVars(vars)) {
                return FormValidation.error("Vars is invalid");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckCount(@QueryParameter("count") String count) {
            if (Utils.isNotEmpty(count)) {
                try {
                    Integer.parseInt(count);
                } catch (NumberFormatException e) {
                    return FormValidation.error("Count is not a number");
                }
            }
            return FormValidation.ok();
        }

        @Override
        public Step newInstance(@Nullable StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            String esId = formData.getString("systemId");
            String changeLogFile = formData.getString("changeLogFile");
            Object contexts = formData.getOrDefault("contexts", null);
            Object count = formData.getOrDefault("count", null);
            Object vars = formData.getOrDefault("vars", null);
            GraphdbChangeSetApplyStep step = new GraphdbChangeSetApplyStep(esId, changeLogFile);
            step.setContexts(contexts.toString());
            step.setVars(vars.toString());
            if (Objects.nonNull(count) && StringUtils.isNotBlank(count.toString())) {
                step.setCount(Integer.parseInt(count.toString()));
            }
            return step;
        }
    }
}
