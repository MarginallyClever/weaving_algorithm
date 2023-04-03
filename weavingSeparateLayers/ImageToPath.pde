import java.util.HashSet;

class ImageToPath {
  int numNails;
  PVector [] nails;
  ArrayList<Integer> nailPath = new ArrayList<Integer>();
  HashSet<String> visitedPaths = new HashSet<String>();
  int currentNailIndex;
  int intensity;
  PImage croppedImg;
  PGraphics pathImg;
  boolean paused=false;
  

  ImageToPath(int numNails,int diameter) {
    this.numNails = numNails;
    createNails(diameter/2f);
    
    pathImg = createGraphics(diameter,diameter);
    pathImg.beginDraw();
    pathImg.background(0);
    pathImg.endDraw();
  }
  
  void begin(PImage image,int startNailIndex,int intensity) {
    croppedImg = image;
    this.intensity = intensity;
    nailPath.clear();
    visitedPaths.clear();
    currentNailIndex = startNailIndex;
    nailPath.add(startNailIndex);
  }
  
  void drawNails() {
    fill(255);
    stroke(255);
    // Draw the "nails" around the edge of the image
    for (PVector nail : nails) {
      ellipse(nail.x, nail.y, 1, 1);
    }
  }
  
  // draw the entire path
  void drawPath() {
    if(pathImg ==null) return;
    image(pathImg,0,0);
  }
  
  void recalculatePathImage() {    
    pathImg.beginDraw();
    pathImg.background(0);
    pathImg.noFill();
    pathImg.stroke(intensity,intensity,intensity);
    pathImg.strokeWeight(1.1);
    pathImg.blendMode(ADD);
    
    for (int i = 0; i < nailPath.size() - 1; i++) {
      int nail1Index = nailPath.get(i);
      int nail2Index = nailPath.get(i + 1);
      PVector nail1 = nails[nail1Index];
      PVector nail2 = nails[nail2Index];
  
      pathImg.line(nail1.x, nail1.y, nail2.x, nail2.y);
    }
    pathImg.endDraw();
  }
  
  void drawAddedPath() {
    int i = nailPath.size()-2;
    int nail1Index = nailPath.get(i);
    int nail2Index = nailPath.get(i + 1);
    PVector nail1 = nails[nail1Index];
    PVector nail2 = nails[nail2Index];

    pathImg.beginDraw();
    pathImg.noFill();
    pathImg.stroke(intensity,intensity,intensity);
    pathImg.blendMode(ADD);
    pathImg.line(nail1.x, nail1.y, nail2.x, nail2.y);
    pathImg.endDraw();
  }
  
  void createNails(float radius) {
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
  
  int getRed(PImage img,int x,int y) {
    color pixelColor = img.get(x, y);
    return (pixelColor >> 16) & 0xFF; // Extract the red channel
  }
  
  int pixelError(int x,int y) {
    int a = getRed(croppedImg, x, y);
    int b = intensity;//getRed(img, x, y);
    return abs(b-a);
  }
  
  @Deprecated
  int lineError1(PVector nail1, PVector nail2) {
    int sumRed = 0;
  
    float deltaX = abs(nail2.x - nail1.x);
    float deltaY = abs(nail2.y - nail1.y);
    int numSteps = int(max(deltaX, deltaY));
  
    for (int i = 0; i <= numSteps; i++) {
      float t = float(i) / float(numSteps);
      float x = lerp(nail1.x, nail2.x, t);
      float y = lerp(nail1.y, nail2.y, t);
  
      // Ensure the coordinates are within the image bounds
      if (x >= 0 && x < croppedImg.width && y >= 0 && y < croppedImg.height) {
        sumRed += pixelError(int(x), int(y));
      }
    }
  
    return sumRed;
  }
  
  int lineError(PVector nail1, PVector nail2) {
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
  
    while (true) {
      // Ensure the coordinates are within the image bounds
      if (x0 >= 0 && x0 < croppedImg.width && y0 >= 0 && y0 < croppedImg.height) {
        error += pixelError(x0,y0);
      }
  
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
  
    return error;
  }

  
  int[] findBestFitChannelPair() {
    int maxLineError = -1;
    int[] nailPair = new int[2];
  
    for (int i = 0; i < numNails; i++) {
      int [] results = findBestFitChannelFromNail(i);
      if(results[1]>maxLineError) {
        maxLineError = results[1];
        nailPair[1] = results[0];
      }
    }
  
    return nailPair;
  }
  
  int [] findBestFitChannelFromNail(int i) {
    int maxLineError = -1;
    int bestNail = -1;
    
    for (int j = i + 1; j < numNails; j++) {
      int currentLineError = lineError(nails[i], nails[j]);
      
      if (currentLineError > maxLineError) {
        maxLineError = currentLineError;
        bestNail = j;
      }
    }
  
    return new int[] {bestNail,maxLineError};
  }
  
  void subtractIntensityBetweenNails(PVector nail1, PVector nail2, int intensity) {
    int x0 = int(nail1.x);
    int y0 = int(nail1.y);
    int x1 = int(nail2.x);
    int y1 = int(nail2.y);
  
    int dx = abs(x1 - x0);
    int dy = abs(y1 - y0);
  
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
  
    int err = dx - dy;
  
    while (true) {
      // Ensure the coordinates are within the image bounds
      if (x0 >= 0 && x0 < croppedImg.width && y0 >= 0 && y0 < croppedImg.height) {
        color pixelColor = croppedImg.get(int(x0), int(y0));
        int r = max(((pixelColor >> 16) & 0xFF) - intensity, 0);
        int g = max(((pixelColor >>  8) & 0xFF) - intensity, 0);
        int b = max(((pixelColor      ) & 0xFF) - intensity, 0);
  
        // Set the modified pixel color
        croppedImg.set(int(x0), int(y0), color(r, g, b));
      }
  
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
  }
  
  int findNextNail(int startNailIndex, int intensity) {
    int maxLineError = -1;
    int nextNailIndex = -1;
  
    for (int i = 0; i < numNails; i++) {
      if (i == startNailIndex) continue;
  
      int currentLineError = lineError(nails[startNailIndex], nails[i]);
      if (currentLineError > maxLineError) {
        maxLineError = currentLineError;
        nextNailIndex = i;
      }
    }
  
    subtractIntensityBetweenNails(nails[startNailIndex], nails[nextNailIndex], intensity);
    return nextNailIndex;
  }

  ArrayList<Integer> buildNailPath(int startNailIndex, int intensity) {
    this.intensity = intensity;
    nailPath.add(startNailIndex);
  
    currentNailIndex = startNailIndex;
    while (nailPath.size() < numNails * (numNails - 1) / 2) { // Maximum number of unique paths
      if(-1==findStep()) {
        break;
      }
    }
  
    return nailPath;
  }
  
  String makePathKey(int currentNailIndex,int nextNailIndex) {
    return currentNailIndex < nextNailIndex ? currentNailIndex + "-" + nextNailIndex : nextNailIndex + "-" + currentNailIndex;
  }

  void iterate() {
    if(croppedImg==null) return;
    if(paused) return;
    
    if(nailPath.size() < numNails * (numNails - 1) / 2) { // Maximum number of unique paths
      findStep();
    }
  }
  
  int findStep() {
    int nextNailIndex = -1;
    int maxLineError = -1;

    for (int i = 0; i < numNails; i++) {
      if (i == currentNailIndex) continue;

      String pathKey = makePathKey(currentNailIndex,i);
      if (visitedPaths.contains(pathKey)) continue;

      int currentLineError = lineError(nails[currentNailIndex], nails[i]);
      if (currentLineError > maxLineError) {
        maxLineError = currentLineError;
        nextNailIndex = i;
      }
    }

    if (nextNailIndex == -1 || maxLineError < intensity) {
      return -1; // No more valid paths found
    }

    subtractIntensityBetweenNails(nails[currentNailIndex], nails[nextNailIndex], intensity);
    nailPath.add(nextNailIndex);
    drawAddedPath();

    String pathKey = makePathKey(currentNailIndex,nextNailIndex);
    visitedPaths.add(pathKey);

    currentNailIndex = nextNailIndex;
    return nextNailIndex;
  }
}
