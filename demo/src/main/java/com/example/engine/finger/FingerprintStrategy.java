package com.example.engine.finger;

import java.nio.file.Path;

/**
 * 指紋產生策略介面
 * 所有的檔案分析器（圖片、影片、音訊）都必須實作此介面
 */
public interface FingerprintStrategy {

    /**
     * 根據檔案路徑產生一個 64 位元的感知雜湊值 (Perceptual Hash)
     * * @param path 檔案的絕對路徑
     * @return 64-bit long 型態的指紋
     * @throws Exception 處理過程中的 IO 錯誤或格式不支援
     */
    long generate(Path path) throws Exception;

    /**
     * 計算兩個指紋之間的相似度，使用 Hamming Distance
     * * @param h1 指紋 A
     * @param h2 指紋 B
     * @return 相似度百分比 (0.0 ~ 1.0)
     */
    default double calculateSimilarity(long h1, long h2) {
        int distance = Long.bitCount(h1 ^ h2);

        // 64 位元中，完全一樣是 0，完全不同是 64
        return (64.0 - distance) / 64.0;
    }
}