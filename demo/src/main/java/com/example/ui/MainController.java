package com.example.ui;

import com.example.Debug;
import com.example.ConfigGlobal;
import com.example.data.SimilarityGroup;
import com.example.engine.HashAlgorithm;
import com.example.engine.file.FileManagement;
import com.example.project.DuplicateStrategy;
import com.example.project.ProjectScan;
import com.example.project.SQLiteDao;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.nio.file.Path;

public class MainController {

    // ===== FXML 元件注入 (與 SceneBuilder 綁定) =====
    @FXML private Button btnSelectFolder, btnStart, btnCancel, btnSmartSelect, btnDeleteSelected;
    @FXML private Label lblProjectPath, lblStatus, lblResultSummary;
    @FXML private ComboBox<HashAlgorithm> comboAlgorithm;
    @FXML private ComboBox<DuplicateStrategy> comboStrategy;
    @FXML private Spinner<Integer> spinnerThreshold, spinnerThreads;
    @FXML private ProgressBar progressBar;
    @FXML private ListView<SimilarityGroup> resultListView;
    @FXML private TextArea txtConsole;

    // ===== 專屬管家實體 (Services) =====
    private final Settings settingsManager = new Settings();
    private final ScanTaskService taskService = new ScanTaskService();

    // ===== 狀態變數 =====
    private Path selectedDirectory;
    private ProjectScan currentProject;
    private SQLiteDao globalDao;

    @FXML
    public void initialize() {
        // 初始化系統底層 (Log, DB)
        initSystemBase();

        // 委託 SettingsManager 處理 UI 初始值與自動存檔
        settingsManager.loadPreferences(comboAlgorithm, comboStrategy, spinnerThreshold, spinnerThreads);
        settingsManager.bindAutoSave(comboAlgorithm, comboStrategy, spinnerThreshold, spinnerThreads);

        // 綁定按鈕事件
        bindButtonActions();
    }

    private void initSystemBase() {
        Debug.setUiListener(msg -> javafx.application.Platform.runLater(() -> {
            if (txtConsole != null) {
                txtConsole.appendText(msg + "\n");
                txtConsole.setScrollTop(Double.MAX_VALUE);
                if (txtConsole.lengthProperty().get() > Config.MAX_UI_LOG_LEN) {
                    txtConsole.deleteText(0, Config.DELETE_UI_LOG_LEN);
                }
            }
        }));
        globalDao = new SQLiteDao(ConfigGlobal.getDataSubPath(ConfigGlobal.DB_BASE));
    }

    private void bindButtonActions() {
        btnSelectFolder.setOnAction(e -> handleSelectFolder());
        btnStart.setOnAction(e -> handleStartScan());
        btnCancel.setOnAction(e -> handleCancelScan());
        btnSmartSelect.setOnAction(e -> handleSmartSelect());
        btnDeleteSelected.setOnAction(e -> handleDeleteSelected());
    }

    // 核心業務邏輯：開始掃描
    private void handleStartScan() {
        if (selectedDirectory == null) {
            Debug.warn("請先選擇要掃描的資料夾！");
            return;
        }

        // 1. 初始化專案指揮官
        currentProject = new ProjectScan(
            selectedDirectory.getFileName().toString(),
            selectedDirectory,
            comboAlgorithm.getValue(),
            spinnerThreshold.getValue(),
            spinnerThreads.getValue(),
            comboStrategy.getValue(),
            globalDao
        );

        // 2. 準備背景任務 (透過 Service)
        Task<Void> scanTask = taskService.createScanTask(currentProject);

        // 3. 綁定 UI 狀態
        setUiLocked(true);
        progressBar.progressProperty().bind(scanTask.progressProperty());
        lblStatus.textProperty().bind(scanTask.messageProperty());

        // 4. 任務成功處理
        scanTask.setOnSucceeded(e -> {
            unbindTaskUI();
            setUiLocked(false);
            lblStatus.setText("🎉 掃描完成！");
            refreshResultList(); // 渲染結果
        });

        // 5. 任務失敗處理
        scanTask.setOnFailed(e -> {
            unbindTaskUI();
            setUiLocked(false);
            lblStatus.setText("❌ 掃描發生錯誤。");
            Debug.error("任務失敗原因:", scanTask.getException().getMessage());
        });

        // 6. 啟動執行緒
        Thread t = new Thread(scanTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClearLogs() {
        if (txtConsole != null) {
            txtConsole.clear();
            Debug.log("日誌已清除。");
        }
    }

    /**
     * 將掃描結果從 Map 轉換為 ListView 顯示
     */
    private void refreshResultList() {
        var results = currentProject.getSimilarityResults();
        ObservableList<SimilarityGroup> uiGroups = FXCollections.observableArrayList();

        if (results != null) {
            results.forEach((hash, list) -> {
                if (list.size() > 1) uiGroups.add(new SimilarityGroup(hash, list));
            });
        }

        // 重要：傳入當前專案的 FileManager 到 CellFactory
        resultListView.setCellFactory(param -> new SimilarityGroupCell(currentProject.getFileManager()));
        resultListView.setItems(uiGroups);
        lblResultSummary.setText("共發現 " + uiGroups.size() + " 組相似圖片");
        btnSmartSelect.setDisable(uiGroups.isEmpty());
        btnDeleteSelected.setDisable(uiGroups.isEmpty());
    }

    private void unbindTaskUI() {
        progressBar.progressProperty().unbind();
        lblStatus.textProperty().unbind();
        progressBar.setProgress(1.0);
    }

    // 其餘按鈕處理邏輯
    private void handleSelectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(btnSelectFolder.getScene().getWindow());
        if (dir != null) {
            selectedDirectory = dir.toPath();
            lblProjectPath.setText(selectedDirectory.toAbsolutePath().toString());
        }
    }

    private void handleCancelScan() {
        if (currentProject != null) {
            currentProject.cancel();
            lblStatus.setText("🛑 正在停止中...");
            btnCancel.setDisable(true);
        }
    }

    private void handleSmartSelect() {
        if (currentProject == null) return;
        FileManagement fm = currentProject.getFileManager();
        fm.clearList();
        resultListView.getItems().forEach(g -> fm.autoMarkDuplicatesToKeepBest(g.getAssets()));
        resultListView.refresh();
        Debug.log("智慧標記完成，選取數:", fm.getPendingDeleteCount());
    }

    private void handleDeleteSelected() {
        FileManagement fm = currentProject.getFileManager();
        if (fm.getPendingDeleteCount() == 0) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "確定要將檔案移至回收桶？", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                fm.executeBatchDelete(true);
                handleStartScan(); // 重新整理
            }
        });
    }

    private void setUiLocked(boolean locked) {
        btnSelectFolder.setDisable(locked);
        btnStart.setDisable(locked);
        comboAlgorithm.setDisable(locked);
        comboStrategy.setDisable(locked);
        spinnerThreshold.setDisable(locked);
        spinnerThreads.setDisable(locked);
        btnCancel.setDisable(!locked);
    }

    /**
     * 從 Dashboard 接收選定的資料夾，並自動啟動掃描。
     */
    public void initFromDashboard(File preSelectedDir) {
        if (preSelectedDir != null) {
            this.selectedDirectory = preSelectedDir.toPath();
            lblProjectPath.setText(selectedDirectory.toAbsolutePath().toString());

            if (lblStatus != null) {
                lblStatus.setText("✅ 專案已載入，請確認左側參數後點擊「開始掃描」。");
            }
        }
    }

    /**
     * 給 Main.java 呼叫的：在應用程式關閉前，確保所有背景任務安全終止
     */
    public void shutdown() {
        if (currentProject != null) {
            currentProject.cancel();
            Debug.log("收到系統關閉訊號，已通知專案中斷掃描。");
        }
    }
}
