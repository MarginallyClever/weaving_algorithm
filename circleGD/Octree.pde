

// see https://rosettacode.org/wiki/Color_quantization
public class OctreeNode {
  int r=0,g=0,b=0;
  int count=0;
  int numKids=0;
  OctreeNode [] kids = new OctreeNode[12];
  
  OctreeNode parent;
  int myChildIndex;
  int myDepth;
  
  public OctreeNode(int childIndex, int depth, OctreeNode parentNode) {
    parent = parentNode;
    myChildIndex = childIndex;
    myDepth = depth;
  }
};


// used to sort the heap
class OctreeSorter implements java.util.Comparator<OctreeNode> {
  // sort with leaves first and with the least-significant leaf first.
  // so the leaf with the least count goes first.
  public int compare(OctreeNode a, OctreeNode b) {
    int aa = a.count + 10000/(8-a.numKids);
    int bb = b.count + 10000/(8-b.numKids);
    return aa - bb;
  } 
}


class Octree {
  ArrayList<OctreeNode> heap = new ArrayList<OctreeNode>();
  OctreeSorter sorter = new OctreeSorter();
  OctreeNode root;


  OctreeNode buildQuantizedPalette(PImage pg,int numColors) {
    pg.loadPixels();
    
    root = new OctreeNode(0,0,null);
    heap.clear();
    
    int size = pg.width*pg.height;
    for(int i=0;i<size;++i) {
      // insert only leaf-most nodes of the tree to the heap.
      OctreeNode n = treeInsert(root,pg.pixels[i]);
      if(!heap.contains(n)) {
        heap.add(n);
      }
    }
  
    // sort the heap
    java.util.Collections.sort(heap,sorter);

    println("start reducing");
    
    // now we have the list of colors, reduce.
    while(heap.size() > numColors) {
      // tail of the heap has the least used color(s) in a leaf-most node.
      // fold that node into its parent.
      OctreeNode goAway = heap.remove(0);
      
      OctreeNode n = treeFold(goAway);
      
      if(n!= root && !heap.contains(n)) {
        heap.add(n);
        // sort the heap
        java.util.Collections.sort(heap,sorter);
      }
    }
    
    println(millis() + " heap.size="+heap.size());
    println("done.");
    
    for(int i=0;i<heap.size();++i) {
      heap.get(i).myDepth=0;
    }
    
    java.util.Collections.sort(heap,sorter);
        
    for(int i=0;i<heap.size();++i) {
      OctreeNode n = heap.get(i);
      n.r=(int)((float)n.r / (float)n.count+0.5);
      n.g=(int)((float)n.g / (float)n.count+0.5);
      n.b=(int)((float)n.b / (float)n.count+0.5);
      println("palette "+n.count
              +"("+n.r
              +","+n.g
              +","+n.b
              +")");
    }
    
    return root;
  }
  
  
  void quantize(PImage pg,int numColors) {
    OctreeNode root = buildQuantizedPalette(pg,numColors);
    pg.loadPixels();
    int size = pg.width*pg.height;
    for(int i=0;i<size;++i) {
      pg.pixels[i] = treeReplace(root,pg.pixels[i]);
    }
    pg.updatePixels();
  }
  
  
  // replace color c with the color in the closest branch
  color treeReplace(OctreeNode node,color c) {
    int r = (int)red  (c);
    int g = (int)green(c);
    int b = (int)blue (c);
    
    for(int depth=0; depth<5; depth++ ) {
      int shift = 7-depth;
      int rr = (r >> shift) & 0x1;
      int gg = (g >> shift) & 0x1;
      int bb = (b >> shift) & 0x1;
      int i = rr*4 + gg*2 + bb;
      //print(i);
      
      if(node.kids[i]==null) break;
      node = node.kids[i];
    }
    
    return color(node.r,node.g,node.b);
  }
  
  
  OctreeNode treeInsert(OctreeNode node,color c) {
    int r = (int)red  (c);
    int g = (int)green(c);
    int b = (int)blue (c);
    
    for(int depth=0; depth<5; depth++ ) {
      int shift = 7-depth;
      int rr = (r >> shift) & 0x1;
      int gg = (g >> shift) & 0x1;
      int bb = (b >> shift) & 0x1;
      int i = rr*4 + gg*2 + bb;
      //print(i);
      
      if(node.kids[i]==null) {
        node.kids[i] = new OctreeNode(i,depth+1,node);
        node.numKids++;
      }
      node = node.kids[i];
    }
    
    //println(" "+node.count);
    
    node.r += r;
    node.g += g;
    node.b += b;
    node.count++;
    
    return node;
  }
  
  
  OctreeNode treeFold(OctreeNode leaf) {
    OctreeNode p = leaf.parent;
    
    p.count += leaf.count;
    p.r += leaf.r;
    p.g += leaf.g;
    p.b += leaf.b;
    
    p.kids[leaf.myChildIndex]=null;
    p.numKids--;
    
    return p;
  }
}
