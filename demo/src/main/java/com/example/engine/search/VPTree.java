package com.example.engine.search;

import java.util.*;

public class VPTree {
    private Node root;

    // 定義節點結構
    private static class Node {
        long vantagePoint; // 基準點指紋
        int threshold;  // 中位數距離
        Node left;         // 圈內 (距離 <= threshold)
        Node right;        // 圈外 (距離 > threshold)

        Node(long vp) { this.vantagePoint = vp; }
    }

    public void build(long[] hashes) {
        long[] copy = new long[hashes.length];
        System.arraycopy(hashes, 0, copy, 0, hashes.length);

        // 使用索引 (start, end) 進行原地建樹
        root = buildTree(copy, 0, copy.length);
    }

    private Node buildTree(long[] arr, int start, int end) {
        if (start >= end) return null;

        // 將陣列最後一個元素作為基準點
        int vpIndex = end - 1;
        long vp = arr[vpIndex];
        Node node = new Node(vp);

        if (start == vpIndex) return node;

        int midIndex = start + (vpIndex - start) / 2;
        quickSelect(arr, start, vpIndex - 1, midIndex, vp);

        // 找出中位數作為閥值
        node.threshold = hammingDistance(vp, arr[midIndex]);

        node.left = buildTree(arr, start, midIndex + 1);
        node.right = buildTree(arr, midIndex + 1, vpIndex);

        return node;
    }

    private void quickSelect(long[] arr, int left, int right, int k, long vp) {
        while (left < right) {
            int pivotIndex = partition(arr, left, right, vp);
            if (pivotIndex == k) {
                return;
            } else if (pivotIndex < k) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
    }

    private int partition(long[] arr, int left, int right, long vp) {
        long pivotVal = arr[right];
        int pivotDist = hammingDistance(vp, pivotVal);
        int i = left;

        for (int j = left; j < right; j++) {
            if (hammingDistance(vp, arr[j]) <= pivotDist) {
                swap(arr, i, j);
                i++;
            }
        }
        swap(arr, i, right);
        return i;
    }

    private void swap(long[] arr, int i, int j) {
        long temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // 範圍搜尋 (找出距離在 radius 內的所有點)
    public void search(Node node, long target, int radius, List<Long> results) {
        if (node == null) return;

        int dist = hammingDistance(node.vantagePoint, target);

        // 如果基準點符合條件，加入結果
        if (dist <= radius) {
            results.add(node.vantagePoint);
        }

        // VP-Tree 剪枝邏輯：判斷是否需要搜尋左/右子樹
        if (dist - radius <= node.threshold) {
            search(node.left, target, radius, results);
        }
        if (dist + radius > node.threshold) {
            search(node.right, target, radius, results);
        }
    }

    private int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    public Node getRoot() { return root; }
}
