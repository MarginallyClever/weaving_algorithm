package com.marginallyclever.weavingradon;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;
import java.util.function.Consumer;


public class RadonThreader {
    private final double LENGTH_FACTOR = 0.94;
    private final double[] cosTheta = new double[180];
    private final double[] sinTheta = new double[180];

    private BufferedImage oneThreadRadonTransform;
    private BufferedImage currentRadonImage;

    private int bufferWidth;
    private int bufferHeight;
    private Vector2d center;
    public int radius;
    private int diameter;
    private Loom loom;

    public RadonThreader() {
        super();

        // Precompute cos and sin values
        for (int theta = 0; theta < 180; theta++) {
            double angle = Math.toRadians(theta);
            cosTheta[theta] = Math.cos(angle);
            sinTheta[theta] = Math.sin(angle);
        }
    }

    public void setLoom(Loom loom) {
        this.loom = loom;
    }

    public void setImage(BufferedImage referenceImage) {
        this.bufferWidth = referenceImage.getWidth();
        this.bufferHeight = referenceImage.getHeight();

        center = new Vector2d(bufferWidth / 2.0, bufferHeight / 2.0);
        radius = bufferWidth / 2;
        diameter = bufferWidth;

        oneThreadRadonTransform = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);

        System.out.println("initial radon transform");
        // Apply radon transform to the initial buffer
        currentRadonImage = createRadonTransform(referenceImage);
        System.out.println("done");
    }

    public BufferedImage createRadonTransform(BufferedImage pg) {
        BufferedImage radonImage = new BufferedImage(180, diameter, BufferedImage.TYPE_INT_ARGB);

        IntStream.range(0, 180).parallel().forEach(theta -> {
            double c = cosTheta[theta];
            double s = sinTheta[theta];
            final int [] sum = new int[3];
            final int [] count = new int[1];

            for (int r = -radius; r < radius; r++) {
                sum[0] = 0;
                //sum[1] = 0;
                //sum[2] = 0;
                count[0] = 0;

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
                        //sum[1] += v.getGreen();
                        //sum[2] += v.getBlue();
                        count[0]++;
                    }
                });

                int ri = r + radius;
                if (ri >= 0 && ri < diameter && count[0]>0) {
                    int r2 = (int)( (double)sum[0] / Math.pow(count[0],LENGTH_FACTOR) );
                    //int g2 = (int)( (double)sum[1] / (double)count[0] );
                    //int b2 = (int)( (double)sum[2] / (double)count[0] );
                    //int v = (int)Math.min(255, Math.max(0, (r2 + g2 + b2) / 3.0));
                    int v = Math.max(0, Math.min(255, r2));
                    radonImage.setRGB(theta, ri, (new Color(v,v,v).getRGB()));
                }
            }
        });

        return radonImage;
    }

    /**
     * Mask the current radon image with the remaining threads.
     */
    public void maskCurrentRadonByAllThreads() {
        System.out.println("filterRadonByThreads");
        //
        var filter = new BufferedImage(currentRadonImage.getWidth(), currentRadonImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(ThreadColor thread : loom.potentialThreads) {
            try {
                filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), Color.WHITE.getRGB());
            } catch (Exception e) {
                System.out.println("OOB "+thread.thetaR);
            }
        }
        for(ThreadColor thread : loom.selectedThreads) {
            try {
                filter.setRGB(thread.thetaR.theta, thread.thetaR.getY(radius), Color.WHITE.getRGB());
            } catch (Exception e) {
                System.out.println("OOB "+thread.thetaR);
            }
        }

        // mask currentRadonImage with filter.
        for(int y = 0; y < currentRadonImage.getHeight(); y++) {
            for(int x = 0; x < currentRadonImage.getWidth(); x++) {
                if(filter.getRGB(x,y) != Color.WHITE.getRGB()) {
                    currentRadonImage.setRGB(x,y,Color.BLACK.getRGB());
                }
            }
        }
    }

    public void addNextBestThread() {
        if (loom.potentialThreads.isEmpty()) return;

        ThetaR bestFound = getNextBestThread();
        ThreadColor bestThread = loom.findThreadForMaxIntensity(bestFound);
        loom.addNextBestThread(bestThread);

        subtractThreadFromRadon(bestThread);
    }

    /**
     * Sets the bestTheta/bestR for the next thread to add.
     * bestTheta is in the range 0...180.
     * bestR is in the range -radius...radius.
     */
    public ThetaR getNextBestThread() {
        double maxIntensity = 0;

        ThetaR bestFound = new ThetaR(0,0);
        ThetaR current = new ThetaR(0,0);
        // Find the pixel with the maximum intensity in the current radon transform
        for(current.r=-radius;current.r<radius;++current.r) {
            for(current.theta = 0; current.theta<180; ++current.theta) {
                var col = currentRadonImage.getRGB(current.theta, current.getY(radius));
                double intensity = intensity(col);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    bestFound.set(current);
                }
            }
        }
        //System.out.println("found "+maxIntensity +"\t"+ bestFound);
        return bestFound;
    }

    void markPoint(ThetaR best) {
        //System.out.println("Mark "+best.theta+","+best.r);
        currentRadonImage.setRGB(best.theta, best.getY(radius), Color.BLACK.getRGB());
    }

    void subtractThreadFromRadon(ThreadColor thread) {
        // draw one thread to a black canvas
        var g = oneThreadRadonTransform.getGraphics();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setColor(new Color(0,0,0,loom.alpha));
        g2.clearRect(0,0, bufferWidth, bufferHeight);
        thread.display(oneThreadRadonTransform);
        // get the radon transform of that canvas
        BufferedImage threadRadonImage = createRadonTransform(oneThreadRadonTransform);

        // subtract the new radon transform from the current radon transform
        for(int x = 0; x < currentRadonImage.getWidth(); x++) {
            for(int y = 0; y < currentRadonImage.getHeight(); y++) {
                double threadIntensity  = intensity( threadRadonImage.getRGB(x,y));
                double currentIntensity = intensity(currentRadonImage.getRGB(x,y));
                int v = (int)Math.max(currentIntensity - threadIntensity, 0);
                Color c = new Color(v,v,v);
                currentRadonImage.setRGB(x,y,c.getRGB());
            }
        }
        // clean up
        g2.dispose();
    }

    double intensity(int col) {
        Color c = new Color(col);
        //return (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
        return c.getRed();
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

    public BufferedImage getCurrentRadonImage() {
        return currentRadonImage;
    }

    public boolean shouldStop() {
        return loom.shouldStop();
    }

    public void setLoomAndImage(Loom loom, BufferedImage grey) {
        setLoom(loom);
        setImage(grey);
    }
}