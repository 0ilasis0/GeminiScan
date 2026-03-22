package com.example.project;

import com.example.Debug;
import com.example.data.MediaAsset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.sql.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SQLiteDao {
    private final String dbUrl;

    public SQLiteDao(Path dbFilePath) {
        ensureDirectories(dbFilePath);
        String absolutePath = dbFilePath.toAbsolutePath().toString();
        this.dbUrl = "jdbc:sqlite:" + absolutePath;
        Debug.log("資料庫連線位址:", this.dbUrl);
        initTables();
    }

    private void ensureDirectories(Path dbFilePath) {
        try {
            Path parentDir = dbFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                Debug.log("已建立資料庫目錄結構:", parentDir.toString());
            }
        } catch (IOException e) {
            Debug.error("無法建立資料庫目錄:", e.getMessage());
        }
    }

    private void initTables() {
        String createProjectsTable = "CREATE TABLE IF NOT EXISTS projects (" +
                "project_name TEXT PRIMARY KEY, " +
                "root_directory TEXT NOT NULL" +
                ");";

        String createAssetsTable = "CREATE TABLE IF NOT EXISTS assets (" +
                "file_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_name TEXT NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "file_size INTEGER, " +
                "fingerprint INTEGER, " +
                "last_modified INTEGER, " +
                "FOREIGN KEY(project_name) REFERENCES projects(project_name) ON DELETE CASCADE" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createProjectsTable);
            stmt.execute(createAssetsTable);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_project ON assets(project_name);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_filepath ON assets(file_path);");

            Debug.log("資料庫初始化成功！");

        } catch (SQLException e) {
            Debug.error("初始化資料庫失敗:", e.getMessage());
        }
    }

    // ==========================================
    // 專案管理 (Project CRUD)
    // ==========================================

    public void saveProject(String projectName, String rootDirectory) {
        String sql = "INSERT OR REPLACE INTO projects (project_name, root_directory) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectName);
            pstmt.setString(2, rootDirectory);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Debug.error("儲存專案失敗:", e.getMessage());
        }
    }

    public void saveProjectIgnore(String projectName, String rootDirectory) {
        String sql = "INSERT OR IGNORE INTO projects (project_name, root_directory) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectName);
            pstmt.setString(2, rootDirectory);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Debug.error("儲存專案(Ignore)失敗:", e.getMessage());
        }
    }

    public boolean existsProject(String projectName) {
        String sql = "SELECT 1 FROM projects WHERE project_name = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 取得所有專案清單
     * @return Map<專案名稱, 根目錄路徑>
     */
    public Map<String, String> getAllProjects() {
        Map<String, String> projects = new HashMap<>();
        String sql = "SELECT project_name, root_directory FROM projects";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                projects.put(rs.getString("project_name"), rs.getString("root_directory"));
            }
        } catch (SQLException e) {
            Debug.error("讀取專案清單失敗:", e.getMessage());
        }
        return projects;
    }

    /**
     * 刪除整個專案
     */
    public void deleteProject(String projectName) {
        String sql = "DELETE FROM projects WHERE project_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectName);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                Debug.log("已成功刪除專案及所有關聯圖片紀錄:", projectName);
            }
        } catch (SQLException e) {
            Debug.error("刪除專案失敗:", e.getMessage());
        }
    }

    // ==========================================
    // 圖片資產管理 (Asset CRUD)
    // ==========================================

    public void saveAssetsBatch(String projectName, List<MediaAsset> newAssets) {
        if (newAssets == null || newAssets.isEmpty()) return;

        String sql = "INSERT OR IGNORE INTO assets (project_name, file_path, file_size, fingerprint, last_modified) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < newAssets.size(); i++) {
                    MediaAsset asset = newAssets.get(i);
                    pstmt.setString(1, projectName);
                    pstmt.setString(2, asset.getFilePath().toString());
                    pstmt.setLong(3, asset.getFileSize());
                    pstmt.setLong(4, asset.getFingerprint());
                    pstmt.setLong(5, asset.getLastModified().toEpochMilli());
                    pstmt.addBatch();

                    if (i > 0 && i % Config.DB_BATCH_SIZE == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }
            conn.commit();
            Debug.log("成功批次儲存", newAssets.size(), "筆資料至資料庫。");
        } catch (SQLException e) {
            Debug.error("批次儲存資料失敗:", e.getMessage());
        }
    }

    public List<MediaAsset> getAssetsByProject(String projectName) {
        List<MediaAsset> assets = new ArrayList<>();
        String sql = "SELECT * FROM assets WHERE project_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Path path = Paths.get(rs.getString("file_path"));
                    java.time.Instant lastMod = java.time.Instant.ofEpochMilli(rs.getLong("last_modified"));

                    MediaAsset asset = new MediaAsset(path, path.getFileName().toString(), rs.getLong("file_size"), lastMod);
                    asset.setFileId(rs.getLong("file_id"));
                    asset.setFingerprint(rs.getLong("fingerprint"));
                    assets.add(asset);
                }
            }
        } catch (SQLException e) {
            Debug.error("讀取專案資料失敗:", e.getMessage());
        }
        return assets;
    }

    public void deleteAssetsBatch(List<Long> fileIdsToDelete) {
        if (fileIdsToDelete == null || fileIdsToDelete.isEmpty()) return;

        String sql = "DELETE FROM assets WHERE file_id = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < fileIdsToDelete.size(); i++) {
                    pstmt.setLong(1, fileIdsToDelete.get(i));
                    pstmt.addBatch();
                    if (i > 0 && i % Config.DB_BATCH_SIZE == 0) pstmt.executeBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
            Debug.log("已從資料庫移除", fileIdsToDelete.size(), "筆失效紀錄。");
        } catch (SQLException e) {
            Debug.error("刪除資料庫紀錄失敗:", e.getMessage());
        }
    }

    // ==========================================
    // 系統底層
    // ==========================================

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        // 🔥 這行非常重要！SQLite 預設不開啟外鍵檢查，必須手動開啟，否則 ON DELETE CASCADE 不會生效
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
