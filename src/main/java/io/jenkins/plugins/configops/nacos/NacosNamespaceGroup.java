package io.jenkins.plugins.configops.nacos;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.configops.model.dto.NacosServerDTO;
import io.jenkins.plugins.configops.model.req.NacosConfigReq;
import io.jenkins.plugins.configops.model.resp.NacosConfigModifyPreviewResp;
import io.jenkins.plugins.configops.utils.ConfigOpsClient;
import io.jenkins.plugins.configops.utils.FileFileFilter;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

@Setter
@Getter
@ToString
@NoArgsConstructor
@Deprecated
public class NacosNamespaceGroup extends AbstractDescribableImpl<NacosNamespaceGroup> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String workingDir;

    /**
     * 工具地址
     */
    private String toolUrl;
    /**
     * 工具访问凭证
     */
    private String credentialsId;

    @DataBoundConstructor
    public NacosNamespaceGroup(String workingDir, String toolUrl) {
        this.workingDir = workingDir;
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NacosNamespaceGroup> {
        public DescriptorImpl() {
            super(NacosNamespaceGroup.class);
        }

        @POST
        public ListBoxModel doFillNamespaceGroupItems(
                @QueryParameter(value = "workingDir", required = true) String workingDir) {
            try {
                ListBoxModel result = new ListBoxModel();
                File workingDirFile = new File(workingDir);
                if (!workingDirFile.exists()) {
                    return result;
                }
                FilePath workingDirPath = new FilePath(workingDirFile);
                List<FilePath> nsPaths = workingDirPath.listDirectories();
                for (FilePath nsPath : nsPaths) {
                    List<FilePath> groupPaths = nsPath.listDirectories();
                    for (FilePath groupPath : groupPaths) {
                        String option = nsPath.getName() + "/" + groupPath.getName();
                        result.add(option);
                    }
                }
                if (!result.isEmpty()) {
                    result.get(0).selected = true;
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ListBoxModel doFillDataIdItems(
                @QueryParameter(value = "workingDir", required = true) String workingDir,
                @QueryParameter(value = "namespaceGroup", required = true) String namespaceGroup)
                throws Exception {
            ListBoxModel list = new ListBoxModel();
            File f = new File(workingDir + "/" + namespaceGroup);
            if (!f.exists()) {
                return list;
            }
            FilePath fp = new FilePath(f);
            List<FilePath> dataIdPaths = fp.list(new FileFileFilter());
            for (FilePath e : dataIdPaths) {
                list.add(e.getName(), e.getName());
            }
            if (!list.isEmpty()) {
                list.get(0).selected = true;
            }
            return list;
        }

        public ListBoxModel doFillVersionItems(
                @QueryParameter(value = "workingDir", required = true) String workingDir,
                @QueryParameter(value = "namespaceGroup", required = true) String namespaceGroup,
                @QueryParameter(value = "dataId", required = true) String dataId)
                throws Exception {
            ListBoxModel list = new ListBoxModel();
            File ngDirFile = new File(workingDir + "/" + namespaceGroup);
            if (!ngDirFile.exists()) {
                return list;
            }
            FilePath ngPath = new FilePath(ngDirFile);
            List<FilePath> versionPaths = ngPath.listDirectories();
            for (FilePath e : versionPaths) {
                List<FilePath> dataIdPaths = e.list(new FileFileFilter());
                for (FilePath dataIdPath : dataIdPaths) {
                    if (Objects.equals(dataIdPath.getName(), dataId)) {
                        list.add(e.getName(), e.getName());
                        break;
                    }
                }
            }
            list.sort((o1, o2) -> o2.value.compareTo(o1.value));
            if (!list.isEmpty()) {
                list.get(0).selected = true;
            }
            return list;
        }

        public ListBoxModel doFillNacosServerItems(
                @QueryParameter("toolUrl") String toolUrl, @QueryParameter("credentialsId") String credentialsId) {
            ListBoxModel list = new ListBoxModel();
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);
            List<NacosServerDTO> nacosServers = client.getNacosServers();
            nacosServers.forEach(e -> list.add(e.getNacosId() + "/" + e.getUrl(), e.getNacosId()));
            if (!list.isEmpty()) {
                list.get(0).selected = true;
            }
            return list;
        }

        @JavaScriptMethod(name = "configModifyPreview")
        public NacosConfigModifyPreviewResp doConfigModifyPreview(
                @QueryParameter(value = "workingDir", required = true) String workingDir,
                @QueryParameter("toolUrl") String toolUrl,
                @QueryParameter("nacosServer") String nacosServer,
                @QueryParameter("namespaceGroup") String namespaceGroup,
                @QueryParameter("dataId") String dataId,
                @QueryParameter("version") String version)
                throws Exception {
            ConfigOpsClient client = new ConfigOpsClient(toolUrl);

            File fullDataIdFile = new File(String.format("%s/%s/%s", workingDir, namespaceGroup, dataId));
            String fullCnt = FileUtils.readFileToString(fullDataIdFile, StandardCharsets.UTF_8);
            String patchCnt = null;
            if (StringUtils.isNotBlank(version)) {
                File patchDataIdFile =
                        new File(String.format("%s/%s/%s/%s", workingDir, namespaceGroup, version, dataId));
                patchCnt = FileUtils.readFileToString(patchDataIdFile, StandardCharsets.UTF_8);
            }
            String[] ng = namespaceGroup.split("/");
            NacosConfigReq nacosConfigReq = NacosConfigReq.builder()
                    .nacosId(nacosServer)
                    .namespaceId(ng[0])
                    .group(ng[1])
                    .dataId(dataId)
                    .fullContent(fullCnt)
                    .patchContent(patchCnt)
                    .build();
            return client.nacosConfigModifyPreview(nacosConfigReq);
        }
    }
}
