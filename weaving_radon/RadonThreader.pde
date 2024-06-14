import java.util.stream.IntStream;
import java.util.function.Consumer;

class RadonThreader {
  final ArrayList<ThreadColor> remainingThreads = new ArrayList<ThreadColor>();

  PImage referenceImage;
  PGraphics buffer;
  PGraphics currentRadonImage;
  PGraphics lastRadonImage;
  
  int bufferWidth;
  int bufferHeight;
  PVector center;
  int radius;
  int diag;
  float[] cosTheta = new float[180];
  float[] sinTheta = new float[180];
  int bestTheta=0;
  int bestR=0;

  RadonThreader(PImage referenceImage) {
    this.referenceImage = referenceImage;
    this.bufferWidth = referenceImage.width;
    this.bufferHeight = referenceImage.height;
    buffer = createGraphics(bufferWidth, bufferHeight);
    
    // Precompute cos and sin values
    for (int theta = 0; theta < 180; theta++) {
        float angle = radians(theta);
        cosTheta[theta] = cos(angle);
        sinTheta[theta] = sin(angle);
    }
    
    center = new PVector(bufferWidth / 2, bufferHeight / 2);
    radius = bufferWidth / 2;
    diag = bufferWidth;//(int)(radius*2);
    
    // Draw initial image to buffer
    buffer.beginDraw();
    buffer.image(referenceImage, 0, 0);
    buffer.endDraw();
    
    println("initial radon transform");
    // Apply Radon transform to the initial buffer
    currentRadonImage = createRadonTransform(buffer);
    println("done");
  }

  /** 
   * allocate all the threads once.  includes start, end, theta, r, and color.
   */
  void createThreads() {
    for (int i = 0; i < numNails; i++) {
      PVector start = nails.get(i).position;
      float sx = start.x - center.x;
      float sy = start.y - center.y;
        
      for (int j = i + 1; j < numNails; j++) {
        PVector end = nails.get(j).position;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        int theta = (int)degrees(atan2(dy, dx));
        if(theta < 0) {
          theta += 180; // Ensure theta is within [0-180)
        }
        if(theta >=180) {
          theta -= 180;
        }
        
        int r = (int)(sx * cos(radians(theta)) + sy * sin(radians(theta)));
        ThreadColor thread = new ThreadColor(start, end, theta, r, color(255, 255, 255,alpha)); 
        remainingThreads.add(thread);
      }
    }
  }

  PGraphics createRadonTransform(PGraphics pg) {
    pg.loadPixels();
    
    PGraphics radonImage = createGraphics(180, diag);
    radonImage.beginDraw();
    radonImage.loadPixels();
    
    IntStream.range(0, 180).parallel().forEach(theta -> {
      float cT = cosTheta[theta];
      float sT = sinTheta[theta];
      final int [] sum = new int[1];
      final int [] count = new int[1];
      
      for (int r = -radius; r < radius; r++) {
        sum[0] = 0;
        count[0] = 1;
        
        // Compute the start and end points for the line at this angle and distance
        // Calculate intersections with the circle
        float d = sqrt(radius * radius - r * r);
        float x0 = center.x + r * cT - d * sT;
        float y0 = center.y + r * sT + d * cT;
        float x1 = center.x + r * cT + d * sT;
        float y1 = center.y + r * sT - d * cT;
        
        // Use Bresenham's algorithm to sample points along the line
        bresenham((int)x0, (int)y0, (int)x1, (int)y1, point -> {
          int x = point[0];
          int y = point[1];
          if (x >= 0 && x < bufferWidth && y >= 0 && y < bufferHeight) {
            int index = x + y * bufferWidth;
            sum[0] += blue(pg.pixels[index]);
            count[0]++;
          }
        });
        
        int ri = r + radius;
        if (ri >= 0 && ri < diag && count[0]>0) {
          int v = (int)( (float)sum[0] / (float)count[0] );
          radonImage.pixels[theta + ri * 180] = color(v);
        }
      }
    });
    
    radonImage.updatePixels();
    radonImage.endDraw();
    return radonImage;
  }

  boolean addNextBestThread() {
    if (remainingThreads.isEmpty()) {
      return false;
    }
    
    currentRadonImage.loadPixels();
    
    float maxIntensity = 0;
    bestTheta=0;
    bestR=0;
    
    // Find the pixel with the maximum intensity in the current radon transform
    //int i=0;
    for(int r=-radius;r<radius;++r) {
      for(int theta = 0; theta<180; ++theta) {
        int i = theta + (r + radius) * 180;
        float intensity = blue(currentRadonImage.pixels[i]);
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
          bestTheta = theta;
          bestR = r;
        }
        //i++;
      }
    }

    ThreadColor bestThread = findThreadForMaxIntensity(bestTheta, bestR);
    if (bestThread != null && remainingThreads.size() > numNails) {
      println(maxIntensity +"\t"+ bestTheta +"\t"+ bestR +"\tfound "+ bestThread.theta +"\t"+ bestThread.r );
      remainingThreads.remove(bestThread);
      threads.add(bestThread);
      subtractThreadFromRadon(bestThread);
      return true;
    }
    
    return false;
  }
  
  void markPoint(int theta,int r) {
    currentRadonImage.loadPixels();
    currentRadonImage.pixels[theta + (int)(r+radius) * 180] = 0;
    currentRadonImage.updatePixels();
  }

  ThreadColor findThreadForMaxIntensity(int targetTheta, int targetR) {
    ThreadColor nearestThread = null;
    float minDistance = Float.MAX_VALUE;
    
    for (ThreadColor thread : remainingThreads) {
      float distanceSquared = sq(thread.theta - targetTheta) + sq(thread.r - targetR);
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
    buffer.beginDraw();
    buffer.background(0);
    thread.display(buffer);
    buffer.endDraw();
    thread.radonTransform = lastRadonImage = createRadonTransform(buffer);
    lastRadonImage.loadPixels();
    
    for(int i = 0; i < currentRadonImage.pixels.length; i++) {
      float threadIntensity = blue(lastRadonImage.pixels[i]);
      float currentIntensity = blue(currentRadonImage.pixels[i]);
      currentRadonImage.pixels[i] = color(max(currentIntensity - threadIntensity, 0));
    }
    
    currentRadonImage.updatePixels();
  }
  
  // Bresenham's line algorithm
  void bresenham(int x0, int y0, int x1, int y1, Consumer<int[]> consumer) {
    int dx = abs(x1 - x0);
    int dy = -abs(y1 - y0);
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
