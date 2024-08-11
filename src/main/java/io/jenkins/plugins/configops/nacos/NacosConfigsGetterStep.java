package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.configops.model.dto.NacosFileDTO;
import io.jenkins.plugins.configops.utils.FileFileFilter;

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
            extends SynchronousNonBlockingStepExecution<List<NacosFileDTO>> {

        private final String workingDir;

        public NacosConfigsGetterStepExecution(@NonNull StepContext context, String workingDir) {
            super(context);
            this.workingDir = workingDir;
        }

        @Override
        protected List<NacosFileDTO> run() throws Exception {
            FilePath workspace = this.getContext().get(FilePath.class);
            if (Objects.isNull(workspace)) {
                throw new NullPointerException("Workspace not found");
            }
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(workingDir)) {
                workingDirPath = workspace.child(workingDir);
            }
            List<NacosFileDTO> list = new ArrayList<>();

            List<FilePath> nsPaths = workingDirPath.listDirectories();
            for (FilePath nsPath : nsPaths) {
                List<FilePath> groupPaths = nsPath.listDirectories();
                for (FilePath groupPath : groupPaths) {
                    List<FilePath> dataIdPaths = groupPath.list(new FileFileFilter());
                    for (FilePath dataIdPath : dataIdPaths) {
                        list.add(new NacosFileDTO(nsPath.getName(), groupPath.getName(), dataIdPath.getName()));
                    }
                }
            }

            for (NacosFileDTO file : list) {
                FilePath groupPath = workingDirPath.child(file.getNamespace() + "/" + file.getGroup());
                FilePath[] versionPaths = groupPath.list("*/" + file.getDataId());
                List<String> vs = Arrays.stream(versionPaths)
                        .map(e -> {
                            String path = e.getRemote();
                            String[] ss = StringUtils.split(path, File.separatorChar);
                            return ss[ss.length - 2];
                        })
                        .collect(Collectors.toList());
                file.setVersions(vs);
            }
            return list;
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
            return "Nacos Config Files Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigGet";
        }
    }
}
