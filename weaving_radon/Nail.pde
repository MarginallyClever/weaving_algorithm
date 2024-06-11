

class Nail {
  PVector position;
  
  Nail(PVector position) {
    this.position = position;
  }
  
  void display(PGraphics pg) {
    pg.fill(0);
    pg.ellipse(position.x, position.y, 5, 5);
  }
}
