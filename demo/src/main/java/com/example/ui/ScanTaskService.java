package com.example.ui;

import com.example.project.ProjectScan;
import javafx.concurrent.Task;
import com.example.Debug;

/**
 * 掃描任務服務員：專門負責背景任務 (Task) 的建立與生命週期管理。
 * 透過將複雜的 Thread 與 Progress 邏輯移出 Controller，實現職責分離。
 */
public class ScanTaskService {

    /**
     * 建立一個專屬於專案掃描的背景任務
     * * @param project 已經初始化完成的專案指揮官 (ProjectScan)
     * @return 一個已經綁定好進度回報機制的 Task
     */
    public Task<Void> createScanTask(ProjectScan project) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // 設定初始狀態訊息
                    updateMessage("正在初始化掃描任務...");

                    // 串接進度回報管道
                    // 將 SimilarityEngine 的進度通知，轉發給 Task 的 updateProgress
                    project.setProgressCallback((current, total) -> {
                        // updateProgress 是執行緒安全的，會由 JavaFX 自動處理 UI 更新
                        updateProgress(current, total);

                        // 順便更新狀態文字 (例如：正在分析 500 / 10000)
                        updateMessage(String.format("正在分析圖片: %d / %d", current, total));
                    });

                    // 執行最耗時的專案運作 (包含掃描、計算指紋、建立 VP-Tree)
                    project.run();

                    // 任務即將結束
                    updateMessage("分析完成，正在整理結果...");
                    return null;

                } catch (Exception e) {
                    // 在 Task 內部記錄錯誤，這會觸發 setOnFailed
                    Debug.error("背景任務執行失敗: " + e.getMessage());
                    throw e;
                }
            }
        };
    }
}
