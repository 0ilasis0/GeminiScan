package com.example.engine.finger.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import com.example.Debug;
import com.example.engine.finger.FingerprintStrategy;

public class ImageAHash implements FingerprintStrategy {
    // 定義縮放大小：8x8 會產生 64 bit，剛好填滿一個 long
    private static final int SIZE = 8;
    private static final int TOTAL_BITS = SIZE * SIZE;

    // 為每個執行緒建立專屬的 Buffer
    private static final ThreadLocal<int[]> PIXEL_BUFFER = ThreadLocal.withInitial(() -> new int[TOTAL_BITS]);
    // 快取 BufferedImage 畫布
    private static final ThreadLocal<BufferedImage> RESIZED_CANVAS = ThreadLocal.withInitial(() ->
        new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY));

    @Override
    public long generate(Path path) throws Exception {
        BufferedImage original = ImageUtils.readSubsampledImage(path, SIZE);
        Debug.requireNoNull(original,() -> "Unable to decode image file: " + path);

        // 縮放並轉為灰階 (減少細節，專注於結構)
        BufferedImage resized = RESIZED_CANVAS.get();
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, SIZE, SIZE, null);
        g.dispose();

        // 計算平均亮度
        double totalBrightness = 0;
        int[] pixels = PIXEL_BUFFER.get();
        // 取得灰階像素值 (0-255)
        resized.getRaster().getSamples(0, 0, SIZE, SIZE, 0, pixels);
        for (int pixel : pixels) {
            totalBrightness += pixel;
        }
        double avg = totalBrightness / TOTAL_BITS;

        // 根據平均值產生 64-bit 指紋
        long hash = 0L;
        for (int i = 0; i < TOTAL_BITS; i++) {
            if (pixels[i] >= avg) {
                hash |= (1L << i);
            }
        }

        return hash;
    }
}
