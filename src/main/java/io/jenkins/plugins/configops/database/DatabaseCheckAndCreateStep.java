package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.configops.model.req.DatabaseProvisionReq;
import io.jenkins.plugins.configops.model.resp.DatabaseProvisionResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.ConfigOpsException;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

@Log
@Getter
public class DatabaseCheckAndCreateStep extends Builder implements SimpleBuildStep {

    /**
     * Config-ops tool url
     */
    private String toolUrl = Constants.DEFAULT_TOOL_URL;
    /**
     * Database ID
     */
    private final String databaseId;
    /**
     * The database name
     */
    private final String dbName;
    /**
     * The database username
     */
    private final String user;
    /**
     * The user ip source
     */
    private String ipsource;
    /**
     * The permissions
     */
    private String permissions;

    @DataBoundConstructor
    public DatabaseCheckAndCreateStep(String databaseId, String dbName, String user) {
        this.databaseId = databaseId;
        this.dbName = dbName;
        this.user = user;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setIpsource(String ipsource) {
        this.ipsource = ipsource;
    }

    @DataBoundSetter
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Logger logger = new Logger("DatabaseCheckAndCreate", listener);
        DatabaseProvisionReq req = new DatabaseProvisionReq();
        req.setDbId(Util.fixEmptyAndTrim(databaseId));
        req.setDbName(Util.fixEmptyAndTrim(dbName));
        req.setUser(Util.fixEmptyAndTrim(user));
        req.setIpsource(Util.fixEmptyAndTrim(ipsource));
        String fixPermissions = Util.fixEmptyAndTrim(permissions);
        List<String> permissionList = null;
        if (fixPermissions != null) {
            permissionList = Arrays.stream(fixPermissions.split(","))
                    .map(Util::fixEmptyAndTrim)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        req.setPermissions(permissionList);
        DatabaseProvisionResp resp = workspace.act(new RunCheckAndCreateCallable(toolUrl, req));
        if (resp.getMessages() != null) {
            for (String msg : resp.getMessages()) {
                logger.log(msg);
            }
        }
    }

    public static class RunCheckAndCreateCallable implements Callable<DatabaseProvisionResp, ConfigOpsException> {

        private static final long serialVersionUID = 3093068774226603461L;
        private final String toolUrl;
        private final DatabaseProvisionReq req;

        public RunCheckAndCreateCallable(String toolUrl, DatabaseProvisionReq req) {
            this.toolUrl = toolUrl;
            this.req = req;
        }

        @Override
        public DatabaseProvisionResp call() throws ConfigOpsException {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            return client.databaseProvision(req);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }

    @Extension
    @Symbol("databaseCheckAndCreate")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Database check and create";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @POST
        public FormValidation doCheckDatabaseId(@QueryParameter("databaseId") String databaseId) {
            if (Utils.isNullOrEmpty(databaseId)) {
                return FormValidation.error("DatabaseId is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckDbName(@QueryParameter("dbName") String dbName) {
            if (Utils.isNullOrEmpty(dbName)) {
                return FormValidation.error("DB name is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckUser(@QueryParameter("user") String user) {
            if (Utils.isNullOrEmpty(user)) {
                return FormValidation.error("User is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckPermissions(@QueryParameter("permissions") String permissions) {
            if (Utils.isNullOrEmpty(permissions)) {
                return FormValidation.ok();
            }
            String[] permissionList = permissions.split(",");
            for (String permission : permissionList) {
                if (Utils.isNullOrEmpty(permission)) {
                    return FormValidation.error("Empty permission in the list");
                }
            }
            return FormValidation.ok();
        }
    }
}
