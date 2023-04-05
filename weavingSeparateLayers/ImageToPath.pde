import java.util.HashSet;

class ImageToPath {
  int numNails;
  PVector [] nails;
  ArrayList<Integer> nailPath = new ArrayList<Integer>();
  HashSet<String> visitedPaths = new HashSet<String>();
  int currentNailIndex;
  int intensity;
  PGraphics croppedImg;
  PGraphics pathImg;
  boolean paused=false;
  color filterColor;
  color channelColor;

  class NailPathValue {
    int index;
    float value;
    
    public NailPathValue(int index,float value) {
      this.index = index;
      this.value = value;
    }
  };

  public ImageToPath(int numNails,int diameter,color channelColor) {
    this.numNails = numNails;
    this.channelColor = channelColor;
    createNails(diameter/2f);
    
    pathImg = createGraphics(diameter,diameter);
    pathImg.beginDraw();
    pathImg.background(0,0,0,0);
    pathImg.endDraw();
  }
  
  public void begin(PImage sourceImage) {
    color c = channelColor;
    PImage redChannel = generateGrayscaleImage(sourceImage,c);
    int alpha = (int)max(1.0f,calculateAverageRedIntensity(redChannel)*alphaAdjust);
    color c2 = color(red(c),green(c),blue(c),alpha);
    
    begin(redChannel,(int)alpha,c2);
  }
  
  float calculateAverageRedIntensity(PImage img) {
    float totalRed = 0;
    int numPixels = img.width * img.height;
  
    img.loadPixels();
    for (int i = 0; i < numPixels; i++) {
      float v = red(circleBorder.pixels[i])/255.0f;
      totalRed += red(img.pixels[i])*v;
    }
  
    return totalRed / numPixels;
  }
    
  public void begin(PImage image,int intensity,color filterColor) {
    this.croppedImg = createGraphicsFromImage(image);
    this.filterColor = filterColor;
    this.intensity = intensity;
    nailPath.clear();
    visitedPaths.clear();
    currentNailIndex = findBestStartingNail();
    nailPath.add(currentNailIndex);
    pathImg.beginDraw();
    pathImg.background(red(filterColor),green(filterColor),blue(filterColor),0);
    pathImg.endDraw();
  }
  
  private PGraphics createGraphicsFromImage(PImage image) {
    PGraphics result = createGraphics(image.width,image.height);
    result.beginDraw();
    result.image(image,0,0);
    result.endDraw();
    
    return result;
  }
  
  public void drawNails() {
    fill(255);
    stroke(255);
    // Draw the "nails" around the edge of the image
    for (PVector nail : nails) {
      ellipse(nail.x, nail.y, 1, 1);
    }
  }
  
  // draw the entire path
  public void drawPath() {
    if(pathImg ==null) return;
    image(pathImg,0,0);
  }
  
  public void recalculatePathImage() {    
    pathImg.beginDraw();
    pathImg.background(0,0,0,0);
    pathImg.noFill();
    pathImg.smooth(mySmooth);
    pathImg.stroke(filterColor);
    pathImg.strokeWeight(myStrokeWeight);
    
    for (int i = 0; i < nailPath.size() - 1; i++) {
      int nail1Index = nailPath.get(i);
      int nail2Index = nailPath.get(i + 1);
      PVector nail1 = nails[nail1Index];
      PVector nail2 = nails[nail2Index];
  
      pathImg.line(nail1.x, nail1.y, nail2.x, nail2.y);
    }
    pathImg.endDraw();
  }
  
  public void drawAddedPath(int nextNailIndex) {
    PVector nail1 = nails[currentNailIndex];
    PVector nail2 = nails[nextNailIndex];

    pathImg.beginDraw();
    pathImg.noFill();
    pathImg.smooth(mySmooth);
    pathImg.stroke(filterColor);
    pathImg.strokeWeight(myStrokeWeight);
    pathImg.blendMode(ADD);
    pathImg.line(nail1.x, nail1.y, nail2.x, nail2.y);
    pathImg.endDraw();
  }
  
  public void createNails(float radius) {
    float r = radius-5;
    
    // Create an array of "nails" in a circle around the edge of the image
    nails = new PVector[numNails];
    for (int i = 0; i < numNails; i++) {
      float angle = map(i, 0, numNails, 0, TWO_PI);
      float x = radius + cos(angle) * r;
      float y = radius + sin(angle) * r;
      nails[i] = new PVector(x, y);
    }
  }
  
  private int getRed(PImage img,int x,int y) {
    color pixelColor = img.get(x, y);
    return (pixelColor >> 16) & 0xFF; // Extract the red channel
  }
  
  private int pixelError(int x,int y) {
    return pixelError_intensity(x,y);
    //return pixelError_diff(x,y);
    //return pixelError_redTanH(x,y);
    //return pixelError_fromCenter(x,y);
  }
  
  private int pixelError_intensity(int x,int y) {
    return getRed(croppedImg,x,y);
  }
  
  private int pixelError_redTanH(int x,int y) {
    float v = (float)getRed(croppedImg,x,y)/255.0;  // 0..1
    v = (float)Math.tanh(v-0.5f)+0.5f;
    return (int)(v*255f);
  }
  
  private int pixelError_diff(int x,int y) {
    int a = getRed(croppedImg, x, y);
    int b = intensity;
    return max(0,a-b);
  }
  
  private int pixelError_fromCenter(int x,int y) {
    float h2 = height/2;
    return (int)( (float)pixelError_intensity(x,y)
                * (3.0f-(dist(x,y,h2,h2)/h2)) );
  }
  
  /**
   * Uses Bresenham's line algorithm to move over the image faster.
   */
  private float lineError(PVector nail1, PVector nail2) {
    int error = 0;
  
    int x0 = int(nail1.x);
    int y0 = int(nail1.y);
    int x1 = int(nail2.x);
    int y1 = int(nail2.y);
  
    int dx = abs(x1 - x0);
    int dy = abs(y1 - y0);
  
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
  
    int err = dx - dy;
    int count = 0;
    
    while (true) {
      // Ensure the coordinates are within the image bounds
      //if (x0 >= 0 && x0 < croppedImg.width && y0 >= 0 && y0 < croppedImg.height) {
        error += pixelError(x0,y0);
        ++count;
      //}
  
      if (x0 == x1 && y0 == y1) {
        break;
      }
  
      int e2 = 2 * err;
  
      if (e2 > -dy) {
        err -= dy;
        x0 += sx;
      }
  
      if (e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
    if(count<minimumLineLength) return 0;
    
    return error/(float)count;
  }

  
  public int findBestStartingNail() {
    float maxLineError = -1;
    int best = 0;
  
    for (int i = 0; i < numNails; i++) {
      NailPathValue result = findBestFitChannelFromNail(i);
      if(result.value > maxLineError) {
        maxLineError = result.value;
        best = i;
      }
    }
  
    return best;
  }
  
  private NailPathValue findBestFitChannelFromNail(int i) {
    float maxLineError = -1;
    int bestNail = -1;
    
    for (int j = i + 1; j < numNails; j++) {
      float currentLineError = lineError(nails[i], nails[j]);
      
      if (currentLineError > maxLineError) {
        maxLineError = currentLineError;
        bestNail = j;
      }
    }
  
    return new NailPathValue(bestNail,maxLineError);
  }
  
  private void subtractIntensityBetweenNails(PVector nail1, PVector nail2) {
    croppedImg.blendMode(SUBTRACT);
    croppedImg.beginDraw();
    croppedImg.smooth(mySmooth);
    croppedImg.stroke(intensity);
    croppedImg.strokeWeight(myStrokeWeight);
    croppedImg.line(nail1.x,nail1.y,nail2.x,nail2.y);
    croppedImg.endDraw();
  }
  
  private String makePathKey(int currentNailIndex,int nextNailIndex) {
    return currentNailIndex < nextNailIndex ? currentNailIndex + "-" + nextNailIndex : nextNailIndex + "-" + currentNailIndex;
  }

  /**
   * step-by-step version of buildNailPath
   */
  void iterate() {
    if(croppedImg==null) return;
    if(paused) return;
    
    if(nailPath.size() < numNails * (numNails - 1) / 2) { // Maximum number of unique paths
      findStep();
    }
  }
  
  private int findStep() {
    int nextNailIndex = -1;
    float maxLineError = -1;

    for (int i = 0; i < numNails; i++) {
      if (i == currentNailIndex) continue;

      String pathKey = makePathKey(currentNailIndex,i);
      if (visitedPaths.contains(pathKey)) continue;

      float currentLineError = lineError(nails[currentNailIndex], nails[i]);
      if (currentLineError > maxLineError) {
        maxLineError = currentLineError;
        nextNailIndex = i;
      }
    }

    if(nextNailIndex == -1) {
      paused=true;
      return -1; // No more valid paths found
    }

    float len = getLength(currentNailIndex,nextNailIndex);
    if(maxLineError < len * minimumErrorLimit) {
      paused=true;
      return -1;  // makes image worse, not better.
    }

    subtractIntensityBetweenNails(nails[currentNailIndex], nails[nextNailIndex]);
    nailPath.add(nextNailIndex);
    drawAddedPath(nextNailIndex);

    String pathKey = makePathKey(currentNailIndex,nextNailIndex);
    visitedPaths.add(pathKey);

    currentNailIndex = nextNailIndex;
    return nextNailIndex;
  }
  
  float getLength(int a,int b) {
    return dist(nails[a].x,nails[a].y,nails[b].x,nails[b].y);
  }
}
