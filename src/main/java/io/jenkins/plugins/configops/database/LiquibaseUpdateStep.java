package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.req.DatabaseRunLiquibaseReq;
import io.jenkins.plugins.configops.model.resp.DatabaseRunLiquibaseResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.ConfigOpsException;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jenkins.MasterToSlaveFileCallable;
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
import org.kohsuke.stapler.DataBoundConstructor;

@Getter
@Setter
@ToString
@Log
public class LiquibaseUpdateStep extends Step implements Serializable {

    private static final long serialVersionUID = -3437547656737490722L;
    /**
     * Database ID
     */
    private final String databaseId;
    /**
     * Config Ops工具地址
     */
    private String toolUrl;
    /**
     * Change log 文件
     */
    private String file;

    @DataBoundConstructor
    public LiquibaseUpdateStep(@NonNull String databaseId, String toolUrl, @NonNull String file) {
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.databaseId = databaseId;
        this.file = file;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<Map<String, Object>> {
        private static final long serialVersionUID = -1872031188937161306L;

        private final LiquibaseUpdateStep step;

        public StepExecutionImpl(@NonNull StepContext context, LiquibaseUpdateStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            FilePath workspace = getContext().get(FilePath.class);
            if (Objects.isNull(workspace)) {
                throw new ConfigOpsException("Step workspace is null");
            }

            FilePath changelogRootPath = workspace.child(step.getFile());
            if (!changelogRootPath.exists() || changelogRootPath.isDirectory()) {
                throw new ConfigOpsException("Changelog root file not exists or dictionary. file:" + step.getFile());
            }

            Logger logger = new Logger("LiquibaseUpdate", taskListener);
            DatabaseRunLiquibaseResp resp =
                    changelogRootPath.act(new RemoteExecutionCallable(step.getToolUrl(), step.getDatabaseId()));

            logger.log("Liquibase update return code: %s", resp.getRetcode());
            if (StringUtils.isNotBlank(resp.getStdout())) {
                logger.log("Liquibase update stdout:\n%s", resp.getStdout());
            }
            if (StringUtils.isNotBlank(resp.getStderr())) {
                logger.log("Liquibase update stderr:\n%s", resp.getStderr());
            }
            if (!resp.isSuccess()) {
                throw new ConfigOpsException("Execute liquibase update unsuccessful");
            }
            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable extends MasterToSlaveFileCallable<DatabaseRunLiquibaseResp> {
        private static final long serialVersionUID = 354632146040812160L;
        private final String toolUrl;
        private final String databaseId;

        public RemoteExecutionCallable(String toolUrl, String databaseId) {
            this.toolUrl = toolUrl;
            this.databaseId = databaseId;
        }

        @Override
        public DatabaseRunLiquibaseResp invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            DatabaseRunLiquibaseReq req = DatabaseRunLiquibaseReq.builder()
                    .dbId(databaseId)
                    .cwd(f.getParentFile().getAbsolutePath())
                    .command("update")
                    .args("--changelog-file " + f.getName())
                    .build();
            return client.databaseRunLiquibase(req);
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
            return "Liquibase Update Step";
        }

        @Override
        public String getFunctionName() {
            return "liquibaseUpdate";
        }
    }
}
