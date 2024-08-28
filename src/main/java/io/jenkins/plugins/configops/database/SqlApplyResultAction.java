package io.jenkins.plugins.configops.database;

import com.google.common.hash.Hashing;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;
import hudson.model.Run;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.ExportedBean;

@Getter
@ToString
@ExportedBean(defaultVisibility = 999)
public class SqlApplyResultAction implements Action, Serializable {
    private static final long serialVersionUID = -8007397076504939791L;

    private final List<SqlDetail> data;

    public SqlApplyResultAction() {
        this(new ArrayList<>());
    }

    public SqlApplyResultAction(List<SqlApplyDetailAction> data) {
        this.data = data.stream()
                .map(d -> {
                    SqlDetail detail = new SqlDetail();
                    detail.setStepHash(d.getStepHash());
                    detail.setUrl(d.getUrl());
                    detail.setDatabase(d.getDatabase());
                    detail.setSql(d.getSql());
                    detail.setRowcount(d.getRowcount());
                    return detail;
                })
                .collect(Collectors.toList());
    }

    public void addDetails(List<SqlApplyDetailAction> data) {
        data.stream()
                .map(d -> {
                    SqlDetail detail = new SqlDetail();
                    detail.setStepHash(d.getStepHash());
                    detail.setUrl(d.getUrl());
                    detail.setDatabase(d.getDatabase());
                    detail.setSql(d.getSql());
                    detail.setRowcount(d.getRowcount());
                    return detail;
                })
                .forEach(this.data::add);
    }

    public void addDetail(SqlApplyDetailAction d) {
        SqlDetail detail = new SqlDetail();
        detail.setStepHash(d.getStepHash());
        detail.setUrl(d.getUrl());
        detail.setDatabase(d.getDatabase());
        detail.setSql(d.getSql());
        detail.setRowcount(d.getRowcount());
        this.data.add(detail);
    }

    @Restricted(NoExternalUse.class) // only used from stapler/jelly
    @CheckForNull
    public Run<?, ?> getOwningRun() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        return req.findAncestorObject(Run.class);
    }

    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-config-ops-plugin/images/sql-file.svg";
    }

    @Override
    public String getDisplayName() {
        return "SQL Apply Result";
    }

    @Override
    public String getUrlName() {
        return "sql-apply";
    }

    @Setter
    @Getter
    @ToString
    public static class SqlDetail implements Serializable {
        private static final long serialVersionUID = -2119842128844551071L;
        private String stepHash;
        private String url;
        private String database;
        private String sql;
        private Long rowcount = 0L;

        public String getUrlName() {
            String hashStr = String.format("%s-%s-%s-%s", stepHash, url, database, sql);
            String crc32 =
                    Hashing.crc32().hashString(hashStr, StandardCharsets.UTF_8).toString();
            return "sql-apply-detail-" + crc32;
        }
    }
}
