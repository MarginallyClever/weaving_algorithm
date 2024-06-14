
class ThreadColor {
  PVector start;  // xy
  PVector end;  // xy 
  color col;  // rgba
  int theta;  // degrees
  int r;  // distance, unit unknown
  PGraphics radonTransform;

  ThreadColor(PVector start, PVector end, int theta, int r, color col) {
    this.start = start;
    this.end = end;
    this.theta = theta;
    this.r = r;
    this.col = col;
  }

  void display(PGraphics pg) {
    pg.stroke(col);
    pg.line(start.x, start.y, end.x, end.y);
  }
}
