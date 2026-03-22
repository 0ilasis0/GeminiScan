package com.example.engine.search;

import java.util.*;

public class DisjointSetUnion {
    private Map<Long, Long> parent = new HashMap<>();

    public void union(long h1, long h2) {
        long root1 = find(h1);
        long root2 = find(h2);
        if (root1 != root2) {
            parent.put(root1, root2);
        }
    }

    public Map<Long, List<Long>> getGroups() {
        Map<Long, List<Long>> groups = new HashMap<>();
        for (long h : parent.keySet()) {
            long root = find(h);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(h);
        }
        return groups;
    }

    private long find(long h) {
        if (!parent.containsKey(h)) {
            parent.put(h, h);
            return h;
        }
        if (parent.get(h) == h) return h;
        // 路徑壓縮優化
        parent.put(h, find(parent.get(h)));
        return parent.get(h);
    }

    public boolean isSameGroup(long h1, long h2) {
        return find(h1) == find(h2);
    }
}
