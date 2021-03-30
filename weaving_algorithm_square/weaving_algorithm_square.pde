//------------------------------------------------------
// square weaving algorithm
// dan@marginallyclever.com 2016-09-28
// based on work by Petros Vrellis (http://artof01.com/vrellis/works/knit.html)
//------------------------------------------------------

int pointsWide=40;
int pointsHigh=60;
int numberOfPoints = pointsWide * pointsHigh;

// self-documenting
int numberOfLinesToDrawPerFrame = 1;
// self-documenting
int totalLinesToDraw=3000;
// how dark is the string being added.  1...255 smaller is lighter.
int stringAlpha = 65;
float stringThickness=1.5;
// ignore N nearest neighbors to this starting point
int skipNeighbors=10;
// set true to start paused.  click the mouse in the screen to pause/unpause.
boolean paused=true;
// make this true to add one line per mouse click.
boolean singleStep=false;

//------------------------------------------------------
// convenience
color white = color(255,255,255);
color black = color(0,0,0);
color blue = color(0,0,255);
color green = color(0,255,0);


//------------------------------------------------------
int numLines = numberOfPoints * numberOfPoints / 2;
float [] intensities = new float[numberOfPoints];
double [] px = new double[numberOfPoints];
double [] py = new double[numberOfPoints];
PImage img;

int totalLinesDrawn=0;
int currentPoint=0;
int previewPoint=0;


//------------------------------------------------------
/**
 * To modify this example for another image, you will have to MANUALLY
 * tweak the size() values to match the img.width and img.height.
 * Don't like it?  Tell the Processing team. 
 */
void setup() {
  // the name of the image to load
  img = loadImage("2016-05-08 greek woman.JPG");
  // the size of the screen is img.width*2, img.height 
  size(1336,1000);
  
  // smash the image to grayscale
  img.filter(GRAY);
  
  int half = numberOfPoints/2;
  
  for(int x=0;x<pointsWide;++x) {
    // top
    px[     x] = img.width*(x/pointsWide);
    py[     x] = 0;
    // bottom
    px[half+x] = img.width - 1 - px[x];
    py[half+x] = img.height;
  }
  
  for(int y=0;y<pointsHigh;++y) {
    // right
    int y2 = pointsWide+y;
    px[y2] = img.width-1;
    py[y2] = img.height * (y/pointsHigh);
    // left
    px[half+y2] = 0;
    py[half+y2] = img.height-1 - py[y2];
  }
}


//------------------------------------------------------
void mouseReleased() {
  paused = paused ? false : true;  
}


//------------------------------------------------------
void draw() {
  if( previewPoint < numberOfPoints ) {
    previewPointOrder();
  } else {
    updateLines();
  }
}


//------------------------------------------------------
// display the points in order before starting the process.
void previewPointOrder() {
  double x = px[previewPoint];
  double y = py[previewPoint];
  
  //clear();
  stroke(green);
  strokeWeight(10);
  point((float)x,(float)y);
  strokeWeight(1);
  
  delay(20);
  
  previewPoint++;
}


//------------------------------------------------------
void updateLines() {
  // if we aren't done
  if(totalLinesDrawn<totalLinesToDraw) {
    if(!paused) {
      // draw a few at a time so it looks interactive.
      int i;
      for(i=0;i<numberOfLinesToDrawPerFrame;++i) {
        drawLine();
        totalLinesDrawn++;
      }
      if(singleStep) paused=true;
    }
    image(img,width/2,0);
  }
  // progress bar
  float percent = (float)totalLinesDrawn / (float)totalLinesToDraw;
  
  strokeWeight(10);  // thick
  stroke(blue);
  line(10,5,(width-10),5);
  stroke(green);
  line(10,5,(width-10)*percent,5);
  strokeWeight(1);  // default
}


//------------------------------------------------------
/**
 * find the darkest line on the image between two points
 * subtract that line from the source image
 * add that line to the output.
 */
void drawLine() {
  int i,j,k;
  double maxValue = 1000000;
  int maxA = 0;
  int maxB = 0;
  // find the darkest line in the picture
  
  // starting from the last line added
  i=currentPoint;

  println(totalLinesDrawn+" : "+currentPoint);
  
  // uncomment this line to choose from all possible lines.  much slower.
  //for(i=0;i<numberOfPoints;++i)
  {
    for(j=1+skipNeighbors;j<numberOfPoints-skipNeighbors;++j) {
      int nextPoint = ( i + j ) % numberOfPoints;
      if(nextPoint==i) continue;
      double dx = px[nextPoint] - px[i];
      double dy = py[nextPoint] - py[i];
      double len = Math.floor( Math.sqrt(dx*dx+dy*dy) );
      
      // measure how dark is the image under this line.
      double intensity = 0;
      for(k=0;k<len;++k) {
        double s = (double)k/len; 
        double fx = px[i] + dx * s;
        double fy = py[i] + dy * s;
        intensity += img.get((int)fx, (int)fy);
      }
      double currentIntensity = intensity / len;
      if( maxValue > currentIntensity ) {
        maxValue = currentIntensity;
        maxA = i;
        maxB = nextPoint;
      }
    }
  }
  
  //println("line "+maxA+ " to "+maxB);
  // maxIndex is the darkest line on the image.
  // subtract the darkest line from the source image.
  currentPoint = maxA;
  int nextPoint = maxB;
  double dx = px[nextPoint] - px[currentPoint];
  double dy = py[nextPoint] - py[currentPoint];
  double len = Math.floor( Math.sqrt(dx*dx+dy*dy) );
  for(k=0;k<len;++k) {
    double s = (double)k/len; 
    double fx = px[currentPoint] + dx * s;
    double fy = py[currentPoint] + dy * s;
    color c = img.get((int)fx, (int)fy);
    float r = red(c);
    if(r<255-stringAlpha) {
      r += stringAlpha; 
    } else {
      r = 255;
    }
    img.set((int)fx, (int)fy,color(r));
  }
  
  // draw darkest lines on screen.
  strokeWeight(stringThickness);
  stroke(0,0,0,stringAlpha);
  line((float)px[currentPoint],(float)py[currentPoint],(float)px[nextPoint],(float)py[nextPoint]);
  
  // move to the end of the line.
  currentPoint = nextPoint;
}
