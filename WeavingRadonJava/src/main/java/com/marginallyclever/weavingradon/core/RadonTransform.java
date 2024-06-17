package com.marginallyclever.weavingradon.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

/**
 * A RadonTransform is a representation of an image in Radon space.  It is a 2D array of intensities.
 * The X axis represents theta, the angle of the line about the center of the image.
 * The Y axis represents the distance from the center of the image.
 */
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
    private final int radius;


    public RadonTransform(BufferedImage source) {
        if(source==null) {
            throw new IllegalArgumentException("image cannot be null");
        }
        if(source.getWidth()!=source.getHeight()) {
            throw new IllegalArgumentException("image must be square, is "+source.getWidth()+"x"+source.getHeight());
        }

        int diameter = source.getWidth();
        radius = diameter / 2;
        graph = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);

        IntStream.range(0, 180).parallel().forEach(theta -> {
            double c = cosTheta[theta];
            double s = sinTheta[theta];
            // three color channels + count of samples
            final int [] sum = new int[3+1];

            for (int r = -radius; r < radius; r++) {
                sum[0] = 0;
                sum[1] = 0;
                sum[2] = 0;
                sum[3] = 0;

                // Compute the start and end points for the line at this angle and distance
                // Calculate intersections with the circle
                double d = Math.sqrt(radius*radius - r*r);
                double x0 = Math.max(0,Math.min(diameter-1, radius + r * c - d * s));
                double y0 = Math.max(0,Math.min(diameter-1, radius + r * s + d * c));
                double x1 = Math.max(0,Math.min(diameter-1, radius + r * c + d * s));
                double y1 = Math.max(0,Math.min(diameter-1, radius + r * s - d * c));

                // Use Bresenham's algorithm to sample points along the line
                LineProducer.bresenham((int)x0, (int)y0, (int)x1, (int)y1, point -> {
                    int x = point[0];
                    int y = point[1];
                    var v = new Color(source.getRGB(x,y));
                    sum[0] += v.getRed();
                    sum[1] += v.getGreen();
                    sum[2] += v.getBlue();
                    sum[3]++;
                });

                if(sum[3]>0) {
                    int r2 = (int)( (double)sum[0] / (double)sum[3] );
                    int g2 = (int)( (double)sum[1] / (double)sum[3] );
                    int b2 = (int)( (double)sum[2] / (double)sum[3] );
                    int v = (int)Math.min(255, Math.max(0, (r2 + g2 + b2) / 3.0));
                    setIntensity(theta, r + radius, v);
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
     * @param theta 0...180
     * @param y 0...diameter
     * @param i 0...255
     */
    public void setIntensity(int theta, int y, int i) {
        graph.setRGB(theta, y, new Color(i,i,i).getRGB());
    }

    /**
     * @param theta 0...180
     * @param y 0...diameter
     * @return 0...255
     */
    public int getIntensity(int theta, int y) {
        return new Color(graph.getRGB(theta, y)).getRed();
    }

    public void subtractThread(LoomThread thread) {
        if (thread == null) throw new IllegalArgumentException("thread cannot be null");

        LoomThread thread2 = new LoomThread(thread);
        thread2.col = new Color(255, 255, 255);
        BufferedImage oneThreadOnCanvas = drawOneThread(thread2);
        RadonTransform oneThreadRadonTransform = new RadonTransform(oneThreadOnCanvas);
        subtract(oneThreadRadonTransform);
    }

    // subtract arg0 from the current radon transform
    public void subtract(RadonTransform arg0) {
        for(int x = 0; x < graph.getWidth(); x++) {
            for(int y = 0; y < graph.getHeight(); y++) {
                int currentIntensity = getIntensity(x,y);
                if(currentIntensity==0) continue;

                int threadIntensity = arg0.getIntensity(x,y);
                if(threadIntensity==0) continue;

                int v = Math.max(currentIntensity - threadIntensity, 0);
                Color c = new Color(v,v,v);
                graph.setRGB(x,y,c.getRGB());
            }
        }
    }

    // draw one thread to a black canvas diameter * diameter in size.
    public BufferedImage drawOneThread(LoomThread thread) {
        BufferedImage oneThreadOnCanvas = new BufferedImage(graph.getHeight(), graph.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = oneThreadOnCanvas.createGraphics();
        RenderHintHelper.setRenderHints(g2);

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

    public void maskWith(BufferedImage filter,int maskColor) {
        // mask currentRadonImage with filter.
        for(int y = 0; y < graph.getHeight(); y++) {
            for(int x = 0; x < graph.getWidth(); x++) {
                if(filter.getRGB(x,y) != maskColor) {
                    setIntensity(x, y, 0);
                }
            }
        }
    }
}
