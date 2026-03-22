package com.example.ui;

public final class Config {
    private Config() {
        throw new AssertionError("這是一個設定檔，不能被實體化！");
    }

    // ===== 日誌設定 =====
    public static final int MAX_UI_LOG_LEN = 50000;
    public static final int DELETE_UI_LOG_LEN = 10000;

    // ===== 演算法設定 =====
    public static final int USER_HD_MIN = 0;
    public static final int USER_HD_MAX = 20;
    public static final int USER_HD_INIT = 10;

    public static final int MAX_CACHE_SIZE = 500;

    // ===== UI 排版與圖片設定 =====
    public static final int THUMBNAIL_SIZE = 150;     // 縮圖的長寬
    public static final int THUMBNAIL_BOX_SIZE = 155; // 縮圖外框的大小
    public static final int CARD_WIDTH = 160;         // 每張圖片小卡片的總寬度
    public static final int H_BOX_MIN_HEIGHT = THUMBNAIL_SIZE + 50;

    // ===== Cell 內部間距設定 (新增) =====
    public static final int CELL_SPACING = 10;        // VBox 零件間距
    public static final int CELL_PADDING = 10;        // Cell 四週留白
    public static final int IMAGE_ROW_SPACING = 15;   // 圖片與圖片之間的距離
    public static final int CARD_SPACING = 5;         // 圖片小卡片的元件垂直間距
}

