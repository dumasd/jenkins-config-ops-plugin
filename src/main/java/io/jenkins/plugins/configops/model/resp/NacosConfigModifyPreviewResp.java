package io.jenkins.plugins.configops.model.resp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Flavor;

@Setter
@ToString
@Getter
public class NacosConfigModifyPreviewResp implements Serializable, HttpResponse {
    private static final long serialVersionUID = 1L;
    /**
     * 内容格式
     */
    private String format;
    /**
     * 当前内容
     */
    private String content;

    /**
     * 修改后的值
     */
    @JSONField(name = "next_content")
    private String nextContent;

    /**
     *
     */
    @JSONField(name = "full_content")
    private String fullContent;

    @JSONField(name = "patch_content")
    private String patchContent;

    @Override
    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
            throws IOException, ServletException {
        byte[] bytes = JSON.toJSONBytes(this);
        rsp.setContentType(Flavor.JSON.contentType);
        try (OutputStream os = rsp.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("format", format);
        map.put("content", content);
        map.put("nextContent", nextContent);
        map.put("fullContent", fullContent);
        map.put("patchContent", patchContent);
        return map;
    }
}
