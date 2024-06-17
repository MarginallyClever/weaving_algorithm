package com.marginallyclever.weavingradon;

import java.util.function.Consumer;

/**
 * Bresenham's line algorithm, adapted to call a Consumer for each point on the line.
 */
public class LineProducer {
    /**
     * Bresenham's line algorithm.
     * @param x0 start x
     * @param y0 start y
     * @param x1 end x
     * @param y1 end y
     * @param consumer callback for each point on the line.
     */
    public static void bresenham(int x0, int y0, int x1, int y1, Consumer<int[]> consumer) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            consumer.accept(new int[]{x0, y0});
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Wu's line algorithm.
     * @param x0 start x
     * @param y0 start y
     * @param x1 end x
     * @param y1 end y
     * @param consumer callback for each point on the line.  The float[] contains (int)x, (int)y, and the alpha value.
     */
    public static void wu(int x0, int y0, int x1, int y1, Consumer<float[]> consumer) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        float gradient;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (x1 < x0) {
                int tempX = x0;
                x0 = x1;
                x1 = tempX;
                int tempY = y0;
                y0 = y1;
                y1 = tempY;
            }
            gradient = (float) dy / dx;

            float y = y0 + gradient * (round(x0) - x0);
            for (int x = x0; x <= x1; x++) {
                consumer.accept(new float[]{x, (int)y, rfpart(y)});
                consumer.accept(new float[]{x, (int)y + 1, fpart(y)});
                y += gradient;
            }
        } else {
            if (y1 < y0) {
                int tempX = x0;
                x0 = x1;
                x1 = tempX;
                int tempY = y0;
                y0 = y1;
                y1 = tempY;
            }
            gradient = (float) dx / dy;

            float x = x0 + gradient * (round(y0) - y0);
            for (int y = y0; y <= y1; y++) {
                consumer.accept(new float[]{(int)x, y, rfpart(x)});
                consumer.accept(new float[]{(int)x + 1, y, fpart(x)});
                x += gradient;
            }
        }
    }

    private static int round(float value) {
        return (int) (value + 0.5);
    }

    private static float fpart(float value) {
        return value - (int) value;
    }

    private static float rfpart(float value) {
        return 1 - fpart(value);
    }
}
