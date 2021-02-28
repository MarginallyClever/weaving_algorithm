//------------------------------------------------------
// circular weaving algorithm
// dan@marginallyclever.com 2016-08-05
// based on work by Petros Vrellis (http://artof01.com/vrellis/works/knit.html)
//------------------------------------------------------

String sourceImage = "david1300.jpg";

// points around the circle
int numberOfPoints = 188*2;
// self-documenting
int numberOfLinesToDrawPerFrame = 50;
// self-documenting
int totalLinesToDraw=5000;//numberOfPoints*numberOfPoints/2;

float lineWeight = 0.50; /// default 1

// ignore N nearest neighbors to this starting point
int skipNeighbors=50;
// set true to start paused.  click the mouse in the screen to pause/unpause.
boolean paused=true;
// make this true to add one line per mouse click.
boolean singleStep=false;

//------------------------------------------------------
// convenience colors.  RGBA. Alpha is how dark is the string being added.  1...255 smaller is lighter.
color white = color(255, 255, 255,64);
color black = color(0, 0, 0,48);
color blue = color(0, 0, 255,48);
color green = color(0, 255, 0,48);


//------------------------------------------------------
float [] px = new float[numberOfPoints];
float [] py = new float[numberOfPoints];
float [] lengths = new float[numberOfPoints];
PImage img;
PGraphics dest; 


class WeavingThread {
  public color c;
  public int currentPoint;
  public String name;
  public char [] done;
};


class BestResult {
  public int maxA,maxB;
  public double maxValue;
  
  public BestResult( int a, int b, double v) {
    maxA=a;
    maxB=b;
    maxValue=v;
  }
};

class FinishedLine {
  public int start,end;
  public color c;
  
  public FinishedLine(int s,int e,color cc) {
    start=s;
    end=e;
    c=cc;
  }
};

ArrayList<FinishedLine> finishedLines = new ArrayList<FinishedLine>(); 
ArrayList<WeavingThread> lines = new ArrayList<WeavingThread>();

int totalLinesDrawn=0;


float scaleW,scaleH;


// run once on start.
void setup() {
  // make the window.  must be (h*2,h+20)
  size(1600,820);

  // load the image
  //img = loadImage("cropped.jpg");
  //img = loadImage("unnamed.jpg");
  img = loadImage(sourceImage);
  
  // crop image to square
  img = img.get(0,0,img.height, img.height);
  // resize to fill window
  img.resize(width/2,width/2);

  dest = createGraphics(img.width, img.height);

  setBackgroundColor();

  strokeWeight(lineWeight);  // default
  
  // smash the image to grayscale
  //img.filter(GRAY);

  // find the size of the circle and calculate the points around the edge.
  float maxr = ( img.width > img.height ) ? img.height/2 : img.width/2;

  int i;
  for (i=0; i<numberOfPoints; ++i) {
    float d = PI * 2.0 * i/(float)numberOfPoints;
    px[i] = img.width/2 + cos(d) * maxr;
    py[i] = img.height/2 + sin(d) * maxr;
  }

  // a lookup table because sqrt is slow.
  for (i=0; i<numberOfPoints; ++i) {
    float dx = px[i] - px[0];
    float dy = py[i] - py[0];
    lengths[i] = sqrt(dx*dx+dy*dy);
  }
  
  lines.add(addLine(white,"white"));
  lines.add(addLine(black,"black"));
  //lines.add(addLine(blue,"blue"));
  //lines.add(addLine(color(230, 211, 133),"yellow"));
}


void setBackgroundColor() {
/*
  // find average color of image
  float r=0,g=0,b=0;
  int size=img.width*img.height;
  int i;
  for(i=0;i<size;++i) {
    color c=img.pixels[i];
    r+=red(c);
    g+=green(c);
    b+=blue(c);
  }
  */
  // set to white
  float r=255,g=255,b=255;
  int size=1;
  
  dest.beginDraw();
  dest.background(
    r/(float)size,
    g/(float)size,
    b/(float)size);
  dest.endDraw();
}

WeavingThread addLine(color c,String name) {
  WeavingThread wt = new WeavingThread();
  wt.c=c;
  wt.name=name;
  wt.done = new char[numberOfPoints*numberOfPoints];

  // find best start
  wt.currentPoint = 0; 
  float bestScore = MAX_FLOAT;
  int i,j;
  for(i=0;i<numberOfPoints;++i) {
    for(j=i+1;j<numberOfPoints;++j) {
      float score = scoreLine(i,j,wt);
      if(bestScore>score) {
        bestScore = score;
        wt.currentPoint=i;
      }
    }
  }
  return wt;
}


//------------------------------------------------------
void mouseReleased() {
  paused = paused ? false : true;
}


//------------------------------------------------------
void draw() {
  // if we aren't done
  if (totalLinesDrawn<totalLinesToDraw) {
    if (!paused) {
      BestResult[] br = new BestResult[lines.size()];
      
      // draw a few at a time so it looks interactive.
      int i;
      for (i=0; i<numberOfLinesToDrawPerFrame; ++i) {
        for(int j=0;j<lines.size();++j) {
          br[j]=findBest(lines.get(j));
        }
        double v = br[0].maxValue;
        int best = 0;
        for(int j=1;j<lines.size();++j) {
          if( v > br[j].maxValue ) {
            v = br[j].maxValue;
            best = j;
          }
        }
        drawLine(lines.get(best),br[best].maxA,br[best].maxB,br[best].maxValue);
      }
      if (singleStep) paused=true;
    }
    image(img, width/2, 0,width/2,height);
    image(dest, 0, 0, width/2, height);
  } else {
    // finished!
    //java.util.Collections.reverse(finishedLines);
    
    /*
    float r=0;
    float g=0;
    float b=0;
    int size=img.width*img.height;
    int i;
    for(i=0;i<size;++i) {
      color c=img.pixels[i];
      r+=red(c);
      g+=green(c);
      b+=blue(c);
    }*/
    
    setBackgroundColor();
    dest.beginDraw();
    for(FinishedLine f : finishedLines ) {
      dest.stroke(f.c);
      dest.line((float)px[f.start], (float)py[f.start], (float)px[f.end], (float)py[f.end]);
    }
    dest.endDraw();
    image(img, width/2, 0,width/2,height);
    image(dest, 0, 0, width/2, height);
    noLoop();
  }
  // progress bar
  float percent = (float)totalLinesDrawn / (float)totalLinesToDraw;

  strokeWeight(10);  // thick
  stroke(blue);
  line(10, 5, (width-10), 5);
  stroke(green);
  line(10, 5, (width-10)*percent, 5);
  strokeWeight(lineWeight);  // default
}


BestResult findBest(WeavingThread wt) {
  int i, j;
  double maxValue = 1000000;
  int maxA = 0;
  int maxB = 0;
  // find the darkest line in the picture

  // starting from the last line added
  i=wt.currentPoint;

  // uncomment this line to choose from all possible lines.  much slower.
  //for(i=0;i<numberOfPoints;++i)
  {
    int i0 = i+1+skipNeighbors;
    int i1 = i+numberOfPoints-skipNeighbors;
    for (j=i0; j<i1; ++j) {
      int nextPoint = j % numberOfPoints;
      if(wt.done[i*numberOfPoints+nextPoint]>0) {
        wt.done[i*numberOfPoints+nextPoint]--;
        wt.done[nextPoint*numberOfPoints+i]--;
        continue;
      }
      double currentIntensity = scoreLine(i,nextPoint,wt);
      if ( maxValue > currentIntensity ) {
        maxValue = currentIntensity;
        maxA = i;
        maxB = nextPoint;
      }
    }
  }
  
  return new BestResult( maxA, maxB, maxValue );
}


/**
 * find the best line on the image between two points
 * subtract that line from the source image
 * add that line to the output.
 */
void drawLine(WeavingThread wt,int maxA,int maxB,double maxValue) {
  //println(totalLinesDrawn+" : "+wt.name+"\t"+maxA+"\t"+maxB+"\t"+maxValue);
  
  drawToDest(maxA, maxB, wt.c);
  wt.done[maxA*numberOfPoints+maxB]=20;
  wt.done[maxB*numberOfPoints+maxA]=20;
  totalLinesDrawn++;
  
  // move to the end of the line.
  wt.currentPoint = maxB;
}

/**
 * measure the change if thread wt were put here.  that is, the developed image - the original image (dc) vs the new thread - the original image (ic)
 */
float scoreLine(int i,int nextPoint,WeavingThread wt) {
  float dx = px[nextPoint] - px[i];
  float dy = py[nextPoint] - py[i];
  float len = lengths[(int)abs(nextPoint-i)];//Math.floor( Math.sqrt(dx*dx+dy*dy) );

  float diff0=scoreColors(img.get((int)px[i], (int)py[i]),wt.c);
  float s,fx,fy,dc,ic,diff1,change;
  
  // measure how dark is the image under this line.
  float intensity = 0;
  for(int k=0; k<len; ++k) {
    s = (float)k/len; 
    fx = px[i] + dx * s;
    fy = py[i] + dy * s;

    color original = img.get((int)fx, (int)fy);
    color latest = dest.get((int)fx, (int)fy);
    dc = scoreColors(latest,wt.c);
    ic = scoreColors(original,wt.c);
    diff1 = ic-dc;
    change=abs(diff0-ic);
    intensity += diff1 + change;  // adjust for high-contrast areas
    diff0=ic;

  }
  return intensity;///len;
}

float scoreColors(color a,color b) {
  float dr = red(a)-red(b);
  float dg = green(a)-green(b);
  float db = blue(a)-blue(b);
  return sqrt(dr*dr+dg*dg+db*db);
}

void drawToDest(int start, int end, color c) {
  // draw darkest lines on screen.
  dest.beginDraw();
  dest.stroke(c);
  dest.line((float)px[start], (float)py[start], (float)px[end], (float)py[end]);
  dest.endDraw();
  finishedLines.add(new FinishedLine(start,end,c));
}
