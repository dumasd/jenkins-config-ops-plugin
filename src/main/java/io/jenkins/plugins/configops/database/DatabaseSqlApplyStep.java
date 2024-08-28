package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.dto.DatabaseSqlDTO;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

@Getter
@Setter
@ToString
@Log
public class DatabaseSqlApplyStep extends Step implements Serializable {

    private static final long serialVersionUID = -3437547656737490722L;
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
        private static final long serialVersionUID = -1872031188937161306L;

        private final DatabaseSqlApplyStep step;

        public StepExecutionImpl(@NonNull StepContext context, DatabaseSqlApplyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);

            Logger logger = new Logger("SQLApply", taskListener);
            Launcher launcher = getContext().get(Launcher.class);
            VirtualChannel channel = Utils.getChannel(launcher);
            Run<?, ?> run = getContext().get(Run.class);
            if (Objects.isNull(run)) {
                throw new IllegalArgumentException("Step run is null");
            }

            SqlApplyResultAction resultAction = run.getAction(SqlApplyResultAction.class);
            if (Objects.isNull(resultAction)) {
                resultAction = new SqlApplyResultAction();
                run.addAction(resultAction);
            }

            for (DatabaseSqlDTO item : step.getItems()) {
                logger.log("========== Execute database sql start:%s", item.getDatabase());
                DatabaseConfigApplyResp resp =
                        channel.call(new RemoteExecutionCallable(step.getToolUrl(), step.getDatabaseId(), item));
                logger.log(false, "Database URL:%s", resp.getDatabase());
                for (DatabaseConfigApplyResp.SqlResult sqlResult : resp.getResult()) {
                    logger.log(false, "%s\n", sqlResult.getSql());
                    logger.log(
                            false,
                            "Affected row count: %s",
                            sqlResult.getRowcount().toString());
                    SqlApplyDetailAction sqlApplyDetail = new SqlApplyDetailAction()
                            .setStepHash(String.valueOf(this.hashCode()))
                            .setUrl(resp.getDatabase())
                            .setDatabase(item.getDatabase())
                            .setRowcount(sqlResult.getRowcount())
                            .setSql(sqlResult.getSql())
                            .setRows(sqlResult.getRows());
                    run.addAction(sqlApplyDetail);
                    resultAction.addDetail(sqlApplyDetail);
                }
                logger.log("========== Execute database sql end.");
            }
            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable implements Callable<DatabaseConfigApplyResp, Exception> {
        private static final long serialVersionUID = 354632146040812160L;
        private final String toolUrl;
        private final String databaseId;
        private final DatabaseSqlDTO item;

        public RemoteExecutionCallable(String toolUrl, String databaseId, DatabaseSqlDTO item) {
            this.toolUrl = toolUrl;
            this.databaseId = databaseId;
            this.item = item;
        }

        @Override
        public DatabaseConfigApplyResp call() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            DatabaseConfigReq databaseConfigReq = DatabaseConfigReq.builder()
                    .dbId(databaseId)
                    .database(item.getDatabase())
                    .sql(item.getSql())
                    .build();

            return client.databaseConfigApply(databaseConfigReq);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
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
