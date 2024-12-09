package io.jenkins.plugins.configops.elasticsearch;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import io.jenkins.plugins.configops.model.dto.ElasticsearchChangeDTO;
import io.jenkins.plugins.configops.model.dto.ElasticsearchChangeSetDTO;
import io.jenkins.plugins.configops.model.req.ElasticsearchChangeSetReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.ConfigOpsException;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

@Getter
public class ElasticsearchChangeSetApplyStep extends Step implements Serializable {
    private static final long serialVersionUID = -8378224065415377409L;
    private final String esId;
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

    private String toolUrl;

    @DataBoundConstructor
    public ElasticsearchChangeSetApplyStep(@NonNull String esId, @NonNull String changeLogFile) {
        this.toolUrl = Constants.DEFAULT_TOOL_URL;
        this.esId = esId;
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setVars(HashMap<String, String> vars) {
        this.vars = vars;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = Util.fixEmpty(contexts);
    }

    @DataBoundSetter
    public void setCount(Integer count) {
        this.count = count;
    }

    public void setVars(String vars) {
        this.vars = Utils.parseChangeLogVars(vars);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<List<ElasticsearchChangeSetDTO>> {

        private static final long serialVersionUID = 7685627166933351893L;

        private final ElasticsearchChangeSetApplyStep step;

        public StepExecutionImpl(@NonNull StepContext context, ElasticsearchChangeSetApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<ElasticsearchChangeSetDTO> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("ElasticsearchChangeSetApply", taskListener);
            FilePath workspace = Objects.requireNonNull(getContext().get(FilePath.class), "Step workspace is null");
            FilePath changeLog = workspace.child(step.getChangeLogFile());
            if (!changeLog.exists()) {
                throw new IllegalArgumentException("Change log file not found");
            }
            logger.log("Start apply changeset from: %s", step.getChangeLogFile());

            ElasticsearchChangeSetReq req = new ElasticsearchChangeSetReq();
            req.setEsId(step.getEsId());
            req.setVars(step.getVars());
            req.setContexts(step.getContexts());
            req.setCount(step.getCount());

            List<ElasticsearchChangeSetDTO> result = changeLog.act(new RemoteCallable(step.getToolUrl(), req));

            int changeSetNum = 0;
            int successNum = 0;
            int failNum = 0;
            int skipNum = 0;

            // 拼装响应日志打印
            if (Objects.nonNull(result) && !result.isEmpty()) {
                changeSetNum = result.size();
                for (ElasticsearchChangeSetDTO changeSet : result) {
                    logger.log(
                            false,
                            "======== Run ChangeSet: %s::%s =========",
                            changeSet.getId(),
                            Util.fixNull(changeSet.getAuthor()));
                    for (ElasticsearchChangeDTO change : changeSet.getChanges()) {
                        String status;
                        String message = Objects.requireNonNullElse(change.getMessage(), "");
                        if (Objects.isNull(change.getSuccess())) {
                            status = "Skip";
                            skipNum++;
                        } else if (change.getSuccess()) {
                            status = "Success";
                            successNum++;
                        } else {
                            status = "Failure";
                            failNum++;
                        }
                        logger.log(
                                false,
                                "Run Change %s: %s::%s\n%s",
                                status,
                                change.getMethod(),
                                change.getPath(),
                                message);
                    }
                }
            }

            logger.log(
                    "Run summary\nChangeset num:       %d\nSuccess change num:  %d\nFailure change num:  %d\nSkip change num:     %d",
                    changeSetNum, successNum, failNum, skipNum);

            if (failNum > 0) {
                throw new ConfigOpsException("Run elasticsearch changelog unsuccessful");
            }

            return result;
        }
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<List<ElasticsearchChangeSetDTO>> {
        private static final long serialVersionUID = 4786751496167500017L;
        private final String toolUrl;
        private final ElasticsearchChangeSetReq req;

        private RemoteCallable(String toolUrl, ElasticsearchChangeSetReq req) {
            this.toolUrl = toolUrl;
            this.req = req;
        }

        @Override
        public List<ElasticsearchChangeSetDTO> invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            req.setChangeLogFile(f.getAbsolutePath());
            return client.applyElasticsearchChangeSet(req);
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
            return "Elasticsearch Change Set Apply";
        }

        @Override
        public String getFunctionName() {
            return "elasticsearchChangeSetApply";
        }

        @POST
        public FormValidation doCheckEsId(@QueryParameter("esId") String esId) {
            if (Utils.isNullOrEmpty(esId)) {
                return FormValidation.error("ElasticsearchID is required");
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
            String esId = formData.getString("esId");
            String changeLogFile = formData.getString("changeLogFile");
            Object contexts = formData.getOrDefault("contexts", null);
            Object count = formData.getOrDefault("count", null);
            Object vars = formData.getOrDefault("vars", null);
            ElasticsearchChangeSetApplyStep step = new ElasticsearchChangeSetApplyStep(esId, changeLogFile);
            step.setContexts(contexts.toString());
            step.setVars(vars.toString());
            if (Objects.nonNull(count) && StringUtils.isNotBlank(count.toString())) {
                step.setCount(Integer.parseInt(count.toString()));
            }
            return step;
        }
    }
}
