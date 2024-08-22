package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.DatabaseSqlDTO;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

@Getter
@Setter
@ToString
public class DatabaseSqlApplyStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Database ID
     */
    private final String databaseId;
    /**
     * Config Ops工具地址
     */
    private String toolUrl;

    private final List<DatabaseSqlDTO> items;

    @DataBoundConstructor
    public DatabaseSqlApplyStep(@NonNull String databaseId, String toolUrl, @NonNull List<DatabaseSqlDTO> items) {
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.databaseId = databaseId;
        this.items = items;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<Map<String, Object>> {

        private final DatabaseSqlApplyStep step;

        public StepExecutionImpl(@NonNull StepContext context, DatabaseSqlApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("DatabaseConfigApplyStep", taskListener);
            ConfigOpsClient client = new ConfigOpsClient(step.getToolUrl());
            for (DatabaseSqlDTO item : step.getItems()) {
                DatabaseConfigReq databaseConfigReq = DatabaseConfigReq.builder()
                        .dbId(step.getDatabaseId())
                        .database(item.getDatabase())
                        .sql(item.getSql())
                        .build();
                logger.log("########## Execute sql file start. database:%s", item.getDatabase());
                DatabaseConfigApplyResp resp = client.databaseConfigApply(databaseConfigReq);
                logger.log(false, "Database URL:%s", resp.getDatabase());
                for (DatabaseConfigApplyResp.SqlResult sqlResult : resp.getResult()) {
                    logger.log(false, "%s", sqlResult.getSql());
                    logger.log(
                            false,
                            "Affected row count: %s\n",
                            sqlResult.getRowcount().toString());
                }
                logger.log("========== Execute sql file end.");
            }
            return Collections.emptyMap();
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
            return "Database SQL Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "databaseSqlApply";
        }
    }
}
