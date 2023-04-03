int drawMode = 0;

void draw() {
  background(0);
  blendMode(BLEND);
  
  switch(drawMode) {
    default:
      if (img != null) image(img, 700, 0);
      break;
    case 1:
      image(imageToPath[0].croppedImg,700,0);
      break;
    case 2:
      image(imageToPath[1].croppedImg,700,0);
      break;
  }
  
  imageToPath[0].iterate();
  imageToPath[1].iterate();
  
  imageToPath[0].drawNails();
  imageToPath[0].drawPath();
  blendMode(SUBTRACT);
  imageToPath[1].drawPath();
}

void keyReleased() {
  if(keyCode=='1') imageToPath[0].paused=!imageToPath[0].paused;
  if(keyCode=='2') imageToPath[1].paused=!imageToPath[1].paused;
  if(keyCode=='3') drawMode=0;
  if(keyCode=='4') drawMode=1;
  if(keyCode=='5') drawMode=2;
}
