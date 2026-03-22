package com.example.engine.finger.image;

import com.example.Debug;
import com.example.engine.finger.FingerprintStrategy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class ImagePHash implements FingerprintStrategy {
    private static final int SIZE = 32;      // pHash 通常用 32x32 進行 DCT
    private static final int HASH_SIZE = 8;  // 最後取左上角 8x8 產生 64-bit
    private static final double X2HASH_SIZE = HASH_SIZE * HASH_SIZE - 1;

    // 為每個執行緒配備專屬的暫存陣列
    private static final ThreadLocal<double[][]> INPUT_BUFFER = ThreadLocal.withInitial(() -> new double[SIZE][SIZE]);
    private static final ThreadLocal<double[][]> TEMP_BUFFER = ThreadLocal.withInitial(() -> new double[SIZE][HASH_SIZE]);
    private static final ThreadLocal<double[][]> OUTPUT_BUFFER = ThreadLocal.withInitial(() -> new double[HASH_SIZE][HASH_SIZE]);

    // cos計算查表
    private static final double[][] DCT_MATRIX = new double[HASH_SIZE][SIZE];

    static {
        double invSqrt2 = 1.0 / Math.sqrt(2.0);

        for (int u = 0; u < HASH_SIZE; u++) {
            double cu = (u == 0) ? invSqrt2 : 1.0;
            for (int x = 0; x < SIZE; x++) {
                DCT_MATRIX[u][x] = cu * Math.cos(((2.0 * x + 1.0) * u * Math.PI) / (2.0 * SIZE));
            }
        }
    }

    @Override
    public long generate(Path path) throws Exception {
        BufferedImage original = ImageUtils.readSubsampledImage(path, SIZE);
        Debug.requireNoNull(original, () -> "pHash: 無法讀取檔案 " + path);

        // 縮放並轉灰階 (32x32)
        BufferedImage resized = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, SIZE, SIZE, null);
        g.dispose();

        // 取得像素矩陣並轉為 double 進行數學運算
        double[][] vals = INPUT_BUFFER.get();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                vals[x][y] = resized.getRaster().getSample(x, y, 0);
            }
        }

        // 執行 DCT (離散餘弦變換)
        double[][] dctVals = DCT(vals);

        // 計算左上角 8x8 的平均值 (排除 DC 分量 dctVals[0][0])
        // 因為 [0][0] 是整張圖的平均亮度，會干擾結構特徵
        double total = 0;
        for (int x = 0; x < HASH_SIZE; x++) {
            for (int y = 0; y < HASH_SIZE; y++) {
                total += dctVals[x][y];
            }
        }
        total -= dctVals[0][0];
        double avg = total / X2HASH_SIZE;

        // 根據平均值產生 64-bit 指紋
        long hash = 0L;
        int bitCounter = 0;
        for (int x = 0; x < HASH_SIZE; x++) {
            for (int y = 0; y < HASH_SIZE; y++) {
                if (dctVals[x][y] > avg) {
                    hash |= (1L << bitCounter);
                }
                bitCounter++;
            }
        }
        return hash;
    }

    /**
     * 執行 2D DCT 轉換
     * @param input 32x32 的灰階像素矩陣
     * @return 轉換後的頻率矩陣8x8
     */
    public static double[][] DCT(double[][] input) {
        double[][] temp = TEMP_BUFFER.get();
        double[][] output = OUTPUT_BUFFER.get();

        // 對每一列做 1D DCT
        for (int i = 0; i < SIZE; i++) {
            for (int u = 0; u < HASH_SIZE; u++) {
                double sum = 0;
                for (int x = 0; x < SIZE; x++) {
                    sum += input[i][x] * DCT_MATRIX[u][x];
                }
                temp[i][u] = sum;
            }
        }

        // 對每一行做 1D DCT
        for (int j = 0; j < HASH_SIZE; j++) {
            for (int v = 0; v < HASH_SIZE; v++) {
                double sum = 0;
                for (int y = 0; y < SIZE; y++) {
                    sum += temp[y][j] * DCT_MATRIX[v][y];
                }
                output[v][j] = sum;
            }
        }

        return output;
    }
}
