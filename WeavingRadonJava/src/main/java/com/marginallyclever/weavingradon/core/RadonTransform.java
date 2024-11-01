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
    private final double [] importanceMap;
    private final int radius;
    private final int diameter;

    public RadonTransform(BufferedImage source) {
        if(source==null) {
            throw new IllegalArgumentException("image cannot be null");
        }
        if(source.getWidth()!=source.getHeight()) {
            throw new IllegalArgumentException("image must be square, is "+source.getWidth()+"x"+source.getHeight());
        }

        diameter = source.getWidth();
        radius = diameter / 2;

        importanceMap = createImportanceMap();

        heatMap = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);
        graph = new double[180 * diameter];

        // instead of `for(int theta = 0; theta < 180; theta++) {` run in parallel because faster.
        IntStream.range(0, 180).parallel().forEach(theta -> {
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
                        var scale = getImportance(x0,y0);
                        sr += scale * v.getRed();
                        sg += scale * v.getGreen();
                        sb += scale * v.getBlue();
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
                    double finalIntensity = (sr+sg+sb) / count;
                    setIntensity(theta,r,finalIntensity);
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
        diameter = radius*2;

        importanceMap = createImportanceMap();

        final double scale = 1.0 / radius;
        heatMap = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);
        graph = new double[180 * diameter];

        double a = thread.col.getAlpha() / 256.0;
        double intensity = (thread.col.getRed() + thread.col.getGreen() + thread.col.getBlue()) * a/3.0;

        // instead of `for(int theta = 0; theta < 180; theta++) {` run in parallel because faster.
        IntStream.range(0, 180).parallel().forEach(theta -> {
            for (int r = -radius; r < radius; r++) {
                double result = testIntersection(thread,theta,r);/*
                if(result!=0 && result<radius) {
                    Vector2d intersection = findIntersection(thread, theta, r);
                    assert intersection != null;
                    int x = Math.max(diameter-1,Math.min(0,(int)(radius+intersection.x)));
                    int y = Math.max(diameter-1,Math.min(0,(int)(radius+intersection.y)));

                    double importance=0;
                    try {
                        importance = getImportance(x,y);
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println("theta="+theta+" r="+r+" x="+x+" y="+y);
                        throw e;
                    }
                    result *= importance;
                }*/

                double finalIntensity = result * scale * intensity;
                setIntensity(theta, r, finalIntensity);
            }
        });
    }

    /**
     * sinMap is a map of the importance of each pixel in the image.
     * if the radius is 1 and the center is 0, then any given pixel is sin( (distance from center) / radius).
     * @return a map of sin values.
     */
    public double [] createImportanceMap() {
        double min = 0.5;
        double range = min+1.0;

        var map = new double[diameter * diameter];
        for (int y = -radius; y < radius; y++) {
            for (int x = -radius; x < radius; x++) {
                double d = Math.sqrt(x * x + y * y);
                d = Math.min(d, radius);
                var result = (min+Math.cos(Math.PI * Math.abs(d) / diameter))/range;
                result = Math.max(0,result);
                result = Math.min(1,result);
                map[(y+radius) * diameter + x + radius] = result;
            }
        }
        return map;
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
        // if the lines are parallel (same theta)...
        if(Math.abs(thread.thetaR.theta-theta)<1e-6) {
            if(Math.abs(thread.thetaR.r-r)<1e-6) {
                // and overlapping (same r) then the two lines are equal.
                return radius;
            }
            // otherwise they are parallel and never intersect.
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
        graph[theta * diameter + r + radius] = intensity;
    }

    private double getImportance(int x, int y) {
        return importanceMap[y * diameter + x];
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
