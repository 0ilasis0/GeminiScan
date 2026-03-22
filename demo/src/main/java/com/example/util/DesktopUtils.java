package com.example.util;

import com.example.Debug;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;

public class DesktopUtils {

    public static void openFileLocation(Path filePath) {
        try {
            File parentDir = filePath.getParent().toFile();
            if (Desktop.isDesktopSupported() && parentDir.exists()) {
                Desktop.getDesktop().open(parentDir);
            } else {
                Debug.warn("作業系統不支援直接開啟資料夾，或路徑不存在。");
            }
        } catch (Exception ex) {
            Debug.error("開啟檔案位置失敗: " + ex.getMessage());
        }
    }

    public static void openImage(Path filePath) {
        try {
            if (Desktop.isDesktopSupported() && filePath.toFile().exists()) {
                Desktop.getDesktop().open(filePath.toFile());
            }
        } catch (Exception ex) {
            Debug.error("開啟圖片失敗: " + ex.getMessage());
        }
    }
}