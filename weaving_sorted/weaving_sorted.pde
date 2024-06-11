import processing.svg.*;
import java.util.*; 

// A class to store thread color information
class ThreadColor {
  PVector start;
  PVector end;
  color c;
  ArrayList<PVector> relevantIntersections = new ArrayList<PVector>();
  
  ThreadColor(PVector start, PVector end, color c) {
    this.start = start;
    this.end = end;
    this.c = c;
  }
}

int numNails = 60; // Number of nails
int skipCount = 5;
  // Example colors
color[] allowedColors = new color[]{
  color(  0,   0,   0),  // black
  color(255, 255, 255),  // white
  color(255,   0,   0),  // red
  color(  0, 255,   0),  // green 
  color(  0,   0, 255),  // blue
  color(  0, 255, 255),  // cyan
  color(255,   0, 255),  // magenta 
  color(255, 255,   0)   // yellow
  
};

PImage backgroundImage;
PImage quantizedImage;
PGraphics dest;
PVector[] nails;
boolean ready=false;

// simulated annealing
float initialTemp = 100;
float coolingRate = 0.99;
int numIterations = 5;

ArrayList<ThreadColor> threadList = new ArrayList<ThreadColor>();
HashMap<PVector, ArrayList<ThreadColor>> intersectionMap = new HashMap<PVector, ArrayList<ThreadColor>>();

PVector intersectionPoint;
color intersectionColor;
boolean intersectionFound = false;
ArrayList<PVector> allIntersections;


Octree tree = new Octree();


void setup() {
  size(800, 820);
  selectInput("Select an image file:", "fileSelected");
}

void fileSelected(File selection) {
  if (selection == null) {
    println("Window was closed or the user hit cancel.");
  } else {
    println("User selected " + selection.getAbsolutePath());
    backgroundImage = loadImage(selection.getAbsolutePath());
    if (backgroundImage != null) {
      setupNails();
      setupImage();
      processThreads();
      allIntersections = findAllIntersections(threadList);
      calculateIntersections(threadList);
      ready=true;
    } else {
      println("Error loading image.");
    }
  }
}

void calculateIntersections(ArrayList<ThreadColor> threads) {
  int size = threads.size();
  for (int i = 0; i < size; i++) {
    ThreadColor t1 = threads.get(i);
    //println(i+" / "+size);
    for (int j = i + 1; j < size; j++) {
      ThreadColor t2 = threads.get(j);
      PVector intersection = findIntersection(t1, t2);
      if (intersection != null) {
        t1.relevantIntersections.add(intersection);
        t2.relevantIntersections.add(intersection);
        
        if (!intersectionMap.containsKey(intersection)) {
          intersectionMap.put(intersection, new ArrayList<ThreadColor>());
        }
        intersectionMap.get(intersection).add(t1);
        intersectionMap.get(intersection).add(t2);
      }
    }
  }
}

void setupImage() {
  if (backgroundImage.width != backgroundImage.height) {
    int minSize = min(backgroundImage.width, backgroundImage.height);
    backgroundImage = backgroundImage.get(0, 0, minSize, minSize);
  }
  backgroundImage.resize(width, width); // Resize to fit the square portion of the window
  backgroundImage.loadPixels();
  
  dest = createGraphics(width, width);
  if(dest==null) {
    println("dest failed to create("+width+"x"+width+")");
    exit();
  }
  
  // make a quantized image to select the thread colors.
  quantizedImage = backgroundImage.copy();
  // mask the quantized image to only the relevant pixels.
  buildMask(quantizedImage);
  // build the quantized palette.
  tree.buildQuantizedPalette(quantizedImage,5);
  //allowedColors = tree.palette;
}


void buildMask(PImage quantizedImage) {
  PGraphics mask = createGraphics(width,width);
  // Draw threads in sorted order
  mask.beginDraw();
  mask.background(0);
  mask.stroke(255);
  for(int i=0;i<numNails;++i) {
    for(int j=0;j<numNails;++j) {
      mask.line(
        nails[i].x,nails[i].y,
        nails[j].x,nails[j].y
      );
    }
  }
  mask.endDraw();
  
  quantizedImage.mask(mask);
}


void setupNails() {
  nails = new PVector[numNails];
  float radius = width / 2.0;
  float centerX = width / 2.0;
  float centerY = width / 2.0;

  for (int i = 0; i < numNails; i++) {
    float angle = TWO_PI * i / numNails;
    float x = centerX + cos(angle) * radius;
    float y = centerY + sin(angle) * radius;
    nails[i] = new PVector(x, y);
  }
  println("nails="+numNails);
  int threadCount = numNails * (numNails-1)/2;
  println("thread count 1="+threadCount);
}

void draw() {
  if(!ready) return;

  iterativeThreadSort();

  updateDest(threadList);
  image(dest, 0, 0, width, width); // Display the graphics buffer
  
  drawAllNails();
  //noLoop(); // Stop draw loop since we only need to draw once
}

// Draw nails
void drawAllNails() {
  if(nails == null) return;
  fill(255);
  for(PVector nail : nails) {
    ellipse(nail.x, nail.y, 3, 3);
  }
}

void processThreads() {
  // Calculate best color for each thread
  for (int i = 0; i < numNails; i++) {
    for (int j = i + 1 + skipCount; j < numNails; j++) {
      PVector start = nails[i];
      PVector end = nails[j];
      findBestThreadColor(start, end);
    }
  }
  println("thread count 2="+threadList.size());
}

// We have the best color for every thread. 
// Sort threads based on intersection and color criteria
void iterativeThreadSort() {
  ArrayList<ThreadColor> sortedThreads = sortThreads(threadList);
  //println("sort done. "+sortedThreads.size());
  threadList = sortedThreads;
}

void updateDest(ArrayList<ThreadColor> order) {
  dest.beginDraw();
  dest.background(255); // Clear the buffer
  for (ThreadColor t : order) {
    dest.stroke(t.c);
    dest.line(
      t.start.x, t.start.y,
      t.end.x, t.end.y
    );
  }
  dest.endDraw();
}

float costFunction(ArrayList<ThreadColor> order, ArrayList<PVector> intersections) {
  updateDest(order);
  dest.loadPixels();
  
  float cost = 0;
  for (PVector p : intersections) {
    int x = (int)p.x;
    int y = (int)p.y;
    if (x >= 0 && x < width && y >= 0 && y < width) {
      int i = x + y * width;
      cost += colorDifference( dest.pixels[i], backgroundImage.pixels[i] );
    }
  }  
  return cost;
}

color getTopColorAtIntersection(ArrayList<ThreadColor> order, PVector p) {
  for (int i = order.size() - 1; i >= 0; i--) {
    ThreadColor t = order.get(i);
    if (isPointOnLine(t, p)) {
      return t.c;
    }
  }
  return color(255); // Default to white if no color found
}

boolean isPointOnLine(ThreadColor t, PVector p) {
  float d1 = dist(p.x, p.y, t.start.x, t.start.y);
  float d2 = dist(p.x, p.y, t.end.x, t.end.y);
  float lineLen = dist(t.start.x, t.start.y, t.end.x, t.end.y);
  float buffer = 0.1;
  return (d1 + d2 >= lineLen - buffer && d1 + d2 <= lineLen + buffer);
}

ArrayList<ThreadColor> swapIntersectingThreads(ArrayList<ThreadColor> order,int index,int jndex) {  
  // Swap the two threads
  ThreadColor temp = order.get(index);
  order.set(index, order.get(jndex));
  order.set(jndex, temp);
  
  return order;
}

ArrayList<ThreadColor> sortThreads(ArrayList<ThreadColor> threads) {
  ArrayList<ThreadColor> currentOrder = new ArrayList<ThreadColor>(threads);
  float temperature = initialTemp;
  
  int size = currentOrder.size();
  boolean first=true;
  float currentCost=Float.MAX_VALUE;
  
  for (int iter = 0; iter < numIterations; iter++) {
    int index = int(random(size));
    int jndex = int(random(size));
    ArrayList<ThreadColor> newOrder = swapIntersectingThreads(new ArrayList<ThreadColor>(currentOrder),index,jndex);
    
    ArrayList<PVector> relevantIntersections = findRelevantIntersections(newOrder, allIntersections, index, jndex);
    if(first) {
      first=false;
      currentCost = costFunction(currentOrder, relevantIntersections);
    }
    float newCost = costFunction(newOrder, relevantIntersections);
    
    if (newCost < currentCost || random(1) < exp((currentCost - newCost) / temperature)) {
      currentOrder = newOrder;
      currentCost = newCost;
      println("new cost="+(currentCost - newCost));
    }
    
    temperature *= coolingRate;
    if (temperature < 1e-10) break;
  }
  return currentOrder;
}

ArrayList<PVector> findRelevantIntersections(ArrayList<ThreadColor> order, ArrayList<PVector> intersections, int index1, int index2) {
  HashSet<PVector> relevantIntersections = new HashSet<PVector>();
  relevantIntersections.addAll(order.get(index1).relevantIntersections);
  relevantIntersections.addAll(order.get(index2).relevantIntersections);
  return new ArrayList<PVector>(relevantIntersections);
}

String intersectionKey(PVector intersection) {
  return String.format("%.5f,%.5f", intersection.x, intersection.y);
}

int betterColorAtIntersection(int c1, int c2, PVector intersection, Integer currentBest) {
  color backgroundColor = backgroundImage.get((int)intersection.x, (int)intersection.y);
  float score1 = colorDifference(backgroundColor,c1);
  float bestScore = Float.MAX_VALUE;
  // if currentBest is null or c1 is better, c1 wins.
  boolean win = (currentBest==null);
  if(!win) {
    bestScore = colorDifference(backgroundColor, currentBest);
    win = (score1<bestScore);
  }
  if(win) {      
    currentBest = c1;
    bestScore = score1;
  }
  // if c2 is better than best, c2 wins
  float score2 = colorDifference(backgroundColor,c2);
  if(score2<bestScore) {
    currentBest = c2;
    bestScore = score2;
  }
  return currentBest;
}


void findBestThreadColor(PVector start, PVector end) {
  float bestDifference = Float.MAX_VALUE;
  color bestColor = color(255);
  
  for (color threadColor : allowedColors) {
    float difference = calculateColorDifference(start, end, threadColor);
    if (difference < bestDifference) {
      bestDifference = difference;
      bestColor = threadColor;
    }
  }
  
  threadList.add(new ThreadColor(start,end,bestColor));
}

float calculateColorDifference(PVector start, PVector end, color threadColor) {
  float totalDifference = 0;
  
  int x0 = int(start.x);
  int y0 = int(start.y);
  int x1 = int(end.x);
  int y1 = int(end.y);
  
  int dx = abs(x1 - x0);
  int dy = abs(y1 - y0);
  int sx = x0 < x1 ? 1 : -1;
  int sy = y0 < y1 ? 1 : -1;
  int err = dx - dy;
  
  while (true) {
    if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < width) {
      color backgroundColor = backgroundImage.get(x0, y0);
      totalDifference += colorDifference(backgroundColor, threadColor);
    }
    if (x0 == x1 && y0 == y1) break;
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
  
  return totalDifference;
}

float colorDifference(color c1, color c2) {
  float r1 = red(c1)-red(c2);
  float g1 = green(c1)-green(c2);
  float b1 = blue(c1)-blue(c2);
    
  //return r1*r1+ g1*g1+ b1*b1;
  return sqrt(r1*r1+ g1*g1+ b1*b1);
}
