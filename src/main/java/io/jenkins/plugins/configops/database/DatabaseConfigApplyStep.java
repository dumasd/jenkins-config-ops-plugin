package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

@Getter
@Setter
@ToString
public class DatabaseConfigApplyStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Database ID
     */
    private final String databaseId;
    /**
     * Config Ops工具地址
     */
    private final String toolUrl;
    /**
     * SQL脚本
     */
    private final String sql;

    @DataBoundConstructor
    public DatabaseConfigApplyStep(String databaseId, String toolUrl, String sql) {
        this.databaseId = databaseId;
        this.toolUrl = toolUrl;
        this.sql = sql;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new DatabaseConfigApplyStepExecution(context, this);
    }

    public static class DatabaseConfigApplyStepExecution
            extends SynchronousNonBlockingStepExecution<Map<String, Object>> {

        private final DatabaseConfigApplyStep step;

        public DatabaseConfigApplyStepExecution(@NonNull StepContext context, DatabaseConfigApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("DatabaseConfigApplyStep", taskListener);
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            DatabaseConfigReq databaseConfigReq = DatabaseConfigReq.builder()
                    .dbId(step.getDatabaseId())
                    .sql(step.getSql())
                    .build();
            DatabaseConfigApplyResp resp = client.databaseConfigApply(databaseConfigReq);
            logger.log("Execute database: %s", resp.getDatabase());
            for (DatabaseConfigApplyResp.SqlResult sqlResult : resp.getResult()) {
                logger.log("sql:%s", sqlResult.getSql());
                logger.log("Affected row count %s", sqlResult.getRowcount().toString());
            }
            return resp.toMap();
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
            return "Database Config Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "databaseConfigApply";
        }
    }
}
