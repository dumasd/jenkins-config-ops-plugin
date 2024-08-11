package io.jenkins.plugins.configops.nacos;

import com.alibaba.fastjson2.annotation.JSONField;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

@Log
@Setter
@Getter
public class NacosConfigEditParameterDefinition extends ParameterDefinition {

    private final String namespaceGroup;

    private final String dataId;
    /**
     * 内容格式
     */
    private final String format;
    /**
     * 当前内容
     */
    private String content;
    /**
     * 修改后的值
     */
    @JSONField(name = "next_content")
    private String nextContent;

    @DataBoundConstructor
    public NacosConfigEditParameterDefinition(
            @NonNull String name, String namespaceGroup, String dataId, String format) {
        super(name);
        this.namespaceGroup = namespaceGroup;
        this.dataId = dataId;
        this.format = format;
    }

    @DataBoundSetter
    public void setContent(String content) {
        this.content = content;
    }

    @DataBoundSetter
    public void setNextContent(String nextContent) {
        this.nextContent = nextContent;
    }

    public String getNamespaceGroup() {
        return Util.fixNull(namespaceGroup);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        log.log(Level.INFO, "Create value with jo. {0}", jo);
        return new NacosConfigEditParameterValue(getName(), jo.getString("nextContent"));
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        log.log(Level.INFO, "Create value without jo.");
        try {
            JSONObject jo = req.getSubmittedForm();
            return createValue(req, jo);
        } catch (Exception e) {
            throw new RuntimeException("Create value error.", e);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class NacosConfigEditParameterValue extends ParameterValue {

        private final String nextContent;

        public NacosConfigEditParameterValue(String name, String nextContent) {
            super(name);
            this.nextContent = nextContent;
        }

        @Override
        public void buildEnvironment(Run<?, ?> build, EnvVars env) {
            env.put(name, nextContent);
        }

        @Override
        public Object getValue() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("nextContent", nextContent);
            return map;
        }
    }

    @Extension
    @Symbol("nacosConfigEdit")
    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(NacosConfigEditParameterDefinition.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nacos Config Edit Parameter";
        }
    }
}
