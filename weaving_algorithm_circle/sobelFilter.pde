// https://en.wikipedia.org/wiki/Sobel_operator

// calculates the sobel filter of the image and stores results in the red and blue channels.
void sobelFilter(PImage src,PImage dest) {
  println("sobel started");
  src.loadPixels();
  dest.loadPixels();
  int [] ox = { -1, 0, 1, -1,0, 1, -1, 0, 1 };
  int [] oy = { -1,-1,-1,  0,0, 0,  1, 1, 1 };
  float [] gx = {  1, 0,-1,  2,0,-2,  1, 0,-1 };
  float [] gy = {  1, 2, 1,  0,0, 0, -1,-2,-1 };
  
  float maxR = -Float.MAX_VALUE;  
  float minR = Float.MAX_VALUE;
  float maxB = -Float.MAX_VALUE;  
  float minB = Float.MAX_VALUE;
  int w = img.width;
  for(int y=1;y<img.height-1;y++) {
    for(int x=1;x<w-1;x++) {
      float r = 0;
      float b = 0;
      for(int n=0;n<9;n++) {
        int px = x+ox[n];
        int py = y+oy[n];
        color c = src.pixels[(py*w)+px];
        float v = sqrt(sq(red(c))+sq(green(c))+sq(blue(c)));
        r += v * gx[n];
        b += v * gy[n];
      }
      
      if(maxR<r) maxR=r;
      if(minR>r) minR=r;
      if(maxB<b) maxB=b;
      if(minB>b) minB=b;
      r=abs(r);
      b=abs(b);
      dest.pixels[y*w+x] = color(r,0,b);
    }
  }
  dest.updatePixels();
  println("sobel finished");
  println("r="+maxR+"\t"+minR);
  println("b="+maxB+"\t"+minB);
}


// calculate the distance from the center of the image.  Store it as a value 0...255 in the green channel
// assumes the image is square
void sobelPrecalculateDistances(PImage dest,float lowpass,float cx,float cy) {
  int w = img.width;
  int center = w/2;
  float maxd = sqrt(sq(center)*2);
  
  for(int y=0;y<img.height;y++) {
    float dy2 = sq(y-cy);
    for(int x=0;x<w;x++) {
      int addr=(y*w)+x;
      color c = dest.pixels[addr];
      float dx2 = sq(x-cx);
      float d = sqrt(dx2+dy2);
      int green = (int)floor(255.0*d/maxd);
      green = (d/maxd > lowpass) ? green : 0; 
      dest.pixels[addr] = color(red(c),green,blue(c));
    }
  }
}
