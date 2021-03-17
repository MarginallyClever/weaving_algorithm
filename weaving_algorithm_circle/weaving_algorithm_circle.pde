//------------------------------------------------------
// circular weaving algorithm
// dan@marginallyclever.com 2016-08-05
// based on work by Petros Vrellis (http://artof01.com/vrellis/works/knit.html)
//------------------------------------------------------

// points around the circle
final int numberOfPoints = 200;
// self-documenting
final int numberOfLinesToDrawPerFrame = 10;
// self-documenting
final int totalLinesToDraw=6000;
// how thick are the threads?
final float lineWeight = 1.2;  // default 1
final float stringAlpha = 48; // 0...255 with 0 being totally transparent.
// ignore N nearest neighbors to this starting point
final int skipNeighbors=20;

// convenience colors.  RGBA. 
// Alpha is how dark is the string being added.  1...255 smaller is lighter.
// Messing with the alpha value seems to make a big difference!
final color white = color(255, 255, 255,stringAlpha);
final color black = color(0, 0, 0,stringAlpha);
final color blue = color(0, 0, 255,stringAlpha);
final color green = color(0, 255, 0,stringAlpha);
final color yellow = color(255, 255, 0,stringAlpha);
final color red = color(255, 0, 0,stringAlpha);

//------------------------------------------------------

// WeavingThread class tracks a thread as it is being woven around the nails.
class WeavingThread {
  // thread color (hex value)
  public color c;
  // thread color name (human readable)
  public String name;
  // last nail reached
  public int currentPoint;
  // a list of all nail pairs already visited.  I don't want the app to put many
  // identical strings on the same two nails, so WeavingThread.done[] tracks 
  // which pairs are finished.
  public char [] done;
};

// for tracking the best place to put the next weaving thread.
class BestResult {
  // nails
  public int bestStart,bestEnd;
  // score
  public double maxValue;
  
  public BestResult( int a, int b, double v) {
    bestStart=a;
    bestEnd=b;
    maxValue=v;
  }
};

// for re-drawing the end result quickly.
class FinishedLine {
  // nails
  public int start,end;
  // hex color
  public color c;
  
  public FinishedLine(int s,int e,color cc) {
    start=s;
    end=e;
    c=cc;
  }
};

//------------------------------------------------------

// finished lines in the weave.
ArrayList<FinishedLine> finishedLines = new ArrayList<FinishedLine>(); 

// threads actively being woven
ArrayList<WeavingThread> threads = new ArrayList<WeavingThread>();

// stop when totalLinesDrawn==totalLinesToDraw
int totalLinesDrawn=0;

// diameter of weave
float diameter;

// can we start weaving yet?!
boolean ready;

// set true to start paused.  click the mouse in the screen to pause/unpause.
boolean paused=false;

// make this true to pause after every frame.
boolean singleStep=false;

// nail locations
float [] px = new float[numberOfPoints];
float [] py = new float[numberOfPoints];

// distance from nail n to nail n+m
float [] lengths = new float[numberOfPoints];

// image user wants converted
PImage img;

// place to store visible progress fo weaving.
// also used for finding next best thread.
PGraphics dest; 

//PImage weights;

long startTime;

//------------------------------------------------------

// run once on start.
void setup() {
  // make the window.  must be (h*2,h+20)
  size(1600,820);

  ready=false;
  selectInput("Select an image file","inputSelected");
}


void inputSelected(File selection) {
  if(selection == null) {
    exit();
    return;
  }
  
  // load the image
  //img = loadImage("cropped.jpg");
  //img = loadImage("unnamed.jpg");
  img = loadImage(selection.getAbsolutePath());
  String wFile = selection.getAbsolutePath();
  String ext = wFile.substring(wFile.indexOf('.'));
  String name = wFile.substring(0,wFile.indexOf('.'));
  //weights = loadImage(name+" weight"+ext);
  
  // crop image to square
  if(img.height<img.width) {
    img = img.get(0,0,img.height, img.height);
  } else {
    img = img.get(0,0,img.width, img.width);
  }
  
  // resize to fill window
  img.resize(width/2,width/2);

  dest = createGraphics(width/2,width/2);
  
  setBackgroundColor();
  
  // smash the image to grayscale
  //img.filter(GRAY);

  // find the size of the circle and calculate the points around the edge.
  diameter = ( img.width > img.height ) ? img.height : img.width;
  float radius = diameter/2;

  int i;
  for (i=0; i<numberOfPoints; ++i) {
    float d = PI * 2.0 * i/(float)numberOfPoints;
    px[i] = img.width /2 + cos(d) * radius;
    py[i] = img.height/2 + sin(d) * radius;
  }

  // a lookup table because sqrt is slow.
  for (i=0; i<numberOfPoints; ++i) {
    float dx = px[i] - px[0];
    float dy = py[i] - py[0];
    lengths[i] = sqrt(dx*dx+dy*dy);
  }
  
  threads.add(startNewWeavingThread(white,"white"));
  threads.add(startNewWeavingThread(black,"black"));
  //threads.add(startNewWeavingThread(red,"red"));
  //threads.add(startNewWeavingThread(blue,"blue"));
  //threads.add(startNewWeavingThread(color(237, 180, 168),"pink"));
  
  startTime=millis();
  ready=true;
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
  float r=127,g=127,b=127;
  int size=1;
  
  dest.beginDraw();
  dest.background(
    r/(float)size,
    g/(float)size,
    b/(float)size);
  dest.endDraw();
}


// setup a new WeavingThread and place it on the best pair of nails.
WeavingThread startNewWeavingThread(color c,String name) {
  WeavingThread wt = new WeavingThread();
  wt.c=c;
  wt.name=name;
  wt.done = new char[numberOfPoints*numberOfPoints];

  // find best start
  int bestI=0, bestJ=1; 
  float bestScore = 0;
  int i,j;
  for(i=0;i<numberOfPoints;++i) {
    for(j=i+1;j<numberOfPoints;++j) {
      float score = scoreLine(i,j,wt);
      if(bestScore<score) {
        bestScore = score;
        bestI=i;
        bestJ=j;
      }
    }
  }
  
  drawLine(wt,bestI,bestJ);
  
  return wt;
}


void mouseReleased() {
  paused = paused ? false : true;
}


void draw() {
  if(!ready) return;
  
  // if we aren't done
  if (totalLinesDrawn<totalLinesToDraw) {
    if (!paused) {
      BestResult[] br = new BestResult[threads.size()];
      
      // draw a few at a time so it looks interactive.
      for(int i=0; i<numberOfLinesToDrawPerFrame; ++i) {
        // find the best thread for each color
        for(int j=0;j<threads.size();++j) {
          br[j]=findBest(threads.get(j));
        }
        // of the threads tested, which is best?
        double v = br[0].maxValue;
        int best = 0;
        for(int j=1;j<threads.size();++j) {
          if( v > br[j].maxValue ) {
            v = br[j].maxValue;
            best = j;
          }
        }
        // draw that best line.
        drawLine(threads.get(best),br[best].bestStart,br[best].bestEnd);
      }
      if (singleStep) paused=true;
    }
  } else {
    // finished!    
    calculationFinished();
  }
  
  image(img, width/2, 0,width/2,height);
  image(dest, 0, 0, width/2, height);
  drawProgressBar();
}

void drawProgressBar() {
  float percent = (float)totalLinesDrawn / (float)totalLinesToDraw;

  strokeWeight(10);  // thick
  stroke(0,0,255,255);
  line(10, 5, (width-10), 5);
  stroke(0,255,0,255);
  line(10, 5, (width-10)*percent, 5);
}


// stop drawing and ask user where (if) to save CSV.
void calculationFinished() {
  noLoop();
  
  long endTime=millis();
  println("Time = "+ (endTime-startTime)+"ms");
  selectOutput("Select a destination CSV file","outputSelected");
}

// write the file if requested
void outputSelected(File output) {
  if(output==null) {
    return;
  }
  // write the file
  PrintWriter writer = createWriter(output.getAbsolutePath());
  writer.println("Color, Start, End");
  for(FinishedLine f : finishedLines ) {
    
    writer.println(getThreadName(f.c)+", "
                  +f.start+", "
                  +f.end+", ");
  }
  writer.close();
}


String getThreadName(color c) {
  for( WeavingThread w : threads ) {
    if(w.c == c) {
      return w.name;
    }
  }
  return "??";
}


// a weaving thread starts at wt.currentPoint.  for all other points Pn, look at the line 
// between here and all other points Ln(Pn).  
// The Ln with the lowest score is the best fit.
BestResult findBest(WeavingThread wt) {
  int i, j;
  double maxValue = Double.MAX_VALUE;
  int bestStart = 0;
  int bestEnd = 0;

  // starting from the last line added
  i=wt.currentPoint;

  //for(i=wt.currentPoint-2;i<wt.currentPoint+2;++i)
  // uncomment this line to compare all starting points, not just the current starting point.  O(n*n) slower.
  //for(i=0;i<numberOfPoints;++i)
  {
    // start, made safe in case we're doing the all-nails-to-all-nails test.
    int iSafe = (i+numberOfPoints)%numberOfPoints;
    
    // the range of ending nails cannot include skipNeighbors.
    int end0 = iSafe+1+skipNeighbors;
    int end1 = iSafe+numberOfPoints-skipNeighbors;
    for (j=end0; j<end1; ++j) {
      int nextPoint = j % numberOfPoints;
      if(isDone(wt,iSafe,nextPoint)) {
        continue;
      }
      double score = scoreLine(iSafe,nextPoint,wt);
      if ( maxValue > score ) {
        maxValue = score;
        bestStart = iSafe;
        bestEnd = nextPoint;
      }
    }
  }
  
  return new BestResult( bestStart, bestEnd, maxValue );
}


// commit the new line to the destination image (our results so far)
// also remember the details for later.
void drawLine(WeavingThread wt,int a,int b) {
  //println(totalLinesDrawn+" : "+wt.name+"\t"+bestStart+"\t"+bestEnd+"\t"+maxValue);
  
  drawToDest(a, b, wt.c);
  setDone(wt,a,b);
  totalLinesDrawn++;
  // move to the end of the line.
  wt.currentPoint = b;
}


// draw thread on screen.
void drawToDest(int start, int end, color c) {
  dest.beginDraw();
  dest.stroke(c);
  dest.strokeWeight(lineWeight);
  dest.line((float)px[start], (float)py[start], (float)px[end], (float)py[end]);
  dest.endDraw();
  
  finishedLines.add(new FinishedLine(start,end,c));
}


void setDone(WeavingThread wt,int a,int b) {
  if(b<a) {
    int c=b;
    b=a;
    a=c;
  }
  int index = a*numberOfPoints+b;
  wt.done[index]=1;
}


boolean isDone(WeavingThread wt,int a,int b) {
  if(b<a) {
    int c=b;
    b=a;
    a=c;
  }
  int index = a*numberOfPoints+b;
  return wt.done[index]!=0;
}


/**
 * Measure the change if thread wt were put here.
 * A line begins at point S and goes to point E.  The difference D=E-S.
 * I want to test at all points i of N along the line, or pN = S + (D*i/N).  
 * (i/N will always be 0...1)
 *
 * There is score A, the result so far: the difference between the original 
 * and the latest image.  A perfect match would be zero.  It is never a negative value.
 * There is score B, the result if latest were changed by the new thread.
 * We are looking for the score that improves the drawing the most.
 */
float scoreLine(int startPoint,int endPoint,WeavingThread wt) {
  // S
  float sx=px[startPoint];
  float sy=py[startPoint];
  // D
  float dx = px[endPoint] - sx;
  float dy = py[endPoint] - sy;
  // N
  float N = lengths[(int)abs(endPoint-startPoint)];
  
  float center=height/2;

  color cc = wt.c;
  float ccAlpha = (alpha(cc)/255.0);
  //println(ccAlpha);
  
  float errorBefore=0;
  float errorAfter=0;

  for(float i=0; i<N; i++) {
    float iN = i/N; 
    int px = (int)(sx + dx * iN);
    int py = (int)(sy + dy * iN);
    
    // color of original picture
    color original = img.get(px,py);
    // color of weave so far
    color current = dest.get(px,py);
    // color of weave if changed by the thread in question.
    color newest = lerpColor(current,cc,ccAlpha);
    
    // how wrong is dest?
    float oldError = scoreColors(original,current);
    // how wrong will dest be with the new thread?
    float newError = scoreColors(original,newest );

    // distance from center of circle.
    float cx = px-center;
    float cy = py-center;
    // square of distance
    float cd = cx*cx + cy*cy;
    // make pixels in the center more important than pixels on the edge.
    float r = 1.0 / (1.0 + cd);

    // experiments with weight maps.
    float m = 1.0;//-((red(weights.get(fx,fy))/255.0));
    
    errorBefore += oldError*r*m;
    errorAfter  += newError*r*m;
  }
  
  // if errorAfter is less than errorBefore, result will be <0.
  // if error is identical, number will be 0.
  // if error is worse, result will be >0
  return (errorAfter-errorBefore);//*diameter/N;
}

// the square of the linear distance between two colors in RGB space.
float scoreColors(color c0,color c1) {
  float r = red(  c0)-red(  c1);
  float g = green(c0)-green(c1);
  float b = blue( c0)-blue( c1);
  return (r*r + g*g + b*b);
}
