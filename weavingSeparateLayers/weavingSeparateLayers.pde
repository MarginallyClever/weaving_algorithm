//-----------------------------------------------------------
// create a separate image for each string color.
// substract the best string from each image.
// display the result of all the strings put together.
// 2023-04-03 dan@marginallyclever.com
//-----------------------------------------------------------
final int SIZE = 700;

PImage img;
ArrayList<ImageToPath> imageToPath = new ArrayList<ImageToPath>();

void setup() {
  size(1400, 700);  // SIZE*2,SIZE
  
  imageToPath.add(new ImageToPath(180,SIZE));
  imageToPath.add(new ImageToPath(180,SIZE));
  imageToPath.add(new ImageToPath(180,SIZE));
  imageToPath.add(new ImageToPath(180,SIZE));
  imageToPath.add(new ImageToPath(180,SIZE));
  
  // Request an image file from the user
  selectInput("Select an image file:", "imageSelected");
}

void imageSelected(File selection) {
  if (selection == null) {
    println("No file selected.");
    exit();
  } else {
    String filePath = selection.getAbsolutePath();
    img = loadImage(filePath);
    img = cropAndScaleImage(img, SIZE, SIZE);
    
    // Crop and scale the image
    startPath(0,img,color(255,255,255),32);  // white
    startPath(1,img,color(0,0,0,0),32);  // black
    startPath(2,img,color(255,0,0),32);  // red
    startPath(3,img,color(0,255,0),32);  // green
    startPath(4,img,color(0,0,255),32);  // blue
  }
}

void startPath(int index,PImage sourceImage,color c,int alpha) {
  PImage redChannel = generateGrayscaleImage(sourceImage,c);
  color c2 = color(red(c),green(c),blue(c),alpha);
  imageToPath.get(index).begin(redChannel,0,alpha,c2);
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
  //PImage result = source.get(cropX, cropY, targetWidth, targetHeight);

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
