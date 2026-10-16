package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Follow-up to V3: translates the last English fragments found in existing rows (raw replenishment
 * strategy codes and English units inside document texts, an English verb in old audit entries).
 * UPDATE-only and idempotent; no data is removed.
 */
public class V4__kazakh_localization_fixes extends BaseJavaMigration {

    private static final String[][] DOCUMENT_TEXT = {
            {" box (стратегия DEMAND_BASED)", " қорап (стратегия: сұранысқа негізделген)"},
            {"(стратегия DEMAND_BASED)", "(стратегия: сұранысқа негізделген)"},
            {"(стратегия TOP_UP_TO_TARGET)", "(стратегия: ең аз қордың 3 есесіне дейін толықтыру)"},
            {"(стратегия FIXED_LOT)", "(стратегия: 50 бірліктік тұрақты партиялар)"},
    };

    private static final String[][] AUDIT_TEXT = {
            {" approved", " бекітілді"}, {" rejected", " қабылданбады"}, {" returned for revision", " пысықтауға қайтарылды"},
            {" at ", ", қалдығы "},
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        for (String column : new String[]{"documents.content", "document_revisions.content"}) {
            replace(c, column, DOCUMENT_TEXT);
        }
        replace(c, "audit_logs.details", AUDIT_TEXT);
    }

    private static void replace(Connection c, String target, String[][] pairs) throws SQLException {
        String[] tc = target.split("\\.");
        for (String[] p : pairs) {
            try (PreparedStatement ps = c.prepareStatement("update " + tc[0] + " set " + tc[1] + " = replace(" + tc[1]
                    + ", ?, ?) where locate(?, " + tc[1] + ") > 0")) {
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[0]);
                ps.executeUpdate();
            }
        }
    }
}
