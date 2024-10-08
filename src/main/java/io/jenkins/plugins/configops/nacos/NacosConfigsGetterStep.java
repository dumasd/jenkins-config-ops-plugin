package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.NacosConfigFileDTO;
import io.jenkins.plugins.configops.utils.FileFileFilter;
import io.jenkins.plugins.configops.utils.Utils;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@Deprecated
@Setter
@Getter
public class NacosConfigsGetterStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private String workingDir;

    @DataBoundConstructor
    public NacosConfigsGetterStep(String workingDir) {
        this.workingDir = workingDir;
    }

    @DataBoundSetter
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new NacosConfigsGetterStepExecution(context, workingDir);
    }

    public static class NacosConfigsGetterStepExecution
            extends SynchronousNonBlockingStepExecution<List<NacosConfigFileDTO>> {

        private final String workingDir;

        public NacosConfigsGetterStepExecution(@NonNull StepContext context, String workingDir) {
            super(context);
            this.workingDir = workingDir;
        }

        @Override
        protected List<NacosConfigFileDTO> run() throws Exception {
            FilePath workspace = this.getContext().get(FilePath.class);
            Objects.requireNonNull(workspace, "Workspace not found");
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(workingDir)) {
                workingDirPath = workspace.child(workingDir);
            }
            List<NacosConfigFileDTO> list = new ArrayList<>();

            List<FilePath> nsPaths = workingDirPath.listDirectories();
            for (FilePath nsPath : nsPaths) {
                List<FilePath> groupPaths = nsPath.listDirectories();
                for (FilePath groupPath : groupPaths) {
                    List<FilePath> dataIdPaths = groupPath.list(new FileFileFilter());
                    for (FilePath dataIdPath : dataIdPaths) {
                        list.add(new NacosConfigFileDTO(nsPath.getName(), groupPath.getName(), dataIdPath.getName()));
                    }
                }
            }

            for (NacosConfigFileDTO file : list) {
                FilePath groupPath = workingDirPath.child(file.getNamespace() + "/" + file.getGroup());
                FilePath[] versionPaths = groupPath.list("*/" + file.getDataId());
                List<String> vs = Arrays.stream(versionPaths)
                        .map(e -> {
                            String path = e.getRemote();
                            String[] ss = StringUtils.split(path, File.separatorChar);
                            return ss[ss.length - 2];
                        })
                        .sorted((o1, o2) -> Utils.compareVersion(o2, o1))
                        .collect(Collectors.toList());
                file.setVersions(vs);
            }
            return list;
        }
    }

    // @Extension
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
            return "Nacos Config Files Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigGet";
        }
    }
}
