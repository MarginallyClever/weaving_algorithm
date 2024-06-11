
class RadonThreader {
  final ArrayList<ThreadColor> remainingThreads = new ArrayList<ThreadColor>();

  PImage referenceImage;
  PGraphics buffer;
  PGraphics currentRadonImage;
  
  int bufferWidth;
  int bufferHeight;
  PVector center;
  float radius;
  int diag;

  RadonThreader(PImage referenceImage) {
    this.referenceImage = referenceImage;
    this.bufferWidth = referenceImage.width;
    this.bufferHeight = referenceImage.height;
    buffer = createGraphics(bufferWidth, bufferHeight);
    
    center = new PVector(bufferWidth / 2, bufferHeight / 2);
    radius = bufferWidth / 2;
    //diag = (int)(radius+1);
    //diag = (int) sqrt(bufferWidth * bufferWidth + bufferHeight * bufferHeight);
    diag = (int)(radius*2);
    
    // Draw initial image to buffer
    buffer.beginDraw();
    buffer.image(referenceImage, 0, 0);
    buffer.endDraw();
    
    println("initial radon transform");
    // Apply Radon transform to the initial buffer
    currentRadonImage = createRadonTransform(buffer);
    println("done");
  }

  void createThreads() {
    for (int i = 0; i < numNails; i++) {
      for (int j = i + 1; j < numNails; j++) {
        PVector start = nails.get(i).position;
        PVector end = nails.get(j).position;
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        int theta = (int)degrees(atan2(dy, dx));
        if(theta < 0) {
          theta += 180; // Ensure theta is within 0-179
        }
        int r = (int) ((start.x - center.x) * cos(radians(theta))
                     + (start.y - center.y) * sin(radians(theta)));
        ThreadColor thread = new ThreadColor(start, end, color(255, 255, 255),theta,r); 
        remainingThreads.add(thread);
      }
    }
  }

  PGraphics createRadonTransform(PGraphics pg) { //<>//
    PGraphics radonImage = createGraphics(180, diag);
    radonImage.beginDraw();
    radonImage.loadPixels();
    pg.loadPixels();
    
    for (int theta = 0; theta < 180; theta++) {
      float angle = radians(theta);
      float cosTheta = cos(angle);
      float sinTheta = sin(angle);
      
      for (int r = (int)-radius; r < (int)radius; r++) {
        long sum = 0;
        int count = 0;
        
        // Compute the start and end points for the line at this angle and distance
        // Calculate intersections with the circle
        float d = sqrt(radius * radius - r * r);
        float x0 = center.x + r * cosTheta - d * sinTheta;
        float y0 = center.y + r * sinTheta + d * cosTheta;
        float x1 = center.x + r * cosTheta + d * sinTheta;
        float y1 = center.y + r * sinTheta - d * cosTheta;
        
        // Use Bresenham's algorithm to sample points along the line
        ArrayList<int[]> points = bresenham((int)x0, (int)y0, (int)x1, (int)y1);
        for (int[] point : points) {
          int x = point[0];
          int y = point[1];
          if (x >= 0 && x < bufferWidth && y >= 0 && y < bufferHeight) {
            int index = x + y * bufferWidth;
            sum += red(pg.pixels[index]);
            count++;
          }
        }
        
        int ri = r + diag / 2;
        if (ri >= 0 && ri < diag && count>0) {
          int v = (int)( (float)sum / (float)count );
          radonImage.pixels[theta + ri * 180] = color(v);
        }
      }
    }
    
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
    int bestTheta=0, bestR=0;
    
    // Find the pixel with the maximum intensity in the current Radon image
    int i=0;
    for(int theta = 0;theta<180;++theta) {
      for(int r=0;r<diag;++r) {
        float intensity = red(currentRadonImage.pixels[i]);
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
          bestTheta = theta;
          bestR = r;
        }
        ++i;
      }
    }
    
    println(maxIntensity +"\t"+ bestTheta +"\t"+ bestR);
    
    currentRadonImage.loadPixels();
    currentRadonImage.pixels[bestTheta + bestR*180] = color(0,0,0);
    currentRadonImage.updatePixels();

    ThreadColor bestThread = findThreadForMaxIntensity(bestTheta, bestR);
    if (bestThread != null && remainingThreads.size() > numNails) {
      println("found "+ bestThread.theta +"\t"+ bestThread.r );
      remainingThreads.remove(bestThread);
      threads.add(bestThread);
      subtractThreadFromRadon(bestThread);
      markPoint(bestTheta,bestR);
      return true;
    }
    
    return false;
  }

  ThreadColor findThreadForMaxIntensity(int targetTheta, int targetR) {
    ThreadColor nearestThread = null;
    float minDistance = Float.MAX_VALUE;
    
    for (ThreadColor thread : remainingThreads) {
        float distance = sq(thread.theta - targetTheta) + sq(thread.r - targetR);
        if (distance < minDistance) {
            minDistance = distance;
            nearestThread = thread;
        }
    }
    return nearestThread;
  }

  void subtractThreadFromRadon(ThreadColor thread) {
    buffer.beginDraw();
    buffer.background(0);
    thread.display(buffer);
    buffer.endDraw();
    thread.radonTransform = createRadonTransform(buffer);
    thread.radonTransform.loadPixels();
    currentRadonImage.loadPixels();
    
    for(int i = 0; i < currentRadonImage.pixels.length; i++) {
      float threadIntensity = red(thread.radonTransform.pixels[i]);
      float currentIntensity = red(currentRadonImage.pixels[i]);
      currentRadonImage.pixels[i] = color(max(currentIntensity - threadIntensity, 0));
    }
    
    currentRadonImage.updatePixels();
  }
  
  void markPoint(int bestTheta,int bestR) {/*
    currentRadonImage.beginDraw();
    currentRadonImage.fill(0,0,0);
    currentRadonImage.ellipse(bestTheta, bestR, 1, 1);
    currentRadonImage.endDraw();*/
    
    currentRadonImage.loadPixels();
    currentRadonImage.pixels[bestTheta + bestR*180] = color(0,0,0);
    currentRadonImage.updatePixels();
  }
  
  // Bresenham's line algorithm
  ArrayList<int[]> bresenham(int x0, int y0, int x1, int y1) {
    ArrayList<int[]> points = new ArrayList<int[]>();
    int dx = abs(x1 - x0);
    int dy = -abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx + dy;
    
    while (true) {
      points.add(new int[]{x0, y0});
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
    return points;
  }
}
