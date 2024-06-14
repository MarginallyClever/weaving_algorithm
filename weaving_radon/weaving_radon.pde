//--------------------------------------------------------------
// using radon transform to select next-best thread
// 2024-06-11 dan@marginallyclever.com
//--------------------------------------------------------------
int numNails = 100;  // Number of nails
int bufferWidth = 800;
int bufferHeight = 800;
int alpha = 255;  // 0...255

ArrayList<Nail> nails = new ArrayList<Nail>();
ArrayList<ThreadColor> threads = new ArrayList<ThreadColor>();
PImage sourceImage;
RadonThreader radonThreader;
boolean ready = false;
int mode = 0;
boolean step = true;

void setup() {
  size(800, 800);
  selectInput("Select a source image:", "fileSelected");
}

void fileSelected(File selection) {
  if (selection == null) {
    println("No file selected.");
  } else {
    sourceImage = loadImage(selection.getAbsolutePath());
    if (sourceImage.width != sourceImage.height) {
      int minSize = min(sourceImage.width, sourceImage.height);
      sourceImage = sourceImage.get(0, 0, minSize, minSize);
    }
    sourceImage.resize(width, width); // Resize to fit the square portion of the window
    
    PImage secondImage = sourceImage.copy();
    secondImage.filter(GRAY);
    //secondImage.filter(INVERT);
    println("threader");
    radonThreader = new RadonThreader(sourceImage);
    println("nails");
    generateNails();
    println("max threads = "+(numNails * (numNails-1)/2)); 
    println("compute...");
    radonThreader.createThreads();
    println("ready");
    ready = true;
  }
}

void draw() {
  if (!ready) return;
  
  if(!step) return;
  //step = false;
  
  background(0);
  switch(mode) {
    case 1:  image(radonThreader.currentRadonImage,0,0);  break;
    case 2:  if(radonThreader.lastRadonImage != null) {
               image(radonThreader.lastRadonImage,0,0);
             }
             break;
    case 3:  image(sourceImage,0,0);  break;
    default: break;
  }
  
  drawAllThreads();
  drawAllNails();
  //drawThreadMask();
  //drawBestThetaR();
  
  radonThreader.addNextBestThread();
  if(threads.size()>500) {
    noLoop();
  }
}

void drawThetaR(int theta,int r) {
  ellipse(theta,(r+radonThreader.radius),1,1);
}


void drawBestThetaR() {
  // green dot at brightest point
  stroke(0,255,0);
  fill(0,255,0);
  drawThetaR(radonThreader.bestTheta,radonThreader.bestR);
  
  if(threads.size()>0) {
    stroke(0,255,255);
    fill(0,255,255);
    for(ThreadColor t : threads) {
      drawThetaR(t.theta,t.r);
    }
    // blue dot at chosen thread theta/r
    ThreadColor t = threads.get(threads.size()-1); 
    stroke(0,0,255);
    fill(0,0,255);
    drawThetaR(t.theta,t.r);
  }
  if(radonThreader.remainingThreads.size()>0) {
    stroke(255,255,0);
    fill(255,255,0);
    for(ThreadColor t : radonThreader.remainingThreads) {
      drawThetaR(t.theta,t.r);
    }
  }
}

void drawAllThreads() {
  for (ThreadColor t : threads) {
    t.display(g);
  }
}

void drawAllNails() {
  for (Nail n : nails) {
    n.display(g);
  }
}

void drawThreadMask() {
  PGraphics img;
  
  if(threads.size()>0) {
    img = threads.get(threads.size()-1).radonTransform;
  } else if(radonThreader.lastRadonImage!=null) {
    img = radonThreader.lastRadonImage;
  } else {
    return;
  }
  //*
  fill(255,0,0);
  stroke(255,0,0);
  for(int y=0;y<img.height;++y) {
    for(int x=0;x<img.width;++x) {
      float b = blue(img.get(x,y));
      fill(255,0,0,5*b);
      stroke(255,0,0,5*b);
      point(180+x,y);
    }
  }
  /*/
  image(180+img,0,0);
  //*/
}


void keyReleased() {
  switch(key) {
    case '1': mode=0; break;
    case '2': mode=1; break;
    case '3': mode=2; break;
    case '4': mode=3; break;
    case ' ': step=true;  break;
    default: break;
  }
}

void generateNails() {
  float radius = bufferWidth / 2;
  PVector center = new PVector(width / 2, height / 2);
  for(int i = 0; i < numNails; i++) {
    float angle = TWO_PI * i / numNails;
    float x = center.x + cos(angle) * radius;
    float y = center.y + sin(angle) * radius;
    nails.add(new Nail(new PVector(x, y)));
  }
}
