package io.jenkins.plugins.configops.nacos;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.configops.utils.Constants;
import java.util.Collections;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Nacos配置参数定义
 *
 * @author Bruce.Wu
 * @date 2024-08-08
 */
@Deprecated
@Log
@Setter
@Getter
public class NacosConfigParameterDefinition extends ParameterDefinition {
    /**
     * 工作目录
     */
    private String workingDir;
    /**
     * 工具地址
     */
    private String toolUrl = Constants.DEFAULT_TOOL_URL;
    /**
     * 工具访问凭证
     */
    private String credentialsId;

    private NacosNamespaceGroup metadata;

    @DataBoundConstructor
    public NacosConfigParameterDefinition(@NonNull String name, String workingDir) {
        super(name);
        this.workingDir = workingDir;
        this.metadata = new NacosNamespaceGroup(workingDir, toolUrl);
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
        this.metadata.setToolUrl(toolUrl);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        this.metadata.setCredentialsId(credentialsId);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return null;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        return null;
    }

    public Descriptor<NacosNamespaceGroup> getMetadataDescriptor() {
        return Jenkins.get().getDescriptorByType(NacosNamespaceGroup.DescriptorImpl.class);
    }

    // @Extension
    // @Symbol("nacosConfig")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(NacosConfigParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Tool";
        }

        public ListBoxModel doFillCredentialsIdItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Select a credential", "");
            for (StandardUsernameCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                    StandardUsernameCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
                items.add(c.getId(), c.getId());
            }
            return items;
        }
    }
}
