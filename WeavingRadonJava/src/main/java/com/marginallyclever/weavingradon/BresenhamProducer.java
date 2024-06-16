package com.marginallyclever.weavingradon;

import java.util.function.Consumer;

// Bresenham's line algorithm
public class BresenhamProducer {
    /**
     * Bresenham's line algorithm.
     * @param x0 start x
     * @param y0 start y
     * @param x1 end x
     * @param y1 end y
     * @param consumer callback for each point on the line
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
}
