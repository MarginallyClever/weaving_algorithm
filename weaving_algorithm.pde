color white = color(255,255,255);
color black = color(0,0,0);
int numberOfPoints = 200;
int numberOfLinesToDrawPerFrame = 1;
int numLines = numberOfPoints * numberOfPoints / 2;
float [] intensities = new float[numberOfPoints];
double [] px = new double[numberOfPoints];
double [] py = new double[numberOfPoints];
PImage img;
int totalLinesDrawn=0;


void setup() {
  img = loadImage("2016-05-08 greek woman.JPG");
  
  size(1336,1000);//img.width,img.height);
  
  img.filter(GRAY);
  
  double maxr;
  if( img.width > img.height ) 
       maxr = img.height/2;
  else maxr = img.width/2;

  int i;
  for(i=0;i<numberOfPoints;++i) {
    double d = Math.PI * 2.0 * (double)i/(double)numberOfPoints;
    px[i] = img.width/2 + Math.sin(d) * maxr;
    py[i] = img.height/2 + Math.cos(d) * maxr;
  }
}


void draw() {
  if(totalLinesDrawn>2000) return;
  int i;
  // go around the circle, calculating intensities
  for(i=0;i<numberOfLinesToDrawPerFrame;++i) {
    drawLine();
    totalLinesDrawn++;
  }
  
  image(img,width/2,0);
}


void drawLine() {
  int i,j,k;
  double maxValue = 1000000;
  int maxA = 0;
  int maxB = 0;
  for(i=0;i<numberOfPoints;++i) {
    for(j=1;j<numberOfPoints;++j) {
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
  int currentPoint = maxA;
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
    if(r<127) r += 127; else r = 255;
    img.set((int)fx, (int)fy,color(r));
  }
  // draw darkest lines on screen.
  stroke(0,0,0,127);
  line((float)px[currentPoint],(float)py[currentPoint],(float)px[nextPoint],(float)py[nextPoint]);
  // move to the end of the line.
  currentPoint = nextPoint;
}