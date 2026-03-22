package com.example.engine.file;

import com.example.data.MediaAsset;
import com.example.Debug;

import java.awt.Desktop;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileManagement {
    private final Set<MediaAsset> deleteList = new HashSet<>();

    // 將檔案標記為待刪除
    public void markForDeletion(MediaAsset asset) {
        if (asset != null) {
            deleteList.add(asset);
        }
    }

    // 取消標記
    public void unmarkForDeletion(MediaAsset asset) {
        if (asset != null) {
            deleteList.remove(asset);
        }
    }

    // 清空待刪除清單
    public void clearList() {
        deleteList.clear();
    }

    // 取得目前有多少檔案在待刪除清單中
    public int getPendingDeleteCount() {
        return deleteList.size();
    }

    public boolean isMarked(MediaAsset asset) {
        return deleteList.contains(asset);
    }

    // 傳入一個相似群組，自動保留檔案最大的一張，其餘加入刪除清單
    public void autoMarkDuplicatesToKeepBest(List<MediaAsset> group) {
        if (group == null || group.size() <= 1) return;

        List<MediaAsset> sortedGroup = new ArrayList<>(group);
        sortedGroup.sort((a, b) -> Long.compare(b.getFileSize(), a.getFileSize()));

        for (int i = 1; i < sortedGroup.size(); i++) {
            markForDeletion(sortedGroup.get(i));
        }
    }

    // 執行批次刪除 (提供兩種模式：永久刪除 或 移至資源回收桶)
    public void executeBatchDelete(boolean moveToTrash) {
        if (deleteList.isEmpty()) return;

        Debug.log("準備刪除", deleteList.size(), "個檔案。模式:", (moveToTrash ? "資源回收桶" : "永久刪除"));

        int successCount = 0;
        int failCount = 0;
        int otherCount = 0;

        // 檢查系統是否支援移至資源回收桶
        boolean isTrashSupported = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH);

        if (moveToTrash && !isTrashSupported) {
            Debug.log("作業系統不支援移至資源回收桶，將改為永久刪除！");
            moveToTrash = false;
        }

        for (MediaAsset asset : deleteList) {
            if (!Files.exists(asset.getFilePath())) {
                otherCount++;
                continue;
            }
            try {
                boolean deleted = false;
                if (moveToTrash) {
                    // 將 Path 轉為 File 並移至回收桶
                    deleted = Desktop.getDesktop().moveToTrash(asset.getFilePath().toFile());
                } else {
                    // NIO 永久刪除
                    deleted = Files.deleteIfExists(asset.getFilePath());
                }

                if (deleted) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                Debug.error("刪除失敗: " + asset.getFilePath() + " (" + e.getMessage() + ")");
                failCount++;
            }
        }

        Debug.log("批次清理完成！成功:", successCount, ",失敗:", failCount, ",其他:", otherCount);

        // 執行完畢後清空清單
        deleteList.clear();
    }
}