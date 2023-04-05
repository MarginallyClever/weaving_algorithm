//-----------------------------------------------------------
// create a separate image for each string color.
// substract the best string from each image.
// display the result of all the strings put together.
// 2023-04-03 dan@marginallyclever.com
//-----------------------------------------------------------
// number of nails around the perimeter.
final int NUM_NAILS = 180;
// 0...1  adjusts alpha of all strings.  lower is more transparent.
final float alphaAdjust = 0.5f;
// 0...1 controls when to stop adding lines.  lower means more lines.
final float minimumErrorLimit = 0.09;
// must be greater than 0.  thickness of line in path.
final float myStrokeWeight = 1.0;
// skip all lines shorter than this many pixels
final int minimumLineLength = 50;
// anti-alias all lines.
final int mySmooth = 1;

// do not touch these.
PImage img;
PGraphics circleBorder;
ArrayList<ImageToPath> imageToPath = new ArrayList<ImageToPath>();
color backgroundColor;

void setup() {
  // size of window
  size(1600, 800);  // width must be height*2.
  
  // change your color layers here.  first color will be on the bottom of the stack.
  imageToPath.add(new ImageToPath(NUM_NAILS,height,color(255,255,255)));  // white
  //imageToPath.add(new ImageToPath(NUM_NAILS,height,color(255,0,0)));  // red
  //imageToPath.add(new ImageToPath(NUM_NAILS,height,color(0,255,0)));  // green
  //imageToPath.add(new ImageToPath(NUM_NAILS,height,color(0,0,255)));  // blue
  imageToPath.add(new ImageToPath(NUM_NAILS,height,color(0,255,255)));  // cyan
  imageToPath.add(new ImageToPath(NUM_NAILS,height,color(255,0,255)));  // magenta
  imageToPath.add(new ImageToPath(NUM_NAILS,height,color(255,255,0)));  // yellow
  imageToPath.add(new ImageToPath(NUM_NAILS,height,color(0,0,0,0)));  // black
  
  createCircleBorder();
  
  // Request an image file from the user
  selectInput("Select an image file:", "imageSelected");
}

String colorToString(color c) {
  return ""+red(c)+","+green(c)+","+blue(c)+","+alpha(c);
}

void imageSelected(File selection) {
  if (selection == null) {
    println("No file selected.");
    exit();
  } else {
    String filePath = selection.getAbsolutePath();
    img = loadImage(filePath);
    img = cropAndScaleImage(img, height, height);
    backgroundColor = calculateAverageColor(img);
    
    // Crop and scale the image
    for(ImageToPath path : imageToPath) {
      path.begin(img);
    }
  }
}

color calculateAverageColor(PImage img) {
  float totalRed = 0;
  float totalBlue = 0;
  float totalGreen = 0;
  int numPixels = img.width * img.height;

  circleBorder.loadPixels();
  img.loadPixels();
  for (int i = 0; i < numPixels; i++) {
    color c = img.pixels[i];
    float v = red(circleBorder.pixels[i])/255.0f;
    totalRed += red(c)*v;
    totalGreen += green(c)*v;
    totalBlue += blue(c)*v;
  }

  return color( totalRed / numPixels,
                totalGreen / numPixels,
                totalBlue / numPixels );
}


PImage cropAndScaleImage(PImage source, int targetWidth, int targetHeight) {
  float aspectRatioSource = float(source.width) / float(source.height);
  float aspectRatioTarget = float(targetWidth) / float(targetHeight);

  int cropWidth, cropHeight;

  if (aspectRatioSource > aspectRatioTarget) {
    cropHeight = source.height;
    cropWidth = int(cropHeight * aspectRatioTarget);
  } else {
    cropWidth = source.width;
    cropHeight = int(cropWidth / aspectRatioTarget);
  }

  int cropX = (source.width - cropWidth) / 2;
  int cropY = (source.height - cropHeight) / 2;

  PImage result = source.get(cropX, cropY, cropWidth, cropHeight);
  result.resize(targetWidth, targetHeight);
  //result = result.get(cropX, cropY, targetWidth, targetHeight);

  return result;
}

/**
 * generate a gray image where white matches the target color and black is as far as possible from the target color. 
 */
public PImage generateGrayscaleImage(PImage img, color targetColor) {
  PImage result = createImage(img.width, img.height, RGB);

  for (int x = 0; x < img.width; x++) {
    for (int y = 0; y < img.height; y++) {
      color currentColor = img.get(x, y);
      float similarity = calculateColorSimilarity(currentColor, targetColor);
      int gray = (int) map(similarity, 0, 1, 0, 255);
      result.set(x, y, color(gray, gray, gray));
    }
  }

  return result;
}

public float calculateColorSimilarity(color c1, color c2) {
  float r1 = red(c1);
  float g1 = green(c1);
  float b1 = blue(c1);
  
  float r2 = red(c2);
  float g2 = green(c2);
  float b2 = blue(c2);

  float distance = dist(r1, g1, b1, r2, g2, b2);
  float maxDistance = dist(0, 0, 0, 255, 255, 255);

  return 1 - (distance / maxDistance);
}

void createCircleBorder() {
  circleBorder = createGraphics(height,height);
  circleBorder.beginDraw();
  circleBorder.background(0);
  circleBorder.fill(255);
  circleBorder.circle(height/2,height/2,height);
  circleBorder.endDraw();
}
