package io.jenkins.plugins.configops.model.resp;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.configops.model.dto.NacosConfigFileDTO;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Flavor;

/**
 * @author Bruce.Wu
 * @date 2024-08-19
 */
@Setter
@Getter
@ToString
public class ListNacosConfigFileResp implements Serializable, HttpResponse {

    private List<NacosConfigFileDTO> values;

    public ListNacosConfigFileResp(List<NacosConfigFileDTO> values) {
        this.values = values;
    }

    public ListNacosConfigFileResp() {
        this(new ArrayList<>());
    }

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
}
