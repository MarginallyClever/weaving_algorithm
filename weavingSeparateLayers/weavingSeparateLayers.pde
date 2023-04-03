//-----------------------------------------------------------
// create a separate image for each string color.
// substract the best string from each image.
// display the result of all the strings put together.
// 2023-04-03 dan@marginallyclever.com
//-----------------------------------------------------------
final int SIZE = 700;

PImage img;
ImageToPath [] imageToPath;

void setup() {
  size(1400, 700);  // SIZE*2,SIZE
  
  imageToPath = new ImageToPath[2];
  imageToPath[0] = new ImageToPath(180,SIZE);
  imageToPath[1] = new ImageToPath(180,SIZE);
  
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
    PImage croppedImage = img.copy();
    croppedImage.filter(GRAY);
    PImage inverse = croppedImage.copy();
    inverse.filter(INVERT);

    imageToPath[0].begin(croppedImage,0,32);
    imageToPath[1].begin(inverse,0,32);
  }
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
