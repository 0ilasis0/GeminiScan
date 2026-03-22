package com.example.data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimilarityGroup {
    private final long groupHash;        // 這一組的代表性 Hash
    private final List<MediaAsset> assets; // 這一組內的所有相似圖片
}
