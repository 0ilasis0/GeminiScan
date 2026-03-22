package com.example.engine.finger.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

public class ImageUtils {

    /**
     * 極速降採樣讀取圖片 (專為 Hash 演算法設計)
     * 不會載入完整 4K 圖片，只會載入所需大小的縮圖，忽略 EXIF。
     * @param path 圖片路徑
     * @param targetSize 目標大小 (例如 pHash 的 32)
     * @return 降採樣後的小圖
     */
    public static BufferedImage readSubsampledImage(Path path, int targetSize) throws Exception {
        File file = path.toFile();

        // 取得影像輸入流 (不直接解碼)
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) {
                throw new Exception("無法建立 ImageInputStream: " + path);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new Exception("沒有支援的圖片解碼器: " + path);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);

                // 計算比例與設定參數
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                // 計算降採樣比例
                int ratio = Math.max(1, (Math.min(width, height) / targetSize));

                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(ratio, ratio, 0, 0);

                // 執行降採樣讀取並回傳結果
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }
}
