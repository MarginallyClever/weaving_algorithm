import java.util.Collections;
import java.util.Comparator;


class WeavingThread {
  // nails
  int from, to;
  // color
  color c;
  // next stack position
  double velocity;
  double index;
  
  public WeavingThread() {
  }
  
  public WeavingThread(int from,int to,color c,int velocity,int index) {
    this.from = from;
    this.to = to;
    this.c = c;
    this.velocity = velocity;
    this.index = index;
  }
};


enum WeaveShape {
  CIRCLE,
  RECTANGLE,
};


// points around the circle
final int numberOfPoints = 50;

// how thick are the threads?
final float lineWeight = 1.2;  // mm.  Default 1
// how transparent are the threads?
final float stringAlpha = 255; // 0...255 with 0 being totally transparent.

// convenience colors.  RGBA. 
// Alpha is how dark is the string being added.  1...255 smaller is lighter.
// Messing with the alpha value seems to make a big difference!
final color white   = color(255, 255, 255,stringAlpha);
final color black   = color(  0,   0,   0,stringAlpha);
final color cyan    = color(  0, 255, 255,stringAlpha);
final color magenta = color(255,   0,   0,stringAlpha);
final color yellow  = color(255, 255,   0,stringAlpha);
final color red     = color(255,   0,   0,stringAlpha);
final color green   = color(  0, 255,   0,stringAlpha);
final color blue    = color(  0,   0, 255,stringAlpha);
final color brown   = color(150,  75,   0,stringAlpha);


// threads actively being woven
ArrayList<WeavingThread> threads = new ArrayList<WeavingThread>();
ArrayList<WeavingThread> testCase = new ArrayList<WeavingThread>();

// colors in use in this weave
color [] colors;

// nail locations
float [] px = new float[numberOfPoints];
float [] py = new float[numberOfPoints];

// distance from nail n to nail n+m
float [] lengths = new float[numberOfPoints];

// diameter of weave
float diameter;

boolean ready = false;
// stores the user's image
PImage img;
// place to store visible progress of weaving.
PGraphics dest; 


int samplingDistance = 10;
float learningRate = 0.001;


void setup() {
  // set window size
  size(580,580);
  // set thread colors
  colors = new color[]{white,black};
  // set border shape.
  setupNailPositions(WeaveShape.CIRCLE);
  
  println("requesting image");
  selectInput("Select an image file","inputSelected");
}


void inputSelected(File selection) {
  if(selection == null) {
    println("No file selected.  Quitting.");
    exit();
    return;
  }
  
  img = loadImage(selection.getAbsolutePath());
  if(img == null) {
    println("failed to load image.  Quitting.");
    exit();
    return;
  }
  
  dest = createGraphics(img.width,img.height);
  if(dest == null) {
    println("Failed to create dest buffer.  Quitting.");
    exit();
    return;
  }
  
  img.loadPixels();
  
  fillWeaveStack();
  ready = true;
}


void fillWeaveStack() {
  println("fillWeaveStack");
  // from every nail to every other nail, one of each color
  int sum=0;
  for(int i=0;i<numberOfPoints;++i) {
    for(int j=i+1;j<numberOfPoints;++j) {
      for(int k=0;k<colors.length;++k) {
        int velocity = (int)random(-samplingDistance,samplingDistance);
        WeavingThread t = new WeavingThread(i,j,colors[k],velocity,sum++); 
        threads.add(t);
      }
    }
  }
}


void setupNailPositions(WeaveShape shape) {
  println("setupNailPositions");
  if(shape==WeaveShape.CIRCLE) {
    setupNailPositionsInACircle();
  } else {
    setupNailPositionsInARectangleClockwise();
  }
  println("setupNailPositions done");
}


void setupNailPositionsInARectangleClockwise() {
  println("Square design");
  float borderLength = width*2 + height*2;
  float betweenNails = borderLength / numberOfPoints;
  float half = betweenNails/2;
  
  int i=0;
  // top
  float y=1;
  float x;
  for(x=half; x<width; x+=betweenNails) {
    px[i]=x;
    py[i]=y;
    ++i;
  }
  // right
  x = width-1;
  for(y=half; y<height; y+=betweenNails) {
    px[i]=x;
    py[i]=y;
    ++i;
  }
  // bottom
  y = height-1;
  for(x=width-half; x>0; x-=betweenNails) {
    px[i]=x;
    py[i]=y;
    ++i;
  }
  // left
  x = 1;
  for(y=height-half; y>0; y-=betweenNails) {
    px[i]=x;
    py[i]=y;
    ++i;
  }
}


void setupNailPositionsInACircle() {
  println("Circle design");
  // find the size of the circle and calculate the points around the edge.
  diameter = min( width, height );
  float radius = (diameter/2)-1;
  
  for(int i=0; i<numberOfPoints; ++i) {
    float d = PI * 2.0 * i/(float)numberOfPoints;
    px[i] = width /2 + cos(d) * radius;
    py[i] = height/2 + sin(d) * radius;
  }
}


void draw() {
  if(!ready) return;
  //image(img,0,0);
  
  gradientDescent();
  
  image(dest,0,0);
}


boolean hasOldScore = false;
float oldScore, newScore;


void gradientDescent() {
  println("gradientDescent");
  if(!hasOldScore) {
    // Initialize the weights with random values and calculate Error (SSE)
    oldScore = scoreStack(threads);
    applyVelocity();
    hasOldScore=true;
  }
  newScore = scoreStack(threads);
  float dScore = abs(newScore-oldScore);
  oldScore = newScore;
  println(newScore+"\t"+oldScore+"\t"+dScore);
  if(0.1>dScore) {
    println("Done.");
    noLoop();
    return;
  }

  adjustVelocities(dScore);
  applyVelocity();
}


// sorts an ArrayList of WeavingThread by their 'index' value.
static final Comparator<WeavingThread> sortTool = new Comparator<WeavingThread>() {
    // Sorting in ascending order of index
    @Override
    public int compare(WeavingThread a, WeavingThread b) {
        return Double.compare(a.index,b.index);
    }
};


void adjustVelocities(float dScore) {
  if(dScore<0) return;
  
  for(WeavingThread t : threads) {
    t.velocity = -t.velocity*0.5;
  }
}


void applyVelocity() {
  for(WeavingThread t : threads) {
    t.index += t.velocity *0.1;
  }
  
  Collections.sort(threads,sortTool);
}


// SSE 
float sumOfSquaredError(float y,float yPred) {
  float yd = y-yPred;
  return (yd*yd)*0.5;
}


float scoreStack(ArrayList<WeavingThread> set) {
  drawStack(set);  
  dest.loadPixels();
  float score = 0;
  int size = width*height;
  for(int i=0;i<size;++i) {
    score += diff(img.pixels[i],dest.pixels[i]);
  }
  return score;
}


int diff(color p0,color p1) {
  int r0 = ( p0 >> 16 ) & 0xff;
  int g0 = ( p0 >> 8  ) & 0xff;
  int b0 = ( p0       ) & 0xff;

  int r1 = ( p1 >> 16 ) & 0xff;
  int g1 = ( p1 >> 8  ) & 0xff;
  int b1 = ( p1       ) & 0xff;
  
  return abs(r1-r0) + abs(g1-g0) + abs(b1-b0);
}


void drawStack(ArrayList<WeavingThread> set) {  
  dest.beginDraw();
  dest.background(white);
  dest.strokeWeight(lineWeight);
  for(WeavingThread t : set) {
    dest.stroke(t.c);
    dest.line(px[t.from], py[t.from], px[t.to], py[t.to]);
  }
  dest.endDraw();
}
