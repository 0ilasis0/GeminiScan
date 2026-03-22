package com.example.data;

import lombok.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

import com.example.Debug;

@Getter
@Setter
public class MediaAsset {
    private long fileId;          // 唯一識別碼 (可用 UUID 或 Hash)
    private Path filePath;        // 檔案絕對路徑
    private String fileName;      // 檔案名稱
    private long fileSize;        // 檔案大小 (bytes)
    private long fingerprint;     // 64-bit pHash 指紋 (使用 long 儲存效能最好)
    private Instant lastModified; // 最後修改時間

    @Builder
    public MediaAsset(Path filePath, String fileName, long fileSize, Instant lastModified) {
        this.filePath = Debug.requireNoNull(filePath,() -> fileName + " :file path must not be null")
                            .toAbsolutePath()
                            .normalize();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.fileId = 0L;          // 初始設為 0，存入資料庫後再回填真正的主鍵 ID
        this.fingerprint = 0L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaAsset that = (MediaAsset) o;
        return Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }
}
