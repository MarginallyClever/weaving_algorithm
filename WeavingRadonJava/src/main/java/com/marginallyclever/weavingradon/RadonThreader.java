package com.marginallyclever.weavingradon;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.function.Consumer;


public class RadonThreader {
    public static final int NUM_NAILS = 188;
    public final int alpha = 64;

    public final List<Vector2d> nails = new ArrayList<>();
    public final List<ThreadColor> threads = new ArrayList<>();
    public final List<ThreadColor> remainingThreads = new ArrayList<>();

    public BufferedImage referenceImage;
    public BufferedImage buffer;
    public BufferedImage currentRadonImage;
    public BufferedImage lastRadonImage;

    private int bufferWidth;
    private int bufferHeight;
    private Vector2d center;
    public int radius;
    private int diag;
    private double[] cosTheta = new double[180];
    private double[] sinTheta = new double[180];
    public int bestTheta=0;
    public int bestR=0;

    public RadonThreader() {
        super();

        // Precompute cos and sin values
        for (int theta = 0; theta < 180; theta++) {
            double angle = Math.toRadians(theta);
            cosTheta[theta] = Math.cos(angle);
            sinTheta[theta] = Math.sin(angle);
        }
    }

    public void setImage(BufferedImage referenceImage) {
        this.referenceImage = referenceImage;
        this.bufferWidth = referenceImage.getWidth();
        this.bufferHeight = referenceImage.getHeight();

        center = new Vector2d(bufferWidth / 2.0, bufferHeight / 2.0);
        radius = bufferWidth / 2;
        diag = bufferWidth;//(int)(radius*2);

        buffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);

        // Draw initial image to buffer
        createNails();
        createThreads();

        System.out.println("initial radon transform");
        // Apply radon transform to the initial buffer
        currentRadonImage = createRadonTransform(referenceImage);
        filterRadonByThreads();
        System.out.println("done");
    }

    private void createNails() {
        System.out.println("createNails");
        nails.clear();
        for(int i=0;i<NUM_NAILS;++i) {
            double angle = i * Math.PI * 2 / NUM_NAILS;
            nails.add(new Vector2d(
                    center.x + Math.sin(angle) * radius,
                    center.y + Math.cos(angle) * radius
            ));
        }
    }

    /**
     * allocate all the threads once.  includes start, end, theta, r, and color.
     */
    private void createThreads() {
        System.out.println("createThreads");
        double maxR = 0;
        for (int i = 0; i < NUM_NAILS; i++) {
            Vector2d start = nails.get(i);
            double sx = start.x - center.x;
            double sy = start.y - center.y;

            for (int j = i + 1; j < NUM_NAILS; j++) {
                Vector2d end = nails.get(j);
                double dx = end.x - start.x;
                double dy = end.y - start.y;
                int theta = (int)Math.toDegrees(Math.atan2(dy, dx));

                // Ensure theta is within [0-180)
                if(theta < 0) theta += 180;
                if(theta >= 180) theta -= 180;

                double angle = Math.toRadians(theta);
                int r = (int)(sx * Math.cos(angle)
                            + sy * Math.sin(angle));

                //System.out.println("theta="+theta+" r="+r);
                maxR = Math.max(maxR, Math.abs(r));
                ThreadColor thread = new ThreadColor(start, end, theta, r, new Color(255, 255, 255,alpha));
                remainingThreads.add(thread);
            }
        }
        System.out.println("maxR="+maxR+" image size="+bufferWidth+"x"+bufferHeight);
    }

    public BufferedImage createRadonTransform(BufferedImage pg) {
        BufferedImage radonImage = new BufferedImage(180, diag, BufferedImage.TYPE_INT_ARGB);

        IntStream.range(0, 180).parallel().forEach(theta -> {
            double c = cosTheta[theta];
            double s = sinTheta[theta];
            final int [] sum = new int[3];
            final int [] count = new int[1];

            for (int r = -radius; r < radius; r++) {
                sum[0] = 0;
                sum[1] = 0;
                sum[2] = 0;
                count[0] = 1;

                // Compute the start and end points for the line at this angle and distance
                // Calculate intersections with the circle
                double d = Math.sqrt(radius*radius - r*r);
                double x0 = center.x + r * c - d * s;
                double y0 = center.y + r * s + d * c;
                double x1 = center.x + r * c + d * s;
                double y1 = center.y + r * s - d * c;

                // Use Bresenham's algorithm to sample points along the line
                bresenham((int)x0, (int)y0, (int)x1, (int)y1, point -> {
                    int x = point[0];
                    int y = point[1];
                    if (x >= 0 && x < bufferWidth && y >= 0 && y < bufferHeight) {
                        var v = new Color(pg.getRGB(x,y));
                        sum[0] += v.getRed();
                        sum[1] += v.getGreen();
                        sum[2] += v.getBlue();
                        count[0]++;
                    }
                });

                int ri = r + radius;
                if (ri >= 0 && ri < diag && count[0]>0) {
                    int r2 = (int)( (double)sum[0] / (double)count[0] );
                    int g2 = (int)( (double)sum[1] / (double)count[0] );
                    int b2 = (int)( (double)sum[2] / (double)count[0] );
                    radonImage.setRGB(theta, ri, (new Color(r2,g2,b2).getRGB()));
                }
            }
        });

        return radonImage;
    }

    private void filterRadonByThreads() {
        System.out.println("filterRadonByThreads");
        //var filter = new BufferedImage(currentRadonImage.getWidth(), currentRadonImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        //Color c = Color.BLUE;
        var g = currentRadonImage.getGraphics();
        g.setColor(Color.PINK);
        g.drawRect(0,0,currentRadonImage.getWidth(),currentRadonImage.getHeight());
        g.setColor(Color.PINK);
        for(ThreadColor thread : threads) {
            //filter.setRGB(thread.theta,thread.r,Color.WHITE.getRGB());
            //currentRadonImage.setRGB(thread.theta,thread.r+radius,0xff);
            g.drawOval(thread.theta-2,thread.r+radius-2,4,4);
        }/*
        // mask currentRadonImage with buffer.
        for(int y = 0; y < currentRadonImage.getHeight(); y++) {
            for(int x = 0; x < currentRadonImage.getWidth(); x++) {
                if(filter.getRGB(x,y) != Color.WHITE.getRGB()) {
                    currentRadonImage.setRGB(x,y,Color.BLACK.getRGB());
                }
            }
        }*/
    }

    public boolean addNextBestThread() {
        if (remainingThreads.isEmpty()) return false;

        getNextBestThread();

        ThreadColor bestThread = findThreadForMaxIntensity(bestTheta, bestR);
        if (bestThread != null && remainingThreads.size() > NUM_NAILS*0.2) {
            System.out.println("matches "+ bestThread.theta +"\t"+ bestThread.r );
            remainingThreads.remove(bestThread);
            threads.add(bestThread);
            subtractThreadFromRadon(bestThread);
            return true;
        }

        return false;
    }

    // sets the bestTheta/bestR for the next thread to add.
    public void getNextBestThread() {
        double maxIntensity = 0;
        bestTheta=0;
        bestR=0;

        // Find the pixel with the maximum intensity in the current radon transform
        for(int r=-radius;r<radius;++r) {
            for(int theta = 0; theta<180; ++theta) {
                var col = currentRadonImage.getRGB(theta, r+radius);
                double intensity = intensity(col);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    bestTheta = theta;
                    bestR = r;
                }
            }
        }
        System.out.println("found "+maxIntensity +"\t"+ bestTheta +"\t"+ bestR);
    }

    void markPoint(int theta,int r) {
        currentRadonImage.setRGB(theta, r + radius, 0);
    }

    ThreadColor findThreadForMaxIntensity(int targetTheta, int targetR) {
        ThreadColor nearestThread = null;
        double minDistance = Double.MAX_VALUE;

        for (ThreadColor thread : remainingThreads) {
            double distanceSquared = Math.pow(thread.theta - targetTheta,2) + Math.pow(thread.r - targetR,2);
            if (distanceSquared < minDistance) {
                minDistance = distanceSquared;
                nearestThread = thread;
            }
        }

        markPoint(targetTheta,targetR);
        if(minDistance>2) return null;

        return nearestThread;
    }

    void subtractThreadFromRadon(ThreadColor thread) {
        var g = buffer.getGraphics();
        g.setColor(new Color(0,0,0,0));
        g.clearRect(0,0, bufferWidth, bufferHeight);
        thread.display(buffer);
        lastRadonImage = createRadonTransform(buffer);

        for(int x = 0; x < currentRadonImage.getWidth(); x++) {
            for(int y = 0; y < currentRadonImage.getHeight(); y++) {
                double threadIntensity = intensity(lastRadonImage.getRGB(x,y));
                double currentIntensity = intensity(currentRadonImage.getRGB(x,y));
                Color c = new Color((int)Math.max(currentIntensity - threadIntensity, 0));
                currentRadonImage.setRGB(x,y,c.getRGB());
            }
        }
    }

    double intensity(int col) {
        Color c = new Color(col);
        return (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
    }

    // Bresenham's line algorithm
    void bresenham(int x0, int y0, int x1, int y1, Consumer<int[]> consumer) {
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