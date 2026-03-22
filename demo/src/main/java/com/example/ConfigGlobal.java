package com.example;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigGlobal {

    // 私有建構子：防止任何人 new ConfigGlobal()
    private ConfigGlobal() {
        throw new AssertionError("這是一個設定檔，不能被實體化！");
    }

    public static final int SCENE_WIDTH = 1280;
    public static final int SCENE_HEIGHT = 800;
    public static final int SCENE_MIN_WIDTH = 960;
    public static final int SCENE_MIN_HEIGHT = 600;
    public static final String APP_TITLE = "GeminiScan";

    public static final String DEJA_VIEW = "dejaview_data";
    public static final String DB_BASE = "database.db";
    public static Path getDataSubPath(String... subPaths) {
        return Paths.get(DEJA_VIEW, subPaths);
    }
}