package io.jenkins.plugins.configops.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.DatabaseConfigOptionDTO;
import io.jenkins.plugins.configops.utils.SqlFileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Bruce.Wu
 * @date 2024-08-20
 */
@Setter
@Getter
@ToString
public class DatabaseConfigGetterStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private String workingDir;

    private int maxFileNum = 100;

    @DataBoundConstructor
    public DatabaseConfigGetterStep(String workingDir) {
        this.workingDir = workingDir;
    }

    @DataBoundSetter
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @DataBoundSetter
    public void setMaxFileNum(int maxFileNum) {
        this.maxFileNum = maxFileNum;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new DatabaseConfigGetterStepExecution(context, workingDir);
    }

    public static class DatabaseConfigGetterStepExecution
            extends SynchronousNonBlockingStepExecution<List<DatabaseConfigOptionDTO>> {

        private final String workingDir;

        public DatabaseConfigGetterStepExecution(@NonNull StepContext context, String workingDir) {
            super(context);
            this.workingDir = workingDir;
        }

        @Override
        protected List<DatabaseConfigOptionDTO> run() throws Exception {
            FilePath workspace = this.getContext().get(FilePath.class);
            Objects.requireNonNull(workspace, "Workspace not found");
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(workingDir)) {
                workingDirPath = workspace.child(workingDir);
            }
            List<DatabaseConfigOptionDTO> result = new ArrayList<>();

            List<FilePath> databaseDirs = workingDirPath.listDirectories();
            for (FilePath databaseDir : databaseDirs) {
                List<FilePath> sqlFiles = databaseDir.list(new SqlFileFilter());
                if (CollectionUtils.isNotEmpty(sqlFiles)) {
                    DatabaseConfigOptionDTO dto = new DatabaseConfigOptionDTO();
                    dto.setDatabase(databaseDir.getName());
                    dto.setSqlFileNames(sqlFiles.stream()
                            .map(FilePath::getName)
                            .sorted(Comparator.reverseOrder())
                            .collect(Collectors.toList()));
                    result.add(dto);
                }
            }
            if (CollectionUtils.isEmpty(result)) {
                throw new IllegalStateException("No sql config found");
            }
            return result;
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
            return "Database Config Getter Step";
        }

        @Override
        public String getFunctionName() {
            return "databaseConfigGet";
        }
    }
}
