package io.jenkins.plugins.configops.database;

import com.google.common.hash.Hashing;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;
import hudson.model.Run;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.ExportedBean;

@Setter
@Getter
@ToString
@Accessors(chain = true)
@ExportedBean(defaultVisibility = 999)
public class SqlApplyDetailAction implements Serializable, Action {

    private static final long serialVersionUID = -5338355240737132819L;

    private String stepHash;
    private String url;
    private String database;
    private String sql;
    private Long rowcount = 0L;
    private List<LinkedHashMap<String, Object>> rows;

    @Restricted(NoExternalUse.class) // only used from stapler/jelly
    @CheckForNull
    public Run<?, ?> getOwningRun() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        return req.findAncestorObject(Run.class);
    }

    public List<String> getColumns() {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> headers = rows.get(0).keySet();
        return new ArrayList<>(headers);
    }

    @Override
    public String getUrlName() {
        String hashStr = String.format("%s-%s-%s-%s", stepHash, url, database, sql);
        String crc32 =
                Hashing.crc32().hashString(hashStr, StandardCharsets.UTF_8).toString();
        return "sql-apply-detail-" + crc32;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
