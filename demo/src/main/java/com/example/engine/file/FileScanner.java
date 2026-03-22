package com.example.engine.file;

import com.example.data.MediaAsset;
import com.example.Debug;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileScanner {
    // 只編列支援的格式
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );
    // 建立一個執行緒安全的取消標記
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    /**
     * 掃描指定目錄，深入所有子資料夾，並返回 MediaAsset 列表
     * @param rootPath 使用者選擇的資料夾根目錄
     */
    public List<MediaAsset> scanFolder(Path rootPath) {
        List<MediaAsset> assets = new ArrayList<>();
        Debug.log("開始掃描專案目錄:", rootPath.toAbsolutePath());

        try {
            // 深入資料夾到沒有為止
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 每次進入新資料夾前檢查是否被取消
                    if (isCancelled.get()) return FileVisitResult.TERMINATE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // 如果是一般檔案，且副檔名符合支援格式
                    if (attrs.isRegularFile() && isSupportedFormat(file)) {
                        MediaAsset asset = new MediaAsset(
                                file,
                                file.getFileName().toString(),
                                attrs.size(),
                                attrs.lastModifiedTime().toInstant()
                        );
                        assets.add(asset);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // 防禦機制：遇到權限不足或無法讀取的檔案，印出警告但不崩潰
                    Debug.error("略過無法讀取的檔案或目錄:", file, "(", exc.getMessage(), ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Debug.error("掃描目錄時發生嚴重錯誤:", e.getMessage());
        }

        Debug.log("掃描完成！共發現", assets.size(), "張支援的圖片。");
        return assets;
    }

    // 判斷檔案是否為支援的圖片格式
    private boolean isSupportedFormat(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1);
            return SUPPORTED_EXTENSIONS.contains(extension);
        }
        return false;
    }

    public void cancelScan() {
        isCancelled.set(true);
        Debug.log("收到使用者取消指令，準備安全終止掃描...");
    }
}