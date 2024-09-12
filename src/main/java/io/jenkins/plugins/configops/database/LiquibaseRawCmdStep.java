package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
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
import java.nio.file.Files;
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
import org.kohsuke.stapler.DataBoundSetter;

/**
 * 运行Liquibase原始命令
 * <a href="https://docs.liquibase.com/commands/home.html">Liquibase Commands</a>
 *
 * @author Bruce.Wu
 * @date 2024-09-12
 */
@Getter
@Setter
@ToString
@Log
public class LiquibaseRawCmdStep extends Step implements Serializable {

    private static final long serialVersionUID = -3437547656737490722L;
    /**
     * Config Ops工具地址
     */
    private String toolUrl;
    /**
     * Database ID
     */
    private final String databaseId;
    /**
     * 在哪个路径下执行
     */
    private String cwd;
    /**
     * Liquibase命令
     */
    private final String command;
    /**
     * Liquibase命令参数
     */
    private final String args;

    @DataBoundConstructor
    public LiquibaseRawCmdStep(
            String toolUrl, @NonNull String databaseId, String cwd, @NonNull String command, String args) {
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.databaseId = databaseId;
        this.cwd = cwd;
        this.command = command;
        this.args = args;
    }

    @DataBoundSetter
    public void setCwd(String cwd) {
        this.cwd = cwd;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<Map<String, Object>> {
        private static final long serialVersionUID = -1872031188937161306L;

        private final LiquibaseRawCmdStep step;

        public StepExecutionImpl(@NonNull StepContext context, LiquibaseRawCmdStep step) {
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
            FilePath cwdPath = workspace;
            if (StringUtils.isNotBlank(step.getCwd())) {
                cwdPath = workspace.child(step.getCwd());
            }

            Logger logger = new Logger("LiquibaseCommand", taskListener);

            logger.log(
                    "cwd:%s, command:%s, args:%s",
                    Util.fixNull(step.getCwd()), step.getCommand(), Util.fixNull(step.getArgs()));

            DatabaseRunLiquibaseResp resp = cwdPath.act(new RemoteExecutionCallable(
                    step.getToolUrl(), step.getDatabaseId(), step.getCommand(), step.getArgs()));

            logger.log("Liquibase run command return code: %s", resp.getRetcode());
            if (StringUtils.isNotBlank(resp.getStdout())) {
                logger.log("Liquibase run command stdout:\n%s", resp.getStdout());
            }
            if (StringUtils.isNotBlank(resp.getStderr())) {
                logger.log("Liquibase run command stderr:\n%s", resp.getStderr());
            }
            if (!resp.isSuccess()) {
                throw new ConfigOpsException("Execute liquibase run command unsuccessful");
            }
            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable extends MasterToSlaveFileCallable<DatabaseRunLiquibaseResp> {
        private static final long serialVersionUID = 354632146040812160L;
        private final String toolUrl;
        private final String databaseId;
        private final String command;
        private final String args;

        public RemoteExecutionCallable(String toolUrl, String databaseId, String command, String args) {
            this.toolUrl = toolUrl;
            this.databaseId = databaseId;
            this.command = command;
            this.args = args;
        }

        @Override
        public DatabaseRunLiquibaseResp invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            if (!Files.isDirectory(f.toPath())) {
                throw new ConfigOpsException("Run command cwd is not a dictionary: " + f.getName());
            }
            DatabaseRunLiquibaseReq req = DatabaseRunLiquibaseReq.builder()
                    .dbId(databaseId)
                    .cwd(f.getAbsolutePath())
                    .command(command)
                    .args(args)
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
            return "Liquibase Raw Command Step";
        }

        @Override
        public String getFunctionName() {
            return "liquibaseRawCmd";
        }
    }
}
