package io.ytcode.pathfinding.astar;

import static io.ytcode.pathfinding.astar.Utils.check;
import static io.ytcode.pathfinding.astar.Utils.mask;

public class Grid {

  private static final int NODE_INFO_BITS = 12;
  private static final int NODE_INFO_MASK = mask(NODE_INFO_BITS);
  private static final int NODE_INFO_NULL = 0;
  private static final int NODE_INFO_CLOSED = NODE_INFO_MASK;

  private static final int NODE_PARENT_DIRECTION_BITS = 3; // 8方向
  private static final int NODE_PARENT_DIRECTION_MASK = mask(NODE_PARENT_DIRECTION_BITS);
  private static final int NODE_PARENT_DIRECTION_SHIFT = NODE_INFO_BITS;
  private static final int NODE_PARENT_DIRECTION_SHIFT_MASK =
      NODE_PARENT_DIRECTION_MASK << NODE_PARENT_DIRECTION_SHIFT;

  private static final int WALKABLE_BITS = 1;
  private static final int WALKABLE_MASK = mask(WALKABLE_BITS);
  private static final int WALKABLE_SHIFT = NODE_INFO_BITS + NODE_PARENT_DIRECTION_BITS;
  private static final int WALKABLE_SHIFT_MASK = WALKABLE_MASK << WALKABLE_SHIFT;
  private static final int WALKABLE_SHIFT_MASK_COMPLEMENT = ~WALKABLE_SHIFT_MASK;

  // const
  static final int DIRECTION_UP = 0;
  static final int DIRECTION_DOWN = 1;
  static final int DIRECTION_LEFT = 2;
  static final int DIRECTION_RIGHT = 3;
  static final int DIRECTION_LEFT_UP = 4;
  static final int DIRECTION_LEFT_DOWN = 5;
  static final int DIRECTION_RIGHT_UP = 6;
  static final int DIRECTION_RIGHT_DOWN = 7;

  static final int MAX_OPEN_NODE_SIZE = NODE_INFO_MASK - 1; // 全0全1都被用了

  // data
  private final short[][] grid;

  private final int width;
  private final int height;

  public Grid(int width, int height) {
    check(width > 0 && width <= Node.X_MASK);
    check(height > 0 && height <= Node.Y_MASK);
    this.grid = new short[width][height];
    this.width = width;
    this.height = height;
  }

  int nodeInfo(int x, int y) {
    return grid[x][y] & NODE_INFO_MASK;
  }

  void nodeInfoClosed(int x, int y) {
    grid[x][y] |= NODE_INFO_CLOSED;
  }

  void nodeInfoOpenNodeIdx(int x, int y, int idx) {
    assert idx >= 0 && idx < MAX_OPEN_NODE_SIZE;
    grid[x][y] = (short) (grid[x][y] & ~NODE_INFO_MASK | (idx + 1));
  }

  static boolean isNullNode(int info) {
    return info == NODE_INFO_NULL;
  }

  static boolean isClosedNode(int info) {
    return info == NODE_INFO_CLOSED;
  }

  static int openNodeIdx(int info) {
    assert info > 0 && info <= MAX_OPEN_NODE_SIZE;
    return info - 1;
  }

  void nodeParentDirectionUpdate(int x, int y, int d) {
    assert d >= 0 && d <= NODE_PARENT_DIRECTION_MASK;
    grid[x][y] =
        (short)
            (grid[x][y] & ~NODE_PARENT_DIRECTION_SHIFT_MASK | (d << NODE_PARENT_DIRECTION_SHIFT));
  }

  int nodeParentDirection(int x, int y) {
    return grid[x][y] >>> NODE_PARENT_DIRECTION_SHIFT & NODE_PARENT_DIRECTION_MASK;
  }

  void clear() {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        grid[i][j] &= ~NODE_INFO_MASK;
      }
    }
  }

  boolean isClean() {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        if (nodeInfo(i, j) != 0) {
          return false;
        }
      }
    }
    return true;
  }

  public void setWalkable(int x, int y, boolean flag) {
    if (flag) {
      grid[x][y] &= WALKABLE_SHIFT_MASK_COMPLEMENT;
    } else {
      grid[x][y] |= WALKABLE_SHIFT_MASK;
    }
  }

  public boolean isWalkable(int x, int y) {
    if (x < 0 || x >= width) {
      return false;
    }
    if (y < 0 || y >= height) {
      return false;
    }
    return (grid[x][y] & WALKABLE_SHIFT_MASK) == 0;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
