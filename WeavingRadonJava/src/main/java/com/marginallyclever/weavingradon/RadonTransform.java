package com.marginallyclever.weavingradon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class RadonTransform {
    private static final double[] cosTheta = new double[180];
    private static final double[] sinTheta = new double[180];

    static {
        // Precompute cos and sin values
        for (int theta = 0; theta < 180; theta++) {
            double angle = Math.toRadians(theta);
            cosTheta[theta] = Math.cos(angle);
            sinTheta[theta] = Math.sin(angle);
        }
    }

    private final BufferedImage graph;


    public RadonTransform(BufferedImage pg) {
        if(pg==null) throw new IllegalArgumentException("image cannot be null");
        if(pg.getWidth()!=pg.getHeight()) throw new IllegalArgumentException("image must be square, is "+pg.getWidth()+"x"+pg.getHeight());
        int diameter = pg.getWidth();
        int radius = diameter / 2;

        graph = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);

        IntStream.range(0, 180).parallel().forEach(theta -> {
            double c = cosTheta[theta];
            double s = sinTheta[theta];
            final int [] sum = new int[1];
            final int [] count = new int[1];
            final int [] total = new int[1];

            for (int r = -radius; r < radius; r++) {
                sum[0] = 0;
                count[0] = 0;
                total[0] = 0;

                // Compute the start and end points for the line at this angle and distance
                // Calculate intersections with the circle
                double d = Math.sqrt(radius*radius - r*r);
                double x0 = radius + r * c - d * s;
                double y0 = radius + r * s + d * c;
                double x1 = radius + r * c + d * s;
                double y1 = radius + r * s - d * c;

                // Use Bresenham's algorithm to sample points along the line
                BresenhamProducer.bresenham((int)x0, (int)y0, (int)x1, (int)y1, point -> {
                    int x = point[0];
                    int y = point[1];
                    if (x >= 0 && x < diameter && y >= 0 && y < diameter) {
                        var v = new Color(pg.getRGB(x,y));
                        sum[0] += v.getRed();
                        count[0]++;
                    }
                    total[0]++;
                });

                int ri = r + radius;
                if (ri >= 0 && ri < diameter && count[0]>0) {
                    int r2 = (int)( (double)sum[0] / count[0] );
                    //int g2 = (int)( (double)sum[1] / (double)count[0] );
                    //int b2 = (int)( (double)sum[2] / (double)count[0] );
                    //int v = (int)Math.min(255, Math.max(0, (r2 + g2 + b2) / 3.0));
                    int v = Math.max(0, Math.min(255, r2));
                    graph.setRGB(theta, ri, (new Color(v,v,v).getRGB()));
                }
            }
        });
    }

    public int getWidth() {
        return graph.getWidth();
    }

    public int getHeight() {
        return graph.getHeight();
    }

    /**
     *
     * @param x
     * @param y
     * @param i 0...255
     */
    public void setIntensity(int x, int y, int i) {
        graph.setRGB(x, y, new Color(i,i,i).getRGB());
    }

    public int getIntensity(int theta, int y) {
        return new Color(graph.getRGB(theta, y)).getRed();
    }

    public void subtractThread(ThreadColor thread) {
        if(thread==null) throw new IllegalArgumentException("thread cannot be null");

        ThreadColor thread2 = new ThreadColor(thread);
        thread2.col = new Color(255,255,255);
        BufferedImage oneThreadOnCanvas = drawOneThread(thread2);
        RadonTransform oneThreadRadonTransform = new RadonTransform(oneThreadOnCanvas);

        // subtract the new radon transform from the current radon transform
        for(int x = 0; x < graph.getWidth(); x++) {
            for(int y = 0; y < graph.getHeight(); y++) {
                int currentIntensity = getIntensity(x,y);
                if(currentIntensity==0) continue;

                int threadIntensity = oneThreadRadonTransform.getIntensity(x,y);
                if(threadIntensity==0) continue;

                int v = Math.max(currentIntensity - threadIntensity, 0);
                Color c = new Color(v,v,v);
                graph.setRGB(x,y,c.getRGB());
            }
        }
    }

    // draw one thread to a black canvas diameter * diameter in size.
    private BufferedImage drawOneThread(ThreadColor thread) {
        BufferedImage oneThreadOnCanvas = new BufferedImage(graph.getHeight(), graph.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = oneThreadOnCanvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setColor(new Color(0,0,0,0));
        g2.clearRect(0,0, oneThreadOnCanvas.getWidth(), oneThreadOnCanvas.getHeight());
        thread.display(oneThreadOnCanvas);
        // clean up
        g2.dispose();

        return oneThreadOnCanvas;
    }

    public Image getGraph() {
        return graph;
    }
}
