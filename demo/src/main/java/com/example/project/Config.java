package com.example.project;

public final class Config {
    private Config() {
        throw new AssertionError("這是一個設定檔，不能被實體化！");
    }
    public static final int DB_BATCH_SIZE = 2000 - 1;
}
