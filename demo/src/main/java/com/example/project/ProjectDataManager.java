package com.example.project;

import com.example.Debug;
import com.example.data.MediaAsset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectDataManager {

    private final SQLiteDao dao;

    public ProjectDataManager(SQLiteDao dao) {
        this.dao = dao;
    }

    /**
     * 1. 清除資料庫中已失效（硬碟找不到）的指紋。
     * 2. 保留硬碟中未修改的檔案指紋（免重算）。
     * 3. 標記全新或被修改過的檔案，交給引擎重算。
     *
     * @param projectName      專案名稱
     * @param rawScannedAssets 這次剛從硬碟掃描出來的「最新狀態」清單
     * @return 合併後的清單 (未修改的檔案會被填上 fileId 與舊 fingerprint)
     */
    public List<MediaAsset> syncProjectAssets(String projectName, List<MediaAsset> rawScannedAssets) {
        Debug.log("🔄 開始與資料庫進行同步比對...");

        // 從 DB 撈出這個專案所有的「舊紀錄」
        List<MediaAsset> dbAssets = dao.getAssetsByProject(projectName);

        // 建立 HashMap 提升比對速度 (以檔案絕對路徑為 Key)
        Map<String, MediaAsset> diskMap = new HashMap<>();
        for (MediaAsset diskAsset : rawScannedAssets) {
            diskMap.put(diskAsset.getFilePath().toString(), diskAsset);
        }

        List<Long> idsToDelete = new ArrayList<>();
        int newOrModifiedCount = 0;

        for (MediaAsset dbAsset : dbAssets) {
            MediaAsset diskAsset = diskMap.get(dbAsset.getFilePath().toString());

            if (diskAsset == null) {
                // 狀態 A：DB 有，但硬碟沒有 -> 使用者在硬碟把它刪除了！
                idsToDelete.add(dbAsset.getFileId());
            } else {
                // 狀態 B：DB 有，硬碟也有 -> 檢查檔案是否被修改過
                boolean isModified =
                    dbAsset.getLastModified().toEpochMilli() != diskAsset.getLastModified().toEpochMilli() ||
                    dbAsset.getFileSize() != diskAsset.getFileSize();

                if (isModified) {
                    // 狀態 B-1：檔案被修改了 (可能被編輯過，圖片變了)
                    // 作法：把舊的 DB 紀錄刪除，讓 diskAsset 保持 fileId=0，待會引擎會重新算指紋
                    idsToDelete.add(dbAsset.getFileId());
                } else {
                    // 狀態 B-2：檔案完全沒動過！
                    // 作法：把 DB 裡的舊指紋與 ID 抄到 diskAsset 身上，直接省下指紋計算的時間！
                    diskAsset.setFileId(dbAsset.getFileId());
                    diskAsset.setFingerprint(dbAsset.getFingerprint());
                }
            }
        }

        // 批次清理資料庫中的無效/過期指紋
        if (!idsToDelete.isEmpty()) {
            Debug.log("🧹 偵測到", idsToDelete.size(), "個檔案已移除或修改，正在清理資料庫...");
            dao.deleteAssetsBatch(idsToDelete);
        }

        // 只要 fileId == 0 的，就是待會需要被引擎壓榨 CPU 的新檔案
        List<MediaAsset> mergedAssets = new ArrayList<>(rawScannedAssets);
        for (MediaAsset asset : mergedAssets) {
            if (asset.getFileId() == 0L) {
                newOrModifiedCount++;
            }
        }

        Debug.log("✅ 同步完成！保留舊指紋:", (mergedAssets.size() - newOrModifiedCount),
                  "筆，需全新計算:", newOrModifiedCount, "筆。");

        return mergedAssets;
    }
}