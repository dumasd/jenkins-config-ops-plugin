package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.DatabaseConfigOptionDTO;
import io.jenkins.plugins.configops.model.req.DatabaseConfigReq;
import io.jenkins.plugins.configops.model.resp.DatabaseConfigApplyResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
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
     * 目录
     */
    private final String workingDir;

    private final List<DatabaseConfigOptionDTO> items;

    @DataBoundConstructor
    public DatabaseConfigApplyStep(
            @NonNull String databaseId,
            @NonNull String toolUrl,
            String workingDir,
            @NonNull List<DatabaseConfigOptionDTO> items) {
        this.databaseId = databaseId;
        this.toolUrl = toolUrl;
        this.workingDir = workingDir;
        this.items = items;
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
            FilePath workspace = this.getContext().get(FilePath.class);
            Objects.requireNonNull(workspace, "Workspace not found");
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(step.getWorkingDir())) {
                workingDirPath = workspace.child(step.getWorkingDir());
            }
            for (DatabaseConfigOptionDTO item : step.getItems()) {
                if (CollectionUtils.isEmpty(item.getSqlFileNames())) {
                    continue;
                }
                for (String sqlFileName : item.getSqlFileNames()) {
                    FilePath sqlFilePath =
                            workingDirPath.child(String.format("%s/%s", item.getDatabase(), sqlFileName));
                    String sql = FileUtils.readFileToString(new File(sqlFilePath.getRemote()), StandardCharsets.UTF_8);
                    DatabaseConfigReq databaseConfigReq = DatabaseConfigReq.builder()
                            .dbId(step.getDatabaseId())
                            .database(item.getDatabase())
                            .sql(sql)
                            .build();
                    logger.log(
                            "########## Execute sql file start. database:%s, sqlFileName:%s",
                            item.getDatabase(), sqlFileName);
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
            return "Database Config Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "databaseConfigApply";
        }
    }
}
