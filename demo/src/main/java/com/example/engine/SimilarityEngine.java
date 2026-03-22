package com.example.engine;

import com.example.engine.finger.FingerprintStrategy;
import com.example.engine.search.DisjointSetUnion;
import com.example.engine.search.VPTree;
import com.example.data.MediaAsset;
import com.example.Debug;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.concurrent.ForkJoinPool;
import java.util.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SimilarityEngine {
    private FingerprintStrategy currentStrategy;
    // 漢明距離閥值
    private int similarityThresholdRadius;
    // 限制最大並行工作數量(核心數量)
    private int CoresCount;
    // 引擎專屬的煞車踏板
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    // 新增回呼通道
    private BiConsumer<Integer, Integer> progressCallback;

    public void setProgressCallback(BiConsumer<Integer, Integer> callback) {
        this.progressCallback = callback;
    }

    public SimilarityEngine(HashAlgorithm algorithm, int thresholdRadius, int CoresCount) {
        this.currentStrategy = algorithm.getStrategy();
        this.similarityThresholdRadius = thresholdRadius;

        int availableCores = Runtime.getRuntime().availableProcessors();
        this.CoresCount = (CoresCount <= 0 || CoresCount > availableCores) ? Math.max(1, availableCores - 2) : CoresCount;
    }

    public Map<Long, List<MediaAsset>> processPipeline(List<MediaAsset> assets) {
        // 每次啟動前，確保煞車是放開的
        isCancelled.set(false);
        if (assets == null || assets.isEmpty()) return Collections.emptyMap();

        Map<Long, List<MediaAsset>> hashToAssetsMap = new ConcurrentHashMap<>();

        Debug.log("Step 1: 開始計算指紋...");
        // 建立一個執行緒安全的計數器
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalAssets = assets.size();

        // 計算 1% 的資料量是多少
        int reportThreshold = Math.max(1, totalAssets / 100);

        // 設定並行核心處理數量
        try (ForkJoinPool customThreadPool = new ForkJoinPool(CoresCount)) {
            customThreadPool.submit(() -> {
                assets.parallelStream().forEach(asset -> {
                    if (isCancelled.get()) return;
                    processAsset(asset, hashToAssetsMap);

                    // 每次處理1%，就更新計數器，並通知 UI！
                    int current = processedCount.incrementAndGet();
                    if (current % reportThreshold == 0 || current == totalAssets) {
                        if (progressCallback != null) {
                            progressCallback.accept(current, totalAssets);
                        }
                    }
                });
            }).get();
        } catch (Exception e) {
            Debug.error("多執行緒計算發生錯誤: " + e.getMessage());
        }

        // 如果沒有足夠的資料可以比對，直接回傳空結果
        if (hashToAssetsMap.size() < 2) return Collections.emptyMap();
        // 檢查是否在中途被取消，直接回傳空結果
        if (isCancelled.get()) return Collections.emptyMap();

        Debug.log("Step 2: 建立 VP-Tree...");
        long[] hashesArray = new long[hashToAssetsMap.size()];
        int index = 0;
        for (Long h : hashToAssetsMap.keySet()) {
            hashesArray[index++] = h;
        }

        VPTree tree = new VPTree();
        tree.build(hashesArray);

        Debug.log("Step 3: 搜尋相似度並用 DSU 打包...");
        DisjointSetUnion dsu = new DisjointSetUnion();

        for (long queryHash : hashesArray) {
            if (isCancelled.get()) return Collections.emptyMap();

            List<Long> searchResults = new ArrayList<>();
            // 在半徑範圍內搜尋相似的指紋
            tree.search(tree.getRoot(), queryHash, similarityThresholdRadius, searchResults);

            for (long matchedHash : searchResults) {
                // 將找到的相似指紋歸入同一個群組
                dsu.union(queryHash, matchedHash);
            }
        }

        Debug.log("Step 4: 將 DSU 的結果轉換為前台 UI 需要的格式...");
        Map<Long, List<MediaAsset>> finalGroups = new HashMap<>();
        Map<Long, List<Long>> groupedHashes = dsu.getGroups();

        for (Map.Entry<Long, List<Long>> entry : groupedHashes.entrySet()) {
            long rootId = entry.getKey();
            List<Long> similarHashesInGroup = entry.getValue();

            // 過濾掉沒有相似圖片的孤立群組
            int totalAssetsInGroup = similarHashesInGroup.stream()
                    .mapToInt(h -> hashToAssetsMap.get(h).size())
                    .sum();

            if (totalAssetsInGroup > 1) {
                List<MediaAsset> groupedAssets = new ArrayList<>();
                for (long h : similarHashesInGroup) {
                    groupedAssets.addAll(hashToAssetsMap.get(h));
                }
                finalGroups.put(rootId, groupedAssets);
            }
        }

        return finalGroups;
    }

    private void processAsset(MediaAsset asset, Map<Long, List<MediaAsset>> hashToAssetsMap) {
        try {
            // 計算指紋
            if (asset.getFingerprint() == 0L) {
                long hash = currentStrategy.generate(asset.getFilePath());
                asset.setFingerprint(hash);
            }

            // 放入 Map
            hashToAssetsMap.computeIfAbsent(asset.getFingerprint(), k -> Collections.synchronizedList(new ArrayList<>()))
                           .add(asset);

        } catch (Exception e) {
            Debug.error("處理失敗:", asset.getFilePath(), "-", e.getMessage());
        }
    }

    public void cancel() {
        isCancelled.set(true);
        Debug.log("比對引擎收到取消訊號，將立刻停止...");
    }
}
