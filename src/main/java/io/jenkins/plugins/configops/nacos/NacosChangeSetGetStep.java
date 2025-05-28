package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.model.req.NacosGetChangeSetReq;
import io.jenkins.plugins.configops.model.resp.NacosGetChangeSetResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.MasterToSlaveFileCallable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
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

@Setter
@Getter
@ToString
public class NacosChangeSetGetStep extends Step implements Serializable {
    private static final long serialVersionUID = 5373505661508888051L;

    private String toolUrl = Constants.DEFAULT_TOOL_URL;

    private final String nacosId;

    private final String changeLogFile;
    /**
     * 变量
     */
    private HashMap<String, String> vars;
    /**
     * 指定上下文
     */
    private String contexts;

    private Integer count;

    private String allowedDataIds;

    @DataBoundConstructor
    public NacosChangeSetGetStep(String nacosId, String changeLogFile) {
        this.nacosId = nacosId;
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setCount(Integer count) {
        this.count = count;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    @DataBoundSetter
    public void setVars(HashMap<String, String> vars) {
        this.vars = vars;
    }

    @DataBoundSetter
    public void setAllowedDataIds(String allowedDataIds) {
        this.allowedDataIds = Util.fixEmptyAndTrim(allowedDataIds);
    }

    public void setVars(String vars) {
        this.vars = Utils.parseChangeLogVars(vars);
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
            FilePath workspace = Objects.requireNonNull(getContext().get(FilePath.class), "Step workspace is null");
            FilePath changeLog = workspace.child(step.getChangeLogFile());
            if (!changeLog.exists()) {
                throw new IllegalArgumentException("Change log file not found");
            }
            NacosGetChangeSetResp resp = changeLog.act(new RemoteCallable(
                    step.getToolUrl(),
                    step.getNacosId(),
                    step.getCount(),
                    step.getContexts(),
                    step.getVars(),
                    step.getAllowedDataIds()));
            logger.log("Get ChangeSet from file: %s", step.getChangeLogFile());
            if (CollectionUtils.isNotEmpty(resp.getChanges())) {
                for (NacosConfigDTO nc : resp.getChanges()) {
                    if (StringUtils.isBlank(nc.getId())) {
                        nc.setId(RandomStringUtils.randomAlphanumeric(20));
                    }
                }
            }
            logger.log("Found ChangeSet. ids:%s", Objects.toString(resp.getIds()));
            return resp;
        }
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<NacosGetChangeSetResp> {

        private static final long serialVersionUID = -5550176413772105947L;
        private final String toolUrl;
        private final String nacosId;
        private final Integer count;
        private final String contexts;
        private final HashMap<String, String> vars;
        private final String allowedDataIds;

        private RemoteCallable(
                String toolUrl,
                String nacosId,
                Integer count,
                String contexts,
                HashMap<String, String> vars,
                String allowedDataIds) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
            this.count = count;
            this.contexts = contexts;
            this.vars = vars;
            this.allowedDataIds = allowedDataIds;
        }

        @Override
        public NacosGetChangeSetResp invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            ConfigOpsClient client =
                    new ConfigOpsClient(StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL));
            Set<String> allowedDataIdSet = null;
            if (allowedDataIds != null) {
                allowedDataIdSet = Arrays.stream(allowedDataIds.split(","))
                        .filter(e -> Objects.nonNull(Util.fixEmptyAndTrim(e)))
                        .collect(Collectors.toSet());
            }
            NacosGetChangeSetReq nacosGetChangeSetReq = new NacosGetChangeSetReq()
                    .setNacosId(nacosId)
                    .setChangeLogFile(f.getAbsolutePath())
                    .setContexts(contexts)
                    .setCount(count)
                    .setVars(vars)
                    .setAllowedDataIds(allowedDataIdSet);
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

        @POST
        public FormValidation doCheckNacosId(@QueryParameter("nacosId") String nacosId) {
            if (Utils.isNullOrEmpty(nacosId)) {
                return FormValidation.error("NacosId is required");
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

        @Override
        public Step newInstance(@Nullable StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            String nacosId = formData.getString("nacosId");
            String changeLogFile = formData.getString("changeLogFile");
            Object contexts = formData.getOrDefault("contexts", null);
            Object count = formData.getOrDefault("count", null);
            Object vars = formData.getOrDefault("vars", null);
            Object allowedDataIds = formData.getOrDefault("allowedDataIds", null);
            NacosChangeSetGetStep step = new NacosChangeSetGetStep(nacosId, changeLogFile);
            if (Objects.nonNull(contexts) && StringUtils.isNotBlank(contexts.toString())) {
                step.setContexts(contexts.toString());
            }
            if (Objects.nonNull(count) && StringUtils.isNotBlank(count.toString())) {
                step.setCount(Integer.parseInt(count.toString()));
            }
            if (Objects.nonNull(vars)) {
                step.setVars(vars.toString());
            }
            if (Objects.nonNull(allowedDataIds)) {
                step.setAllowedDataIds(allowedDataIds.toString());
            }
            return step;
        }
    }
}
