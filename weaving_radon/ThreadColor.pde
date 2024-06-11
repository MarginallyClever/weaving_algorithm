
class ThreadColor {
  PVector start;
  PVector end;
  color col;
  float theta, r;
  PGraphics radonTransform;

  ThreadColor(PVector start, PVector end, color col,float theta,float r) {
    this.start = start;
    this.end = end;
    this.col = col;
    this.theta = theta;
    this.r = r;
  }

  void display(PGraphics pg) {
    pg.stroke(col);
    pg.line(start.x, start.y, end.x, end.y);
  }
}
