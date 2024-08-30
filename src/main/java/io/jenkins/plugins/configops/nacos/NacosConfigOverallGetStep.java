package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.dto.NacosConfigDTO;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.FileFileFilter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.MasterToSlaveFileCallable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

@Deprecated
@Setter
@Getter
@ToString
public class NacosConfigOverallGetStep extends Step implements Serializable {
    private static final long serialVersionUID = 5373505661508888051L;

    private final String nacosId;

    private final String toolUrl;

    private final String workingDir;

    @DataBoundConstructor
    public NacosConfigOverallGetStep(String nacosId, String toolUrl, String workingDir) {
        this.nacosId = nacosId;
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.workingDir = workingDir;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, this);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<List<NacosConfigDTO>> {

        private static final long serialVersionUID = -7586842755857966658L;

        private final NacosConfigOverallGetStep step;

        public StepExecutionImpl(@NonNull StepContext context, NacosConfigOverallGetStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<NacosConfigDTO> run() throws Exception {
            FilePath workspace = getContext().get(FilePath.class);
            if (Objects.isNull(workspace)) {
                throw new IllegalArgumentException("Step workspace is null");
            }
            FilePath workingDirPath = workspace;
            if (StringUtils.isNotBlank(step.getWorkingDir())) {
                workingDirPath = workspace.child(step.getWorkingDir());
            }
            List<NacosConfigDTO> result = workingDirPath.act(new RemoteCallable(step.getToolUrl(), step.getNacosId()));
            return result;
        }
    }

    private static class RemoteCallable extends MasterToSlaveFileCallable<List<NacosConfigDTO>> {

        private static final long serialVersionUID = -5550176413772105947L;
        private final String toolUrl;
        private final String nacosId;

        private RemoteCallable(String toolUrl, String nacosId) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
        }

        @Override
        public List<NacosConfigDTO> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath workingDirPath = new FilePath(f);
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            List<FilePath> nsPaths = workingDirPath.listDirectories();
            List<NacosConfigDTO> result = new ArrayList<>();
            for (FilePath nsPath : nsPaths) {
                List<FilePath> groupPaths = nsPath.listDirectories();
                String namespace = nsPath.getName();
                for (FilePath groupPath : groupPaths) {
                    String group = groupPath.getName();
                    Map<String, FilePath> patchDataIdPathMap = groupPath.list(new FileFileFilter()).stream()
                            .collect(Collectors.toMap(FilePath::getName, e -> e));
                    Map<String, FilePath> deleteDataIdPathMap;
                    FilePath deleteDataIdDirPath = groupPath.child("delete");
                    if (deleteDataIdDirPath.exists() && deleteDataIdDirPath.isDirectory()) {
                        deleteDataIdPathMap = deleteDataIdDirPath.list(new FileFileFilter()).stream()
                                .collect(Collectors.toMap(FilePath::getName, e -> e));
                    } else {
                        deleteDataIdPathMap = new HashMap<>();
                    }
                    for (Map.Entry<String, FilePath> patchEntry : patchDataIdPathMap.entrySet()) {
                        FilePath deleteFile = deleteDataIdPathMap.remove(patchEntry.getKey());
                        getNacosConfig(client, namespace, group, patchEntry.getKey(), patchEntry.getValue(), deleteFile)
                                .ifPresent(result::add);
                    }

                    for (Map.Entry<String, FilePath> deleteEntry : deleteDataIdPathMap.entrySet()) {
                        getNacosConfig(client, namespace, group, deleteEntry.getKey(), null, deleteEntry.getValue())
                                .ifPresent(result::add);
                    }
                }
            }
            return result;
        }

        private Optional<NacosConfigDTO> getNacosConfig(
                ConfigOpsClient client,
                String namespace,
                String group,
                String dataId,
                FilePath patchFile,
                FilePath deleteFile)
                throws IOException {
            String patchContent = "";
            if (Objects.nonNull(patchFile)) {
                patchContent = FileUtils.readFileToString(new File(patchFile.getRemote()), StandardCharsets.UTF_8);
            }
            String deleteContent = "";
            if (Objects.nonNull(deleteFile)) {
                deleteContent = FileUtils.readFileToString(new File(deleteFile.getRemote()), StandardCharsets.UTF_8);
            }
            NacosConfigDTO nacosConfigDTO = client.getNacosConfig(nacosId, namespace, group, dataId);
            if (StringUtils.isBlank(nacosConfigDTO.getId())) {
                // 配置不存在，随便搞个ID
                nacosConfigDTO.setId(RandomStringUtils.randomAlphanumeric(20));
                if (StringUtils.isBlank(patchContent)) {
                    return Optional.empty();
                }
            }
            nacosConfigDTO.setPatchContent(patchContent);
            nacosConfigDTO.setDeleteContent(deleteContent);
            return Optional.of(nacosConfigDTO);
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
            return "Nacos Config Overall Getter";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigOverallGet";
        }
    }
}
