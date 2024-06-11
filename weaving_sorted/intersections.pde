class Intersection {
  PVector location;
  ArrayList<ThreadColor> threads = new ArrayList<ThreadColor>();
  public Intersection(PVector p) {
    location = p;
  }
}
ArrayList<Intersection> intersections = new ArrayList<Intersection>();


ArrayList<PVector> findAllIntersections(ArrayList<ThreadColor> order) {
  int size = order.size();
  for (int i = 0; i < size; i++) {
    //println(i +" of "+size);
    for (int j = i + 1; j < size; j++) {
      ThreadColor a = order.get(i);
      ThreadColor b = order.get(j);
      PVector intersection = findIntersection(a,b);
      if (intersection != null) {
        Intersection c = containsIntersection(a,b);
        if(c==null) {
          c = new Intersection(intersection);
          intersections.add(c);
        }
        c.threads.add(a);
        c.threads.add(b);
      }
    }
  }
  println("intersections found. "+intersections.size());
  ArrayList<PVector> list = new ArrayList<PVector>();
  
  for(Intersection c : intersections) {
    list.add(c.location);
  }
  
  return list;
}

Intersection containsIntersection(ThreadColor a,ThreadColor b) {
  for (Intersection existing : intersections) {
    if (existing.threads.contains(a) && existing.threads.contains(b)) {
      return existing;
    }
  }
  return null;
}


PVector findIntersection(ThreadColor A, ThreadColor B) {
  // Implement the logic to find intersection point between two threads if they intersect.
  // This is a simplified version assuming line segments intersect at some point.
  float a1 = A.end.y - A.start.y;
  float b1 = A.start.x - A.end.x;
  float c1 = a1 * (A.start.x) + b1 * (A.start.y);
  
  float a2 = B.end.y - B.start.y;
  float b2 = B.start.x - B.end.x;
  float c2 = a2 * (B.start.x) + b2 * (B.start.y);
  
  float determinant = a1 * b2 - a2 * b1;
  
  if (determinant == 0) {
    return null; // Parallel lines
  } else {
    float x = (b2 * c1 - b1 * c2) / determinant;
    float y = (a1 * c2 - a2 * c1) / determinant;
    return new PVector(x, y);
  }
}


boolean doIntersect(ThreadColor A, ThreadColor B) {
  // Implementation for checking if two threads intersect
  PVector p1 = A.start, q1 = A.end;
  PVector p2 = B.start, q2 = B.end;
  
  int o1 = orientation(p1, q1, p2);
  int o2 = orientation(p1, q1, q2);
  int o3 = orientation(p2, q2, p1);
  int o4 = orientation(p2, q2, q1);

  // General case
  if (o1 != o2 && o3 != o4) {
    intersectionPoint = findIntersection(A, B);
    intersectionColor = getColorAtIntersection(intersectionPoint);
    intersectionFound = true;
    return true;
  }

  // Special cases
  if (o1 == 0 && onSegment(p1, p2, q1)) return true;
  if (o2 == 0 && onSegment(p1, q2, q1)) return true;
  if (o3 == 0 && onSegment(p2, p1, q2)) return true;
  if (o4 == 0 && onSegment(p2, q1, q2)) return true;

  return false;
}

// To find orientation of ordered triplet (p, q, r).
int orientation(PVector p, PVector q, PVector r) {
  float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
  if (val == 0) return 0; // collinear
  return (val > 0) ? 1 : 2; // clock or counterclock wise
}

// Given three collinear points p, q, r, the function checks if
// point q lies on line segment 'pr'
boolean onSegment(PVector p, PVector q, PVector r) {
  if (q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x) &&
      q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y))
    return true;
  return false;
}

color getColorAtIntersection(PVector intersection) {
  // Placeholder function to get the color at the intersection point
  // This function should return the color from the image or any other source
  // For now, we return a random color for demonstration purposes
  return backgroundImage.get((int)intersection.x,(int)intersection.y);
}

boolean betterColor(color c1, color c2) {
  // Compare color c1 and c2 to the intersectionColor
  return colorDifference(c1, intersectionColor) < colorDifference(c2, intersectionColor);
}
