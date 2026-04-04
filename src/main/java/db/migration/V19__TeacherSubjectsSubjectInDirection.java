package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Приводит {@code teacher_subjects} к модели «преподаватель ↔ предмет в направлении».
 * <p>
 * Идемпотентно обрабатывает:
 * <ul>
 *   <li>legacy: колонка {@code subject_id} (как в V11);</li>
 *   <li>уже новая схема: только {@code subject_direction_id} (например, после bulk_demo_seed без {@code subject_id}).</li>
 * </ul>
 * Старый SQL с {@code AFTER subject_id} ломался, если колонки {@code subject_id} в таблице не было.
 */
public class V19__TeacherSubjectsSubjectInDirection extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        try (Statement st = c.createStatement()) {
            boolean hasSubjectId = columnExists(c, "teacher_subjects", "subject_id");
            boolean hasSubjectDirection = columnExists(c, "teacher_subjects", "subject_direction_id");

            if (!hasSubjectDirection) {
                st.execute("ALTER TABLE teacher_subjects ADD COLUMN subject_direction_id BIGINT NULL");
            }

            if (hasSubjectId) {
                st.executeUpdate("""
                        UPDATE teacher_subjects ts
                        SET subject_direction_id = (
                            SELECT MIN(sid.id)
                            FROM subjects_in_directions sid
                            WHERE sid.subject_id = ts.subject_id
                        )
                        WHERE ts.subject_direction_id IS NULL
                        """);

                st.executeUpdate("DELETE FROM teacher_subjects WHERE subject_direction_id IS NULL");

                st.executeUpdate("""
                        DELETE t1 FROM teacher_subjects t1
                        INNER JOIN teacher_subjects t2
                            ON t1.teacher_id = t2.teacher_id
                            AND t1.subject_direction_id = t2.subject_direction_id
                            AND t1.id > t2.id
                        """);

                String fkSubject = foreignKeyNameOnColumn(c, "teacher_subjects", "subject_id");
                if (fkSubject != null) {
                    st.execute("ALTER TABLE teacher_subjects DROP FOREIGN KEY `" + escapeIdent(fkSubject) + "`");
                }
                st.execute("ALTER TABLE teacher_subjects DROP COLUMN subject_id");
            }

            st.executeUpdate("DELETE FROM teacher_subjects WHERE subject_direction_id IS NULL");

            st.executeUpdate("""
                    DELETE t1 FROM teacher_subjects t1
                    INNER JOIN teacher_subjects t2
                        ON t1.teacher_id = t2.teacher_id
                        AND t1.subject_direction_id = t2.subject_direction_id
                        AND t1.id > t2.id
                    """);

            st.execute("ALTER TABLE teacher_subjects MODIFY COLUMN subject_direction_id BIGINT NOT NULL");

            // fk_ts_teacher (teacher_id) часто «сидит» на префиксе UNIQUE(teacher_id, subject_id).
            // Без отдельного индекса по teacher_id MySQL не даёт снять uk_ts_teacher_subject (errno 1553).
            dropUkTeacherSubjectIfExists(st, c);

            String fkOldName = "fk_ts_subject";
            if (foreignKeyByNameExists(c, "teacher_subjects", fkOldName)) {
                st.execute("ALTER TABLE teacher_subjects DROP FOREIGN KEY `" + fkOldName + "`");
            }

            if (!foreignKeySubjectDirectionExists(c, "teacher_subjects")) {
                st.execute("""
                        ALTER TABLE teacher_subjects
                        ADD CONSTRAINT fk_ts_subject_direction
                        FOREIGN KEY (subject_direction_id) REFERENCES subjects_in_directions (id)
                        """);
            }

            if (!uniqueIndexExists(c, "teacher_subjects", "uk_teacher_subject_direction")) {
                st.execute("""
                        ALTER TABLE teacher_subjects
                        ADD UNIQUE KEY uk_teacher_subject_direction (teacher_id, subject_direction_id)
                        """);
            }

            dropV19TeacherIdSupportIndexIfRedundant(st, c);
        }
    }

    private static final String V19_TEACHER_ID_SUPPORT_IDX = "idx_v19_teacher_subjects_teacher_id";

    /**
     * Снимает старый UNIQUE(teacher_id, subject_id), не ломая FK на teacher_id.
     */
    private static void dropUkTeacherSubjectIfExists(Statement st, Connection c) throws SQLException {
        if (!indexExists(c, "teacher_subjects", "uk_ts_teacher_subject")) {
            return;
        }
        if (!indexExists(c, "teacher_subjects", V19_TEACHER_ID_SUPPORT_IDX)) {
            st.execute("ALTER TABLE teacher_subjects ADD INDEX `" + V19_TEACHER_ID_SUPPORT_IDX + "` (teacher_id)");
        }
        st.execute("ALTER TABLE teacher_subjects DROP INDEX `uk_ts_teacher_subject`");
    }

    /** Временный индекс не нужен, если уже есть уникальный (teacher_id, subject_direction_id). */
    private static void dropV19TeacherIdSupportIndexIfRedundant(Statement st, Connection c) throws SQLException {
        if (!indexExists(c, "teacher_subjects", V19_TEACHER_ID_SUPPORT_IDX)) {
            return;
        }
        if (uniqueIndexExists(c, "teacher_subjects", "uk_teacher_subject_direction")) {
            st.execute("ALTER TABLE teacher_subjects DROP INDEX `" + V19_TEACHER_ID_SUPPORT_IDX + "`");
        }
    }

    private static String escapeIdent(String s) {
        return s.replace("`", "``");
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String foreignKeyNameOnColumn(Connection c, String table, String column) throws SQLException {
        String sql = """
                SELECT kcu.CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE kcu
                INNER JOIN information_schema.TABLE_CONSTRAINTS tc
                    ON tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
                    AND tc.TABLE_NAME = kcu.TABLE_NAME
                    AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE kcu.TABLE_SCHEMA = DATABASE()
                  AND kcu.TABLE_NAME = ?
                  AND kcu.COLUMN_NAME = ?
                  AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
                LIMIT 1
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static boolean foreignKeyByNameExists(Connection c, String table, String constraintName)
            throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, constraintName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean foreignKeySubjectDirectionExists(Connection c, String table) throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = 'subject_direction_id'
                  AND REFERENCED_TABLE_NAME = 'subjects_in_directions'
                LIMIT 1
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean uniqueIndexExists(Connection c, String table, String indexName) throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                  AND NON_UNIQUE = 0
                LIMIT 1
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean indexExists(Connection c, String table, String indexName) throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                LIMIT 1
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
