package com.marginallyclever.weavingradon.core;

import javax.vecmath.Vector2d;
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

    // Precompute cos and sin values
    static {
        for (int theta = 0; theta < 180; theta++) {
            double angle = Math.toRadians(theta);
            cosTheta[theta] = Math.cos(angle);
            sinTheta[theta] = Math.sin(angle);
        }
    }

    private final BufferedImage heatMap;
    private final double [] graph;
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
        heatMap = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);
        graph = new double[180 * diameter];

        // run in parallel because faster.
        IntStream.range(0, 180).parallel().forEach(theta -> {
        //for(int theta = 0; theta < 180; theta++) {
            double c = cosTheta[theta];
            double s = sinTheta[theta];
            // three color channels + count of samples
            double sr, sg, sb;
            int count;

            for (int r = -radius; r < radius; r++) {
                sr = 0;
                sg = 0;
                sb = 0;
                count = 0;

                // Compute the start and end points for the line at this angle and distance
                // Calculate intersections with the circle
                double d = Math.sqrt(radius*radius - r*r);
                int x0 = (int)Math.max(0,Math.min(diameter-1, radius + r * c - d * s));
                int y0 = (int)Math.max(0,Math.min(diameter-1, radius + r * s + d * c));
                int x1 = (int)Math.max(0,Math.min(diameter-1, radius + r * c + d * s));
                int y1 = (int)Math.max(0,Math.min(diameter-1, radius + r * s - d * c));

                // Bresenham's line algorithm
                int dx = Math.abs(x1 - x0);
                int dy = -Math.abs(y1 - y0);
                int sx = x0 < x1 ? 1 : -1;
                int sy = y0 < y1 ? 1 : -1;
                int err = dx + dy;

                while (true) {
                    {
                        // get the color at this point
                        var v = new Color(source.getRGB(x0,y0));
                        sr += v.getRed();
                        sg += v.getGreen();
                        sb += v.getBlue();
                        count++;
                    }
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

                if(count>0) {
                    double i = (sr+sg+sb) / count;
                    setIntensity(theta,r,i);
                }
            }
        });
    }

    /**
     * calculate the intensity at the given theta and r.
     * Assumes the "image" is a square of size radius*2.
     * Assumes there is a single line on the image described by thread.thetaR.
     * @param radius the radius of the image.
     * @param thread the thread to test against.
     */
    public RadonTransform(int radius,LoomThread thread) {
        this.radius = radius;
        int diameter = radius*2;

        final double scale = 1.0 / radius;
        heatMap = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);
        graph = new double[180 * diameter];

        double a = thread.col.getAlpha() / 256.0;
        double intensity = (thread.col.getRed() + thread.col.getGreen() + thread.col.getBlue()) * a/3.0;

        /// instead of `for(int theta = 0; theta < 180; theta++) {` run in parallel because faster.
        IntStream.range(0, 180).parallel().forEach(theta -> {
            for (int r = -radius; r < radius; r++) {
                double intersection = testIntersection(thread,theta,r) * scale;
                double finalIntensity = intersection * intensity;
                setIntensity(theta, r, finalIntensity);
            }
        });
    }

    /**
     * scale the values in the graph to fit in the range 0...255 using the maxIntensity.
     */
    private void updateHeatMap() {
        int diameter = radius*2;
        double maxIntensity = 0;
        for (double v : graph) {
            maxIntensity = Math.max(maxIntensity, v);
        }

        for(int i=0;i<graph.length;++i) {
            int intensity = (int)(graph[i] * 255 / maxIntensity);
            intensity = Math.max(0,Math.min(255,intensity));
            heatMap.setRGB(i/diameter,i%diameter, getHeatMapColor(intensity).getRGB());
        }
    }

    /**
     * Find the intensity of the intersection of the line theta/r and the thread.
     * @param thread the thread to test against.
     * @param theta the angle of the line.
     * @param r the distance from the center of the image.
     * @return a value between 0 and radius.
     */
    public double testIntersection(LoomThread thread, int theta, int r) {
        // Find the intersection of line theta/r and the thread.
        // if the lines are parallel (same theta) and overlapping (same r) then the two lines are equal.
        if(Math.abs(thread.thetaR.theta-theta)<1e-6) {
            if(Math.abs(thread.thetaR.r-r)<1e-6) {
                return radius;
            }
            return 0;
        }

        // is there an intersection?
        Vector2d intersection = findIntersection(thread, theta, r);
        if(intersection == null) return 0;
        // is the intersection inside the circle?
        if(intersection.lengthSquared() >= radius*radius) return 0;

        // the intensity at the intersection depends on the relative angle of the two lines.
        // the closer the angle, the more intense the color.
        double v = 1.0/Math.abs(Math.sin(Math.toRadians(thread.thetaR.theta - theta)));
        return Math.max(0,Math.min(radius,v));
    }

    /**
     * Find the intersection of the line theta/r and the thread.
     * @param thread the thread to test against.
     * @param theta the angle of the line.
     * @param r the distance from the center of the image.
     * @return the intersection point or null if the lines are parallel.
     */
    private Vector2d findIntersection(LoomThread thread, double theta, double r) {
        double thetaRad = Math.toRadians(theta);
        double threadThetaRad = Math.toRadians(thread.thetaR.theta);
        double threadR = thread.thetaR.r;

        // Intersection calculations.
        double sinTheta = Math.sin(thetaRad);
        double cosTheta = Math.cos(thetaRad);
        double sinThreadTheta = Math.sin(threadThetaRad);
        double cosThreadTheta = Math.cos(threadThetaRad);

        double denom = cosTheta * sinThreadTheta - sinTheta * cosThreadTheta;
        if (Math.abs(denom) < 1e-6) return null; // Lines are parallel.

        double x = (r * sinThreadTheta - threadR * sinTheta) / denom;
        double y = (threadR * cosTheta - r * cosThreadTheta) / denom;
        return new Vector2d(x, y);
    }

    public int getWidth() {
        return heatMap.getWidth();
    }

    public int getHeight() {
        return heatMap.getHeight();
    }

    /**
     * @param theta 0...180
     * @param r -radius...radius
     * @param intensity 0...255
     */
    public void setIntensity(int theta, int r, double intensity) {
        int diameter = radius*2;
        graph[theta * diameter + r + radius] = intensity;
    }

    /**
     * @param i 0...255
     * @return a color in the rainbow spectrum.
     */
    private Color getHeatMapColor(int i) {
        return new Color(i,i,i);  // greyscale
        //return new Color(Color.HSBtoRGB(i/256.0f,1f,1f));  // rainbow
    }

    /**
     * @param theta 0...180
     * @param r -radius...radius
     * @return 0...255
     */
    public double getIntensity(int theta, int r) {
        int diameter = radius*2;
        return graph[theta * diameter + r + radius];
    }

    /**
     * Get the color at the given theta and y.
     * @param theta 0...180
     * @param r -radius...radius
     * @return the color at the given theta and y.
     */
    @Deprecated
    public Color getColor(double theta, int r) {
        return new Color(heatMap.getRGB((int)theta, r+radius));
    }

    public void subtractThread(LoomThread thread) {
        if (thread == null) throw new IllegalArgumentException("thread cannot be null");
        RadonTransform oneThreadRadonTransform = new RadonTransform(radius, thread);
        subtract(oneThreadRadonTransform);
    }

    // this -= remove
    public void subtract(RadonTransform remove) {
        for(int theta=0;theta<180;++theta) {
            for(int r=-radius;r<radius;++r) {
                subtract(remove,theta,r);
            }
        }
    }

    public void subtract(RadonTransform remove,int theta, int r) {
        double intensity = getIntensity(theta, r) - remove.getIntensity(theta, r);
        setIntensity(theta, r, intensity);
    }

    private Color subtractColor(Color c1, Color c2) {
        float alphaFactor = c2.getAlpha() / 255.0f;

        float r = Math.max(0, Math.min(255, c1.getRed() - c2.getRed() * alphaFactor));
        float g = Math.max(0, Math.min(255, c1.getGreen() - c2.getGreen() * alphaFactor));
        float b = Math.max(0, Math.min(255, c1.getBlue() - c2.getBlue() * alphaFactor));

        return new Color((int) r, (int) g, (int) b);
    }

    public BufferedImage getHeatMap() {
        updateHeatMap();
        return heatMap;
    }

    /**
     * Mask the current radon transform with the filter.  Keep all parts of this transform where the filter image
     * is equal to the mask color.
     * @param filter the filter image
     * @param maskColor the color to mask with
     */
    public void maskWith(BufferedImage filter,int maskColor) {
        for(int theta = 0;theta<180;++theta) {
            for(int r = -radius;r<radius;++r) {
                if(filter.getRGB(theta,r+radius) != maskColor) {
                    setIntensity(theta, r, 0);
                }
            }
        }
    }
}
