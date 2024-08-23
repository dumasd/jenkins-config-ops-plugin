package io.jenkins.plugins.configops.nacos;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.configops.model.dto.NacosConfigModifyDTO;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.Constants;
import io.jenkins.plugins.configops.utils.Logger;
import io.jenkins.plugins.configops.utils.Utils;
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
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 修改确认
 *
 * @author Bruce.Wu
 * @date 2024-08-11
 */
@Setter
@Getter
@ToString
public class NacosConfigModifyApplyStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nacosId;

    private final String toolUrl;

    private final List<NacosConfigModifyDTO> items;

    @DataBoundConstructor
    public NacosConfigModifyApplyStep(
            @NonNull String nacosId, String toolUrl, @NonNull List<NacosConfigModifyDTO> items) {
        this.nacosId = nacosId;
        this.toolUrl = StringUtils.defaultIfBlank(toolUrl, Constants.DEFAULT_TOOL_URL);
        this.items = items;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new NacosConfigModifyApplyStepExecution(context, new NacosConfigApplyData(nacosId, toolUrl, items));
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
            return "Nacos Config Modify Apply Step";
        }

        @Override
        public String getFunctionName() {
            return "nacosConfigModifyApply";
        }
    }

    public static class NacosConfigModifyApplyStepExecution extends SynchronousStepExecution<Map<String, Object>> {

        private static final long serialVersionUID = -1372992911615088880L;
        private final NacosConfigApplyData data;

        public NacosConfigModifyApplyStepExecution(@NonNull StepContext context, NacosConfigApplyData data) {
            super(context);
            this.data = data;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            TaskListener taskListener = getContext().get(TaskListener.class);
            Logger logger = new Logger("NacosConfigApply", taskListener);
            Launcher launcher = getContext().get(Launcher.class);
            List<NacosConfigModifyDTO> items = data.getItems();
            String toolUrl = data.getToolUrl();
            String nacosId = data.getNacosId();
            Utils.requireNotEmpty(items, "Config items is empty");
            for (NacosConfigModifyDTO dto : items) {
                Utils.requireNotBlank(dto.getNextContent(), "Next content is blank");
                Utils.requireNotBlank(dto.getGroup(), "Group is blank");
                Utils.requireNotBlank(dto.getDataId(), "Group is blank");
            }

            VirtualChannel channel = Utils.getChannel(launcher);

            for (NacosConfigModifyDTO item : items) {
                logger.log(
                        "Applying nacos config. toolUrl:%s, nacosId:%s, namespace:%s, group:%s, dataId:%s",
                        toolUrl, nacosId, item.getNamespace(), item.getGroup(), item.getDataId());
                channel.call(new RemoteExecutionCallable(toolUrl, nacosId, item));
            }
            return Collections.emptyMap();
        }
    }

    public static class RemoteExecutionCallable implements Callable<String, Exception> {

        private static final long serialVersionUID = 4711346178005514552L;
        private final String toolUrl;
        private final String nacosId;
        private final NacosConfigModifyDTO item;

        public RemoteExecutionCallable(String toolUrl, String nacosId, NacosConfigModifyDTO item) {
            this.toolUrl = toolUrl;
            this.nacosId = nacosId;
            this.item = item;
        }

        @Override
        public String call() throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                    .nacosId(nacosId)
                    .namespaceId(item.getNamespace())
                    .group(item.getGroup())
                    .dataId(item.getDataId())
                    .content(item.getNextContent())
                    .format(item.getFormat())
                    .build();
            return client.nacosConfigModifyApply(nacosConfigReq);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }

    @Setter
    @Getter
    @ToString
    public static class NacosConfigApplyData implements Serializable {

        private static final long serialVersionUID = -4306037005248347948L;

        private final String nacosId;

        private final String toolUrl;

        private final List<NacosConfigModifyDTO> items;

        public NacosConfigApplyData(String nacosId, String toolUrl, List<NacosConfigModifyDTO> items) {
            this.nacosId = nacosId;
            this.toolUrl = toolUrl;
            this.items = items;
        }
    }
}
