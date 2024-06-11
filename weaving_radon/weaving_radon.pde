//--------------------------------------------------------------
// using radon transform to select next-best thread
// 2024-06-11 dan@marginallyclever.com
//--------------------------------------------------------------
int numNails = 188;  // Number of nails
int bufferWidth = 800;
int bufferHeight = 800;

ArrayList<Nail> nails = new ArrayList<Nail>();
ArrayList<ThreadColor> threads = new ArrayList<ThreadColor>();
PImage sourceImage;
RadonThreader radonThreader;
boolean ready = false;


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
    println("compute...");
    radonThreader.createThreads();
    println("ready");
    ready = true;
  }
}

void draw() {
  if (!ready) return;
  
  background(0);
  image(radonThreader.currentRadonImage,0,0);
  
  if(threads.size()>0) {
    ThreadColor t = threads.get(threads.size()-1);
    image(t.radonTransform,180,0);
  }
  
  for (ThreadColor t : threads) {
    t.display(g);
  }
  
  for (Nail n : nails) {
    n.display(g);
  }
  
  if(!radonThreader.addNextBestThread()) {
    noLoop();
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
