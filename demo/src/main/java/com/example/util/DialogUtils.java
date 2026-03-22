package com.example.util;

import com.example.data.MediaAsset;
import javafx.scene.control.Alert;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DialogUtils {

    public static void showAssetDetails(MediaAsset asset) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("圖片詳細資料");
        alert.setHeaderText(asset.getFileName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        String content = String.format(
            "📁 檔案路徑：\n%s\n\n" +
            "💾 檔案大小：%.2f MB\n" +
            "🕒 最後修改：%s\n" +
            "🔑 指紋 Hash：0x%x",
            asset.getFilePath().toAbsolutePath().toString(),
            asset.getFileSize() / (1024.0 * 1024.0),
            formatter.format(asset.getLastModified()),
            asset.getFingerprint()
        );

        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(450);
        alert.showAndWait();
    }
}