package com.example.project;

public enum DuplicateStrategy {
    OVERWRITE,  // 徹底重來：刪除舊資料，重新掃描
    SYNC,       // 增量同步：保留舊資料，只掃描新增或變動的檔案
}
