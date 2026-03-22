package com.example.project;

import com.example.Debug;
import com.example.data.MediaAsset;
import com.example.engine.HashAlgorithm;
import com.example.engine.SimilarityEngine;
import com.example.engine.file.FileManagement;
import com.example.engine.file.FileScanner;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
public class ProjectScan {

    // 專案基本屬性
    private final String projectName;
    private final Path rootDirectory;
    private final DuplicateStrategy strategy;

    // 後端核心模組
    private final FileScanner fileScanner;
    private final ProjectDataManager dataManager;
    private final SimilarityEngine similarityEngine;
    private final FileManagement fileManager;

    // 全域共用的資料庫連線
    private final SQLiteDao dao;

    // 存放最終掃描與比對的結果
    private Map<Long, List<MediaAsset>> similarityResults;

    // 新增一個回呼通道：接收 (目前進度, 總數量)
    private BiConsumer<Integer, Integer> progressCallback;

    /**
     * 建立一個新的專案掃描任務。
     * @param projectName    專案名稱 (通常是資料夾名稱)
     * @param rootDirectory  要掃描的根目錄路徑
     * @param algorithm      選擇要使用的雜湊演算法 (aHash / pHash)
     * @param threshold      相似度容忍閥值
     * @param threadCount    要啟用的 CPU 核心數 (0 代表自動最佳化)
     * @param dao            全域共用的 SQLite 資料庫操作物件
     */
    public ProjectScan(String projectName, Path rootDirectory, HashAlgorithm algorithm,
                       int threshold, int threadCount, DuplicateStrategy strategy, SQLiteDao dao) {
        this.projectName = projectName;
        this.rootDirectory = rootDirectory;
        this.strategy = strategy; // 接收 UI 的選擇
        this.dao = dao;

        // 初始化所有核心武器
        this.fileScanner = new FileScanner();
        this.dataManager = new ProjectDataManager(dao);
        this.similarityEngine = new SimilarityEngine(algorithm, threshold, threadCount);
        this.fileManager = new FileManagement();
    }

    // 專案建構
    public void run() {
        Debug.log("========== 專案啟動:", projectName, "==========");

        // 處理專案資訊儲存
        if (strategy == DuplicateStrategy.OVERWRITE) {
            // 清空舊資產並儲存當前資產
            dao.saveProject(projectName, rootDirectory.toAbsolutePath().toString());
        } else if (strategy == DuplicateStrategy.SYNC) {
            // 確保專案存在但不刪除舊資產
            dao.saveProjectIgnore(projectName, rootDirectory.toAbsolutePath().toString());
        }

        // 找出所有支援的圖片
        List<MediaAsset> rawScannedAssets = fileScanner.scanFolder(rootDirectory);

        // 如果被使用者中途取消，就直接停住
        if (rawScannedAssets.isEmpty()) {
            Debug.log("掃描中斷或無資料。");
            return;
        }

        // 跟資料庫舊資料做聯集比對，剔除已經算過指紋的
        List<MediaAsset> mergedAssets = dataManager.syncProjectAssets(projectName, rawScannedAssets);

        // 啟動引擎：並行計算新照片指紋，建立 VP-Tree，並分組相似照片
        this.similarityResults = similarityEngine.processPipeline(mergedAssets);

        // 把剛剛引擎算出來的「全新指紋」，通通存進資料庫
        List<MediaAsset> newlyHashedAssets = new java.util.ArrayList<>();
        for (MediaAsset asset : mergedAssets) {
            // 由於 processPipeline 執行完後，所有的 asset 都會有指紋了
            // 我們可以透過檢查它是否還沒有 fileId 來判斷是不是全新資料
            if (asset.getFileId() == 0L) {
                newlyHashedAssets.add(asset);
            }
        }

        if (!newlyHashedAssets.isEmpty()) {
            Debug.log("準備將", newlyHashedAssets.size(), "筆全新指紋寫入資料庫...");
            dao.saveAssetsBatch(projectName, newlyHashedAssets);
        }

        Debug.log("========== 專案分析完成！==========");
    }

    // 停止目前的掃描任務
    public void cancel() {
        // 停止硬碟掃描
        if (fileScanner != null) {
            fileScanner.cancelScan();
        }

        // 停止多執行緒指紋運算與比對
        if (similarityEngine != null) {
            similarityEngine.cancel();
        }
    }

    // 取得最終的相似圖片分組結果
    public Map<Long, List<MediaAsset>> getSimilarityResults() {
        return similarityResults;
    }

    // 取得這個專案專屬的檔案管理員 (用來標記和刪除檔案)
    public FileManagement getFileManager() {
        return fileManager;
    }

    // 提供給 MainController 設定的方法
    public void setProgressCallback(BiConsumer<Integer, Integer> callback) {
        this.progressCallback = callback;
        if (this.similarityEngine != null) {
            this.similarityEngine.setProgressCallback(callback);
        }
    }
}
