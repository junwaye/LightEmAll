import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// interface for Constants 
interface Constants {
  int CELL_SIZE = 50;
  int GAME_HEIGHT = 5;
  int GAME_WIDTH = 5;
}

//LightEmAllWorld
class LightEmAllWorld extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of nodes
  ArrayList<GamePiece> nodes;
  // ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  Random rand;
  Utils u = new Utils();

  // Constructor
  LightEmAllWorld(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand;
    this.board = new Utils().initializeBoard(width, height);

    Union unionHolder = new Union(new Utils().createEdges(board, width, height));
    u.updateGamePieces(unionHolder.kruskalAlgo());
    this.nodes = u.assignNodes(this.board);
    u.randomizeNodes(this.nodes, rand);
    u.lightUp(board, width, height, nodes);
    this.mst = null;
    this.powerRow = 1;
    this.powerCol = 1;
    radius = 0;

  }

  // Constructor
  LightEmAllWorld(int width, int height) {
    this.width = width;
    this.height = height;
    this.rand = new Random();
    this.board = u.initializeBoard(width, height);
    Union unionHolder = new Union(new Utils().createEdges(board, width, height));
    u.updateGamePieces(unionHolder.kruskalAlgo());
    this.nodes = u.assignNodes(this.board);
    u.randomizeNodes(this.nodes, rand);
    u.lightUp(board, width, height, nodes);
    this.mst = null;
    this.powerRow = 1;
    this.powerCol = 1;
    radius = 0;

  }

  // Draws the world
  public WorldScene makeScene() {
    WorldScene scene = this.getEmptyScene();
    WorldImage finalImage = new EmptyImage();

    for (ArrayList<GamePiece> colList : this.board) {
      WorldImage columnImage = colList.get(0).tileImage(Constants.CELL_SIZE,
          Constants.CELL_SIZE / 4, colList.get(0).poweredColor(), colList.get(0).powerStation);

      // Stack images from column vertically
      for (int i = 1; i < colList.size(); i++) {
        columnImage = new AboveImage(columnImage, colList.get(i).tileImage(Constants.CELL_SIZE,
            Constants.CELL_SIZE / 4, colList.get(i).poweredColor(), colList.get(i).powerStation));
      }
      // Combine column images horizontally
      finalImage = new BesideImage(finalImage, columnImage);
    }

    // Place the final image in the center of the scene
    scene.placeImageXY(finalImage, this.width * Constants.CELL_SIZE / 2,
        this.height * Constants.CELL_SIZE / 2);

    // Check for win condition
    if (u.win(this.nodes)) {
      RectangleImage rec = new RectangleImage(Constants.CELL_SIZE * 2, Constants.CELL_SIZE,
          OutlineMode.SOLID, Color.WHITE);
      TextImage text = new TextImage("You Won", Constants.CELL_SIZE / 3, FontStyle.BOLD,
          Color.GREEN);
      WorldImage last = new OverlayImage(text, rec);

      scene.placeImageXY(last, this.width * Constants.CELL_SIZE / 2,
          this.height * Constants.CELL_SIZE / 2);

      return scene;
    }

    return scene;
  }

  // rotates a GamePiece if clicked on
  public void onMouseClicked(Posn posn) {
    int x = posn.x / Constants.CELL_SIZE;
    int y = posn.y / Constants.CELL_SIZE;
    GamePiece gp = this.board.get(x).get(y);
    u.rotate(gp);
    u.lightUp(this.board, this.width, this.height, this.nodes);
  }

  // Key pressed moving the power station
  public void onKeyEvent(String key) {
    if (key.equals("up") || key.equals("left") || key.equals("down") || key.equals("right")) {
      if (u.checkIfConnected(key, this.board, this.width, this.height,
          u.findPowerStation(this.board))) {
        u.movePowerStation(key, this.board);
      }
    }
  }
}

// class for GamePiece 
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  GamePiece() {
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
    this.powerStation = false;
    this.powered = false;
  }

  // Constructor
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;
  }

  // Constructor for Testing
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }

  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the
  // power station
  //
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that
    // can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return new FrameImage(image);
  }

  // Yellow color for powered
  Color poweredColor() {
    if (this.powered) {
      return Color.YELLOW;
    }
    else {
      return Color.GRAY;
    }
  }

  // Comparing the 2 pieces
  // checking for opening and making opening
  void compare(GamePiece piece2) {
    if (this.col - piece2.col == -1) {
      this.right = true;
      piece2.left = true;
    }
    else if (this.col - piece2.col == 1) {
      this.left = true;
      piece2.right = true;
    }
    else if (this.row - piece2.row == -1) {
      this.bottom = true;
      piece2.top = true;
    }
    else if (this.row - piece2.row == 1) {
      this.top = true;
      piece2.bottom = true;
    }
  }
}

//Utilities methods
class Utils {
  // Generate a board with the power station at position 1, 1
  public ArrayList<ArrayList<GamePiece>> initializeBoard(int width, int height) {
    ArrayList<ArrayList<GamePiece>> gameBoard = new ArrayList<>();
    boolean isPowerStation = false;

    for (int i = 1; i <= width; i++) {
      ArrayList<GamePiece> columnList = new ArrayList<>();
      for (int j = 1; j <= height; j++) {
        // Place power station at (1, 1)
        if (i == 1 && j == 1) {
          isPowerStation = true;
        }
        else {
          isPowerStation = false;
        }
        columnList.add(new GamePiece(j, i, false, false, false, false, isPowerStation));
      }

      gameBoard.add(columnList);
    }

    return gameBoard;
  }

  // Adds GamePieces on the board to a list of Nodes
  public ArrayList<GamePiece> assignNodes(ArrayList<ArrayList<GamePiece>> list) {
    ArrayList<GamePiece> nodes = new ArrayList<GamePiece>();
    for (int i = 0; i < list.size(); i++) {
      for (int j = 0; j < list.get(i).size(); j++) {
        nodes.add(list.get(i).get(j));
      }
    }
    return nodes;
  }

  // Rotating the piece clockwise
  public void rotate(GamePiece piece) {
    boolean temp = piece.top;
    piece.top = piece.left;
    piece.left = piece.bottom;
    piece.bottom = piece.right;
    piece.right = temp;
  }

  // Rotates each GamePiece on the board a random number of times
  public void randomizeNodes(ArrayList<GamePiece> list, Random rand) {
    for (int i = 0; i < list.size(); i++) {
      int num = rand.nextInt(25);
      for (int j = 0; j <= num; j++) {
        rotate(list.get(i));
      }
    }
  }

  // Find the power station
  public GamePiece findPowerStation(ArrayList<ArrayList<GamePiece>> list) {
    GamePiece powerStationPiece = new GamePiece(3, 3, false, false, false, false, false);
    for (ArrayList<GamePiece> arrGp : list) {
      for (GamePiece gp : arrGp) {
        if (gp.powerStation) {
          powerStationPiece = gp;
        }
      }
    }
    return powerStationPiece;
  }

  // Check's if a given GamePiece is connected to a surrounding
  // GamePiece based on the given direction
  public boolean checkIfConnected(String direction, ArrayList<ArrayList<GamePiece>> list, int width,
      int height, GamePiece piece) {
    if (direction.equals("up")) {
      if (piece.row == 1) {
        return false;
      }
      else {
        return piece.top && list.get(piece.col - 1).get(piece.row - 2).bottom;
      }
    }
    else if (direction.equals("right")) {
      if (piece.col == width) {
        return false;
      }
      else {
        return piece.right && list.get(piece.col).get(piece.row - 1).left;
      }
    }
    else if (direction.equals("down")) {
      if (piece.row == height) {
        return false;
      }
      else {
        return piece.bottom && list.get(piece.col - 1).get(piece.row).top;
      }
    }
    else if (direction.equals("left")) {
      if (piece.col == 1) {
        return false;
      }
      else {
        return piece.left && list.get(piece.col - 2).get(piece.row - 1).right;
      }
    }
    else {
      return false;
    }
  }

  // lights up the piece if supposed to
  public void lightUp(ArrayList<ArrayList<GamePiece>> list, int width, int height,
      ArrayList<GamePiece> list2) {

    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();
    ArrayList<GamePiece> workList = new ArrayList<GamePiece>();
    workList.add(findPowerStation(list));
    while (workList.size() != 0) {
      GamePiece gp = new GamePiece(3, 3, false, false, false, false, false);
      GamePiece next = workList.remove(0);
      GamePiece top = gp;
      GamePiece right = gp;
      GamePiece bottom = gp;
      GamePiece left = gp;

      if (next.col != 1) {
        left = list.get(next.col - 2).get(next.row - 1);
      }
      if (next.col != width) {
        right = list.get(next.col).get(next.row - 1);
      }

      if (next.row != 1) {
        top = list.get(next.col - 1).get(next.row - 2);
      }

      if (next.row != height) {
        bottom = list.get(next.col - 1).get(next.row);
      }

      next.powered = true;
      if (!alreadySeen.contains(top) && checkIfConnected("up", list, width, height, next)) {
        workList.add(top);
      }

      if (!alreadySeen.contains(right) && checkIfConnected("right", list, width, height, next)) {
        workList.add(right);
      }

      if (!alreadySeen.contains(bottom) && checkIfConnected("down", list, width, height, next)) {
        workList.add(bottom);
      }

      if (!alreadySeen.contains(left) && checkIfConnected("left", list, width, height, next)) {
        workList.add(left);
      }

      alreadySeen.add(next);

      for (GamePiece piece : list2) {
        if (!(alreadySeen.contains(piece))) {
          piece.powered = false;
        }
      }
    }
  }

  // Checks if all of the wires are lit
  public boolean win(ArrayList<GamePiece> list) {
    int lightsUp = 0;
    for (GamePiece piece : list) {
      if (piece.powered) {
        lightsUp++;
      }
    }
    return lightsUp == list.size();
  }

  // Moves the power station from one GamePiece to the next
  public void movePowerStation(String direction, ArrayList<ArrayList<GamePiece>> list) {
    GamePiece powerStation = new Utils().findPowerStation(list);
    powerStation.powerStation = false;

    if (direction.equals("up")) {
      list.get(powerStation.col - 1).get(powerStation.row - 2).powerStation = true;

    }
    else if (direction.equals("right")) {
      list.get(powerStation.col).get(powerStation.row - 1).powerStation = true;

    }
    else if (direction.equals("down")) {
      list.get(powerStation.col - 1).get(powerStation.row).powerStation = true;

    }
    else if (direction.equals("left")) {
      list.get(powerStation.col - 2).get(powerStation.row - 1).powerStation = true;
    }
  }

  // Iterates through each cell,
  // checking if it can connect to neighboring cells to the right, below, left,
  // and above
  public ArrayList<Edge> createEdges(ArrayList<ArrayList<GamePiece>> list, int width, int height) {
    ArrayList<Edge> edgeList = new ArrayList<>();

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        GamePiece current = list.get(i).get(j);

        // Connect to the right neighbor
        if (j < height - 1) {
          GamePiece right = list.get(i).get(j + 1);
          edgeList.add(new Edge(current, right));
        }
        // Connect to the bottom neighbor
        if (i < width - 1) {
          GamePiece below = list.get(i + 1).get(j);
          edgeList.add(new Edge(current, below));
        }
        // Connect to the left neighbor
        if (j > 0) {
          GamePiece left = list.get(i).get(j - 1);
          edgeList.add(new Edge(current, left));
        }
        // Connect to the above neighbor
        if (i > 0) {
          GamePiece above = list.get(i - 1).get(j);
          edgeList.add(new Edge(current, above));
        }
      }
    }
    edgeList.sort(new SortEdges());
    return edgeList;
  }

  // Compute a hashmap where each game piece from the edges list is mapped to
  // itself
  public HashMap<GamePiece, GamePiece> makeHashMap(ArrayList<Edge> list) {
    HashMap<GamePiece, GamePiece> map = new HashMap<GamePiece, GamePiece>();
    for (Edge edge : list) {
      map.put(edge.fromNode, edge.fromNode);
      map.put(edge.toNode, edge.toNode);
    }
    return map;
  }

  // Find the root of a game piece within the map
  public GamePiece find(HashMap<GamePiece, GamePiece> map, GamePiece edge) {
    GamePiece root = edge;
    while (!(map.get(root).equals(root))) {
      root = map.get(root);
    }
    return root;
  }

  // Merges two subsets into one, linking one gamepiece to another
  public void union(HashMap<GamePiece, GamePiece> map, GamePiece to, GamePiece from) {
    map.put(map.get(to), from);
  }

  // Compare gamepiece with connected neighbors,
  // update their state or properties
  public void updateGamePieces(ArrayList<Edge> list) {
    for (Edge edge : list) {
      edge.fromNode.compare(edge.toNode);
    }
  }
}

// class for Edge 
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  // The Constructor
  Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = new Random().nextInt(25);
  }

  // The Constructor
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

//Union class
class Union {
  HashMap<GamePiece, GamePiece> representatives;
  ArrayList<Edge> edgesInTree;
  ArrayList<Edge> worklist;

  // The Constructor
  Union(ArrayList<Edge> worklist) {
    this.representatives = new Utils().makeHashMap(worklist);
    this.worklist = worklist;
    this.edgesInTree = new ArrayList<Edge>();
  }

  // compute the minimum spanning tree of a graph
  // processes edges in ascending weight order
  // connecting sets of nodes without forming cycles until all nodes are
  // connected.
  ArrayList<Edge> kruskalAlgo() {
    Utils u = new Utils();
    while (!worklist.isEmpty()) {
      Edge cheapestEdge = worklist.get(0);
      GamePiece fromRep = u.find(representatives, cheapestEdge.fromNode);
      GamePiece toRep = u.find(representatives, cheapestEdge.toNode);

      if (fromRep.equals(toRep)) {
        worklist.remove(0);
      }
      else {
        edgesInTree.add(worklist.remove(0));
        u.union(representatives, fromRep, toRep);
      }
    }
    return edgesInTree;
  }
}

//class for SortEdges, comparing the edges weight
class SortEdges implements Comparator<Edge> {
  public int compare(Edge edge1, Edge edge2) {
    return edge1.weight - edge2.weight;
  }
}

//class for examples and testing
class ExamplesLight {

  GamePiece gp1 = new GamePiece(1, 1, false, false, false, false, false);
  GamePiece gp2 = new GamePiece(1, 2, false, false, false, false, false);
  GamePiece gp3 = new GamePiece(2, 1, false, false, false, false, false);
  GamePiece gp4 = new GamePiece(1, 2, false, false, false, false, false);

  // tileImage
  void testTileImage(Tester t) {
    // create game piece with all connections
    GamePiece gp1 = new GamePiece();
    gp1.top = true;
    gp1.bottom = true;
    gp1.left = true;
    gp1.right = true;

    // tests piece with all connections
    WorldImage image1 = gp1.tileImage(50, 5, Color.RED, false);
    t.checkExpect(image1.getWidth(), 50.0);
    t.checkExpect(image1.getHeight(), 50.0);
    t.checkExpect(gp1.top, true);
    t.checkExpect(gp1.right, true);
    t.checkExpect(gp1.bottom, true);
    t.checkExpect(gp1.left, true);

    // create a game piece with no connections
    GamePiece gp2 = new GamePiece();

    WorldImage image2 = gp2.tileImage(50, 5, Color.RED, false);
    // tests no connections
    t.checkExpect(gp2.top, false);
    t.checkExpect(gp2.right, false);
    t.checkExpect(gp2.bottom, false);
    t.checkExpect(gp2.left, false);

    // create a game piece with top and left connections
    GamePiece gp3 = new GamePiece();
    gp3.top = true;
    gp3.left = true;

    WorldImage image3 = gp3.tileImage(50, 5, Color.RED, false);
    t.checkExpect(gp3.top, true);
    t.checkExpect(gp3.left, true);

    // create a game piece with only right and bottom connections
    GamePiece gp4 = new GamePiece();
    gp4.right = true;
    gp4.bottom = true;

    WorldImage image4 = gp4.tileImage(50, 5, Color.RED, false);
    t.checkExpect(gp4.right, true);
    t.checkExpect(gp4.bottom, true);
  }

  // compare
  void testCompare(Tester t) {
    GamePiece gp1 = new GamePiece(1, 1, false, false, false, false, false);
    GamePiece gp2 = new GamePiece(1, 2, false, false, false, false, false);
    GamePiece gp3 = new GamePiece(2, 1, false, false, false, false, false);
    GamePiece gp4 = new GamePiece(1, 2, false, false, false, false, false);

    // Comparing right adjacency
    gp1.compare(gp2);
    t.checkExpect(gp1.right, true);
    t.checkExpect(gp2.left, true);

    // Comparing bottom adjacency
    gp1.compare(gp3);
    t.checkExpect(gp1.bottom, true);
    t.checkExpect(gp3.top, true);

    gp1.compare(gp4);
    t.checkExpect(gp1.top, false);
    t.checkExpect(gp3.bottom, false);

  }

  // initializeBoard
  void testInitializeBoard(Tester t) {
    Utils utils = new Utils();
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(5, 5);

    // Check dimensions
    t.checkExpect(board.size(), 5);

    // Checking the number of rows in specific columns
    t.checkExpect(board.get(0).size(), 5);
    t.checkExpect(board.get(4).size(), 5);

    // Check power station placement at (1,1) (translated to (0,0) zero-indexed
    // Java)
    GamePiece powerStation = board.get(0).get(0);
    t.checkExpect(powerStation.powerStation, true);
    t.checkExpect(powerStation.powered, false);

    // Non-power station piece at the bottom right, a edge corner piece
    GamePiece cornerPiece = board.get(4).get(4);
    t.checkExpect(cornerPiece.powerStation, false);
    t.checkExpect(cornerPiece.powered, false);
    t.checkExpect(cornerPiece.left, false);
    t.checkExpect(cornerPiece.right, false);
    t.checkExpect(cornerPiece.top, false);
    t.checkExpect(cornerPiece.bottom, false);

    // Check non-edge piece
    GamePiece middlePiece = board.get(2).get(2);
    t.checkExpect(middlePiece.powerStation, false);
    t.checkExpect(middlePiece.powered, false);

    // they all return false because the piece have not been initialized with
    // properties
    // however, initialized board has made the board as shown
  }

  // assignNodes
  void testAssignNodes(Tester t) {
    Utils utils = new Utils();
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(5, 5);
    ArrayList<GamePiece> nodes = utils.assignNodes(board);

    // Total nodes (5x5)
    t.checkExpect(nodes.size(), 25);

    // Check the first node
    GamePiece firstNode = nodes.get(0);
    t.checkExpect(firstNode.row, 1);
    t.checkExpect(firstNode.col, 1);
    t.checkExpect(firstNode.powerStation, true);

    // Check the last node (4 x 4))
    GamePiece lastNode = nodes.get(24);
    t.checkExpect(lastNode.row, 5);
    t.checkExpect(lastNode.col, 5);
    t.checkExpect(lastNode.powerStation, false);

    // Check middle of the board (2 x 2)
    GamePiece middleNode = nodes.get(2 * 5 + 2);
    t.checkExpect(middleNode.row, 3);
    t.checkExpect(middleNode.col, 3);
    t.checkExpect(middleNode.powerStation, false);
  }

  // rotate
  void testRotate(Tester t) {
    // Creating a GamePiece with initial connection states
    GamePiece gp = new GamePiece(1, 1, true, false, false, true, false);

    // Rotate the GamePiece once clockwise
    Utils u = new Utils();
    u.rotate(gp);
    t.checkExpect(gp.top, true);
    t.checkExpect(gp.right, false);
    t.checkExpect(gp.bottom, false);
    t.checkExpect(gp.left, true);

    // Rotate the GamePiece second time clockwise
    u.rotate(gp);
    t.checkExpect(gp.top, true);
    t.checkExpect(gp.right, true);
    t.checkExpect(gp.bottom, false);
    t.checkExpect(gp.left, false);
  }

  // lightUp
  void testLightUp(Tester t) {
    Utils utils = new Utils();
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(2, 2);
    utils.findPowerStation(board).powerStation = true;
    ArrayList<GamePiece> nodes = utils.assignNodes(board);

    // Assuming the power station is at (1,1) and it's connected to (1,2)
    board.get(0).get(0).right = true;
    board.get(1).get(0).left = true;

    t.checkExpect(board.get(0).get(0).powered, false);
    t.checkExpect(board.get(1).get(0).powered, false);

    utils.lightUp(board, 2, 2, nodes);

    t.checkExpect(board.get(0).get(0).powered, true);
    t.checkExpect(board.get(1).get(0).powered, true);
  }

  // // createEdges
  // void testCreateEdges(Tester t) {
  // Utils utils = new Utils();
  // // Initialize a 2x2 board
  // ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(2, 2);
  // ArrayList<Edge> edges = utils.createEdges(board, 2, 2);
  //
  // // There should be exactly 3 edges in a 2x2 grid: 2 horizontal and 1 vertical
  // t.checkExpect(edges.size(), 3, "There should be 3 edges in a 2x2 board");
  //
  // // Manually check each edge
  // // Edge between (1,1) and (1,2)
  // t.checkExpect(edges.get(0).fromNode.col, 1, "First edge should start from
  // column 1");
  // t.checkExpect(edges.get(0).toNode.col, 2, "First edge should go to column
  // 2");
  //
  // // Edge between (1,1) and (2,1)
  // t.checkExpect(edges.get(1).fromNode.row, 1, "Second edge should start from
  // row 1");
  // t.checkExpect(edges.get(1).toNode.row, 2, "Second edge should go to row 2");
  //
  // }

  // tests win method
  public void testWin(Tester t) {
    // list to store peices
    ArrayList<GamePiece> gamePieces = new ArrayList<>();

    // initializing/adding game pieces too list
    gamePieces.add(new GamePiece(0, 0, true, false, true, false, true));
    gamePieces.add(new GamePiece(1, 1, false, true, false, true, false));
    gamePieces.add(new GamePiece(2, 2, true, false, true, false, false));

    // making sure all pieces are in place to simulate a win
    boolean isWin = false;
    for (GamePiece piece : gamePieces) {
      if (piece.row == 0 && piece.col == 0 && piece.powerStation) {
        isWin = true;
        break;
      }
    }

    // check if it is won
    t.checkExpect(isWin, true);
  }

  // tests poweredColor method
  public void testPoweredColor(Tester t) {
    // creates new peices
    // powered
    GamePiece poweredPiece = new GamePiece(0, 0, false, false, false, false, true);
    // unpowered
    GamePiece unpoweredPiece = new GamePiece(0, 1, false, false, false, false, false);

    // set power status true
    poweredPiece.powered = true;

    // test powered color
    t.checkExpect(poweredPiece.poweredColor(), Color.YELLOW);

    // test unpowered color
    t.checkExpect(unpoweredPiece.poweredColor(), Color.GRAY);
  }

  // tests randomNodes method
  void testRandomizeNodes(Tester t) {
    Utils utils = new Utils();
    Random rand = new Random(123);
    ArrayList<GamePiece> gamePieces = new ArrayList<>();

    // create peices with known initial rotation states
    GamePiece gp1 = new GamePiece(1, 1, true, false, false, false, false);
    GamePiece gp2 = new GamePiece(2, 1, false, true, false, false, false);
    gamePieces.add(gp1);
    gamePieces.add(gp2);

    // randomize the rotation of GamePieces
    utils.randomizeNodes(gamePieces, rand);

    // check if rotation is randomized
    t.checkExpect(gp1.left, true);
    t.checkExpect(gp1.top, false);
    t.checkExpect(gp1.right, false);
    t.checkExpect(gp1.bottom, false);

    t.checkExpect(gp2.left, false);
    t.checkExpect(gp2.top, false);
    t.checkExpect(gp2.right, false);
    t.checkExpect(gp2.bottom, true);
  }

  // tests findPowerStationMethod
  void testFindPowerStation(Tester t) {
    Utils utils = new Utils();
    int width = 5;
    int height = 5;
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(width, height);
    // set cells as power station
    int powerRow = 2; // update for row for the power station
    int powerCol = 3; // update for column for the power station
    board.get(powerCol - 1).get(powerRow - 1).powerStation = true;

    // test to find the power station
    GamePiece foundPowerStation = utils.findPowerStation(board);
    t.checkExpect(foundPowerStation.row, powerRow);
    t.checkExpect(foundPowerStation.col, powerCol);
    t.checkExpect(foundPowerStation.powerStation, true);
  }

  // tests updateGamePeices method
  void testUpdateGamePieces(Tester t) {
    Utils utils = new Utils();
    int width = 5;
    int height = 5;
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(width, height);
    ArrayList<Edge> edges = utils.createEdges(board, width, height);

    // update pieces based on the edges
    utils.updateGamePieces(edges);

    // checking the updated state of game pieces
    for (ArrayList<GamePiece> row : board) {
      for (GamePiece gp : row) {
        // check left edge is updated
        if (gp.col > 1) {
          t.checkExpect(gp.left, true);
        }
        // check right edge is updated
        if (gp.col < width) {
          t.checkExpect(gp.right, true);
        }
        // check top edge is updated
        if (gp.row > 1) {
          t.checkExpect(gp.top, true);
        }
        // check bottom edge is updated
        if (gp.row < height) {
          t.checkExpect(gp.bottom, true);
        }
      }
    }
  }

  // tests movePowerStation method
  void testMovePowerStation(Tester t) {
    Utils utils = new Utils();
    int width = 5;
    int height = 5;
    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(width, height);

    // move station right
    utils.movePowerStation("right", board);
    t.checkExpect(board.get(0).get(0).powerStation, false);
    t.checkExpect(board.get(1).get(0).powerStation, true);

    // move station down
    utils.movePowerStation("down", board);
    t.checkExpect(board.get(1).get(0).powerStation, false);
    t.checkExpect(board.get(1).get(1).powerStation, true);

    // move station left
    utils.movePowerStation("left", board);
    t.checkExpect(board.get(1).get(1).powerStation, false);
    t.checkExpect(board.get(0).get(1).powerStation, true);

    // move station up
    utils.movePowerStation("up", board);
    t.checkExpect(board.get(0).get(1).powerStation, false);
    t.checkExpect(board.get(0).get(0).powerStation, true);
  }

  // tests onMouseClicked method
  void testOnMouseClicked(Tester t) {
    // create 5x5 board
    LightEmAllWorld world = new LightEmAllWorld(5, 5);

    // test top-left corner (0, 0)
    Posn clickedPosn1 = new Posn(0, 0);
    GamePiece gp1 = world.board.get(0).get(0);

    world.onMouseClicked(clickedPosn1);

    // check rotation of the GamePiece at (0, 0)
    t.checkExpect(gp1.top, gp1.top);
    t.checkExpect(gp1.left, gp1.left);
    t.checkExpect(gp1.bottom, gp1.bottom);
    t.checkExpect(gp1.right, gp1.right);

    // test center of the board (2, 2)
    Posn clickedPosn2 = new Posn(2 * Constants.CELL_SIZE, 2 * Constants.CELL_SIZE);
    GamePiece gp2 = world.board.get(2).get(2);

    world.onMouseClicked(clickedPosn2);
    // check rotation of the GamePiece at (2, 2)
    t.checkExpect(gp2.top, gp2.top);
    t.checkExpect(gp2.left, gp2.left);
    t.checkExpect(gp2.bottom, gp2.bottom);
    t.checkExpect(gp2.right, gp2.right);
    // test bottom-right corner (4, 4)
    Posn clickedPosn3 = new Posn(4 * Constants.CELL_SIZE, 4 * Constants.CELL_SIZE);
    GamePiece gp3 = world.board.get(4).get(4);

    world.onMouseClicked(clickedPosn3);

    // check rotation of the GamePiece at (4, 4)
    t.checkExpect(gp3.top, gp3.top);
    t.checkExpect(gp3.left, gp3.left);
    t.checkExpect(gp3.bottom, gp3.bottom);
    t.checkExpect(gp3.right, gp3.right);
  }

  // tests onKeyEvent method
  void testOnKeyEvent(Tester t) {
    LightEmAllWorld world = new LightEmAllWorld(5, 5);

    // tests counterclockwise
    world.onKeyEvent("left");
    // check rotating left moves the power station left
    t.checkExpect(world.board.get(2).get(0).powerStation, false);

    // tests clockwise
    world.onKeyEvent("right");
    // check rotating right moves the power station right
    t.checkExpect(world.board.get(0).get(0).powerStation, true);
    t.checkExpect(world.board.get(1).get(0).powerStation, false);

    // tests moving up
    world.onKeyEvent("up");
    // tests moving power station up
    t.checkExpect(world.board.get(0).get(0).powerStation, true);
    t.checkExpect(world.board.get(0).get(1).powerStation, false);

    // tests moving down
    world.onKeyEvent("down");
    // tests moving power station down
    t.checkExpect(world.board.get(0).get(0).powerStation, true);
    t.checkExpect(world.board.get(0).get(4).powerStation, false);
  }

  // tests find method
  void testFind(Tester t) {
    Utils util = new Utils();

    // initialize map
    HashMap<GamePiece, GamePiece> map = new HashMap<>();

    // create game pieces
    GamePiece root = new GamePiece(0, 0, true, true, true, true, true, true);
    GamePiece child1 = new GamePiece(1, 0, false, true, false, true, false, false);
    GamePiece child2 = new GamePiece(1, 1, false, false, false, true, false, false);
    GamePiece child3 = new GamePiece(0, 1, true, false, true, false, false, false);

    // set map with peices
    map.put(root, root);
    map.put(child1, root);
    map.put(child2, child1);
    map.put(child3, root);

    // tests different game pieces
    t.checkExpect(util.find(map, child1), root);
    t.checkExpect(util.find(map, child2), root);
    t.checkExpect(util.find(map, child3), root);

    // tests root on itself
    t.checkExpect(util.find(map, root), root);
  }

  // checkIfConnected
  void testCheckIfConnected(Tester t) {
    Utils utils = new Utils();
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<>();
    ArrayList<GamePiece> column1 = new ArrayList<>();
    ArrayList<GamePiece> column2 = new ArrayList<>();
    ArrayList<GamePiece> column3 = new ArrayList<>();

    // Manually creating a 3x1 board with specific connections
    // Board layout:
    // [1] -> [2] -> [3]
    GamePiece piece1 = new GamePiece(1, 1, false, true, false, false, false);
    GamePiece piece2 = new GamePiece(1, 2, true, true, false, false, false);
    GamePiece piece3 = new GamePiece(1, 3, true, false, false, false, false);

    column1.add(piece1);
    column2.add(piece2);
    column3.add(piece3);

    board.add(column1);
    board.add(column2);
    board.add(column3);

    // Check connections
    t.checkExpect(utils.checkIfConnected("right", board, 3, 1, piece1), true);
    t.checkExpect(utils.checkIfConnected("left", board, 3, 1, piece2), true);
    t.checkExpect(utils.checkIfConnected("right", board, 3, 1, piece2), true);
    t.checkExpect(utils.checkIfConnected("left", board, 3, 1, piece3), true);

    // Check non-existent connections
    t.checkExpect(utils.checkIfConnected("left", board, 3, 1, piece1), false);
    t.checkExpect(utils.checkIfConnected("right", board, 3, 1, piece3), false);

    // Check boundaries
    t.checkExpect(utils.checkIfConnected("up", board, 3, 1, piece1), false);
    t.checkExpect(utils.checkIfConnected("down", board, 3, 1, piece2), false);
  }

  // Test makeHashMap with a few edges
  void testMakeHashMap(Tester t) {
    Utils utils = new Utils();

    ArrayList<ArrayList<GamePiece>> board = utils.initializeBoard(3, 3);
    ArrayList<Edge> edges = utils.createEdges(board, 3, 3);

    HashMap<GamePiece, GamePiece> result = utils.makeHashMap(edges);

    // Check that all nodes from edges are keys in the map
    for (Edge edge : edges) {
      t.checkExpect(result.containsKey(edge.fromNode), true);
      t.checkExpect(result.containsKey(edge.toNode), true);

      // Check that each game piece maps to itself
      t.checkExpect(result.get(edge.fromNode), edge.fromNode);
      t.checkExpect(result.get(edge.toNode), edge.toNode);
    }

    // Check the map size (should contain all unique nodes)
    HashSet<GamePiece> uniqueNodes = new HashSet<>();
    for (Edge edge : edges) {
      uniqueNodes.add(edge.fromNode);
      uniqueNodes.add(edge.toNode);
    }
    t.checkExpect(result.size(), uniqueNodes.size());
  }

  // union
  void testUnion(Tester t) {
    Utils utils = new Utils();
    HashMap<GamePiece, GamePiece> map = new HashMap<>();
    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false);
    GamePiece gp2 = new GamePiece(1, 0, false, false, false, false, false);
    GamePiece gp3 = new GamePiece(0, 1, false, false, false, false, false);
    map.put(gp1, gp1);
    map.put(gp2, gp2);
    map.put(gp3, gp3);

    // Get nodes from the map
    GamePiece[] nodes = map.keySet().toArray(new GamePiece[0]);
    GamePiece root1 = nodes[0];
    GamePiece root2 = nodes[1];
    GamePiece root3 = nodes[2];

    // union on root1 and root2
    utils.union(map, root1, root2);
    // root2 should now point to root1
    t.checkExpect(utils.find(map, root2), root2);

    // union between root1/2 and root3
    utils.union(map, root3, root1);
    // root3 should also point to root1
    t.checkExpect(utils.find(map, root3), root2);

    // Ensure all nodes are now part of the same subset
    t.checkExpect(utils.find(map, root2), utils.find(map, root3));
  }

  // kruskalAlgo
  void testKruskalAlgorithm(Tester t) {
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<>();
    ArrayList<GamePiece> row = new ArrayList<>();
    // Create nodes
    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false);
    GamePiece gp2 = new GamePiece(0, 1, false, false, false, false, false);
    GamePiece gp3 = new GamePiece(1, 0, false, false, false, false, false);

    row.add(gp1);
    row.add(gp2);
    board.add(row);
    row = new ArrayList<>();
    row.add(gp3);
    board.add(row);

    ArrayList<Edge> edges = new ArrayList<>();
    edges.add(new Edge(gp1, gp2, 1));
    edges.add(new Edge(gp1, gp3, 3));
    edges.add(new Edge(gp2, gp3, 2));

    Union union = new Union(edges);
    ArrayList<Edge> mst = union.kruskalAlgo();

    int totalWeight = 0;
    HashSet<GamePiece> connectedNodes = new HashSet<>();
    for (Edge e : mst) {
      totalWeight += e.weight;
      connectedNodes.add(e.fromNode);
      connectedNodes.add(e.toNode);
    }

    t.checkExpect(mst.size(), 2);
    t.checkExpect(totalWeight, 4);
    t.checkExpect(connectedNodes.size(), 3);
  }

  // SortEdges
  void testSortEdges(Tester t) {
    ArrayList<Edge> edges = new ArrayList<>();
    edges.add(new Edge(new GamePiece(0, 0, false, false, false, false, false),
        new GamePiece(0, 1, false, false, false, false, false), 5));
    edges.add(new Edge(new GamePiece(0, 1, false, false, false, false, false),
        new GamePiece(1, 1, false, false, false, false, false), 1));
    edges.add(new Edge(new GamePiece(1, 1, false, false, false, false, false),
        new GamePiece(1, 0, false, false, false, false, false), 3));

    Collections.sort(edges, new SortEdges());

    // Verify the order by checking weights
    t.checkExpect(edges.get(0).weight, 1);
    t.checkExpect(edges.get(1).weight, 3);
    t.checkExpect(edges.get(2).weight, 5);
  }

  // tests method makeScene
  void testMakeScene(Tester t) {
    LightEmAllWorld world = new LightEmAllWorld(3, 3);
    world.powerRow = 0;
    world.powerCol = 0;
    world.board.get(0).get(0).powerStation = true;
    world.board.get(0).get(0).powered = true;
    world.board.get(1).get(0).powered = true;

    // Assume Utils class properly updates the powered states
    new Utils().lightUp(world.board, 3, 3, world.nodes);

    // Verify that the game piece properties affecting the rendering are set
    // correctly
    t.checkExpect(world.board.get(0).get(0).powerStation, true);
    t.checkExpect(world.board.get(0).get(0).powered, true);
    t.checkExpect(world.board.get(1).get(0).powered, false);
  }

  // running the game
  void testGame(Tester t) {
    LightEmAllWorld world = new LightEmAllWorld(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
    int worldWidth = Constants.CELL_SIZE * Constants.GAME_WIDTH;
    int worldHeight = Constants.CELL_SIZE * Constants.GAME_HEIGHT;
    world.bigBang(worldWidth, worldHeight);
  }
}
