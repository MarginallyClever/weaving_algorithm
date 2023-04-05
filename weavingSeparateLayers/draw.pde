int drawMode = 0;

void draw() {
  background(255);
  blendMode(BLEND);
  
  if(drawMode==0) {
    if(img!=null) image(img,700,0);
  } else {
    if(imageToPath.get(drawMode-1).croppedImg!=null) {
      image(imageToPath.get(drawMode-1).croppedImg,700,0);
    }
  }
  
  imageToPath.get(0).iterate();
  imageToPath.get(1).iterate();
  imageToPath.get(2).iterate();
  imageToPath.get(3).iterate();
  imageToPath.get(4).iterate();
  
  imageToPath.get(0).drawNails();
  
  imageToPath.get(1).drawPath();
  imageToPath.get(2).drawPath();
  imageToPath.get(3).drawPath();
  imageToPath.get(4).drawPath();
  imageToPath.get(0).drawPath();
}

void keyReleased() {
  if(keyCode=='1') imageToPath.get(0).paused = !imageToPath.get(0).paused;
  if(keyCode=='2') imageToPath.get(1).paused = !imageToPath.get(1).paused;
  if(keyCode=='3') imageToPath.get(2).paused = !imageToPath.get(2).paused;
  if(keyCode=='4') imageToPath.get(3).paused = !imageToPath.get(3).paused;
  if(keyCode=='5') imageToPath.get(4).paused = !imageToPath.get(4).paused;
  
  int size = imageToPath.size()+1;
  if(keyCode=='6') drawMode=(drawMode+      1) % size;
  if(keyCode=='7') drawMode=(drawMode+ size-1) % size;
}
