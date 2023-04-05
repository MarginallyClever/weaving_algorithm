int drawMode = 0;
int startIndex = 0;

void draw() {
  background(0);
  blendMode(BLEND);
  
  if(drawMode==0) {
    if(img!=null) image(img,width/2,0);
    drawCircleBorder();
  } else {
    if(imageToPath.get(drawMode-1).croppedImg!=null) {
      image(imageToPath.get(drawMode-1).croppedImg,width/2,0);
      drawCircleBorder();
      drawOneFilterColor(drawMode-1,width/2+5,5);
    }
    noFill();
  }
  
  for(ImageToPath i : imageToPath) {
    i.iterate();
  }
  
  drawActiveLayers();
  
  imageToPath.get(0).drawNails();
}

void drawActiveLayers() {
  for(int i=0;i<imageToPath.size();++i) {
    int j = getOffsetIndex(i);
    ImageToPath path = imageToPath.get(j);
    path.drawPath();
    
    drawOneFilterColor(j,5+i*20,5);
    if(path.paused) {
      noFill();
      stroke(255,0,0);
      rect(5+i*20,5,20,20);
    }
  }
}

int getOffsetIndex(int i) {
  int size=imageToPath.size();
  return (i+startIndex)%size;
}


void drawOneFilterColor(int index, int x,int y) {
  // draw the color of this imagePath
  fill(imageToPath.get(index).filterColor);
  stroke(imageToPath.get(index).filterColor);
  rect(x,y,20,20);
}


void drawCircleBorder() {
  blendMode(MULTIPLY);
  image(circleBorder,700,0);
  blendMode(BLEND);
}

void keyReleased() {
  if(keyCode=='1') imageToPath.get(getOffsetIndex(0)).paused = !imageToPath.get(getOffsetIndex(0)).paused;
  if(keyCode=='2') imageToPath.get(getOffsetIndex(1)).paused = !imageToPath.get(getOffsetIndex(1)).paused;
  if(keyCode=='3') imageToPath.get(getOffsetIndex(2)).paused = !imageToPath.get(getOffsetIndex(2)).paused;
  if(keyCode=='4') imageToPath.get(getOffsetIndex(3)).paused = !imageToPath.get(getOffsetIndex(3)).paused;
  if(keyCode=='5') imageToPath.get(getOffsetIndex(4)).paused = !imageToPath.get(getOffsetIndex(4)).paused;
  
  int size = imageToPath.size()+1;
  if(keyCode=='6') drawMode=(drawMode+      1) % size;
  if(keyCode=='7') drawMode=(drawMode+ size-1) % size;
  
  size = imageToPath.size();
  if(keyCode=='8') startIndex = (startIndex+1) % size; 
  if(keyCode=='9') startIndex = (startIndex+size-1) % size;
}
