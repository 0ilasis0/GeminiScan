package com.example.engine;

import com.example.engine.finger.FingerprintStrategy;
import com.example.engine.finger.image.ImageAHash;
import com.example.engine.finger.image.ImagePHash;

public enum HashAlgorithm {
    AHASH("快速分析 (aHash)", new ImageAHash()),
    PHASH("精準分析 (pHash)", new ImagePHash());

    private final String displayName;
    private final FingerprintStrategy strategy;

    HashAlgorithm(String displayName, FingerprintStrategy strategy) {
        this.displayName = displayName;
        this.strategy = strategy;
    }

    public String getDisplayName() { return displayName; }
    public FingerprintStrategy getStrategy() { return strategy; }
}
