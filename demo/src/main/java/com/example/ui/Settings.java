package com.example.ui;

import com.example.engine.HashAlgorithm;
import com.example.project.DuplicateStrategy;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import java.util.prefs.Preferences;

/**
 * 偏好設定管家：專門負責與作業系統的 Preferences API 對接
 * 處理 UI 元件的數值載入、預設值設定以及自動儲存監聽
 */
public class Settings {

    // 取得與目前類別綁定的 Preferences 節點
    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    // 定義儲存的 Key 名稱，避免打錯字
    private static final String KEY_ALGO = "algorithm";
    private static final String KEY_STRATEGY = "strategy";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String KEY_THREADS = "threads";

    /**
     * 從系統載入設定並初始化 UI 元件
     */
    public void loadPreferences(ComboBox<HashAlgorithm> comboAlgo,
                                ComboBox<DuplicateStrategy> comboStrategy,
                                Spinner<Integer> spinnerThreshold,
                                Spinner<Integer> spinnerThreads) {

        // 初始化演算法下拉選單
        comboAlgo.getItems().setAll(HashAlgorithm.values());
        String savedAlgo = prefs.get(KEY_ALGO, HashAlgorithm.AHASH.name());
        try {
            comboAlgo.getSelectionModel().select(HashAlgorithm.valueOf(savedAlgo));
        } catch (IllegalArgumentException e) {
            comboAlgo.getSelectionModel().select(HashAlgorithm.AHASH);
        }

        // 初始化策略下拉選單
        comboStrategy.getItems().setAll(DuplicateStrategy.values());
        String savedStrategy = prefs.get(KEY_STRATEGY, DuplicateStrategy.SYNC.name());
        try {
            comboStrategy.getSelectionModel().select(DuplicateStrategy.valueOf(savedStrategy));
        } catch (IllegalArgumentException e) {
            comboStrategy.getSelectionModel().select(DuplicateStrategy.SYNC);
        }

        // 初始化漢明距離 Spinner (從 Config 讀取範圍)
        int savedThreshold = prefs.getInt(KEY_THRESHOLD, Config.USER_HD_INIT);
        spinnerThreshold.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
            Config.USER_HD_MIN, Config.USER_HD_MAX, savedThreshold
        ));

        // 開啟鍵盤輸入
        spinnerThreshold.setEditable(true);
        syncSpinnerValueOnFocus(spinnerThreshold);

        // 初始化執行緒 Spinner (預設為系統核心數)
        int defaultCores = Runtime.getRuntime().availableProcessors();
        int savedThreads = prefs.getInt(KEY_THREADS, defaultCores);
        spinnerThreads.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
            1, defaultCores * 2, savedThreads
        ));

        spinnerThreads.setEditable(true);
        syncSpinnerValueOnFocus(spinnerThreads);
    }

    /**
     * 確保 Spinner 在失去焦點時會自動「確認」鍵盤輸入的內容
     */
    private void syncSpinnerValueOnFocus(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((s, ov, nv) -> {
            if (!nv) { // 當失去焦點時 (nv == false)
                spinner.increment(0); // 技巧：增加 0 會觸發 Spinner 內部的 Text-to-Value 轉換
            }
        });
    }

    /**
     * 綁定自動儲存事件：只要 UI 數值改變，立刻寫入 Preferences
     */
    public void bindAutoSave(ComboBox<HashAlgorithm> comboAlgo,
                             ComboBox<DuplicateStrategy> comboStrategy,
                             Spinner<Integer> spinnerThreshold,
                             Spinner<Integer> spinnerThreads) {

        // 監聽演算法切換
        comboAlgo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) prefs.put(KEY_ALGO, newVal.name());
        });

        // 監聽策略切換
        comboStrategy.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) prefs.put(KEY_STRATEGY, newVal.name());
        });

        // 監聽閥值改變
        spinnerThreshold.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) prefs.putInt(KEY_THRESHOLD, newVal);
        });

        // 監聽執行緒數量改變
        spinnerThreads.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) prefs.putInt(KEY_THREADS, newVal);
        });
    }

    /**
     * 重置所有設定為預設值 (選配功能，若有「重置按鈕」時可呼叫)
     */
    public void resetToDefault() {
        try {
            prefs.clear();
        } catch (Exception e) {
            com.example.Debug.error("重置設定失敗: " + e.getMessage());
        }
    }
}