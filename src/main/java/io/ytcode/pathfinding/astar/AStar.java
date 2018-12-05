package io.ytcode.pathfinding.astar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.ytcode.pathfinding.astar.Cost.COST_DIAGONAL;
import static io.ytcode.pathfinding.astar.Cost.COST_ORTHOGONAL;
import static io.ytcode.pathfinding.astar.Cost.hCost;
import static io.ytcode.pathfinding.astar.Grid.*;
import static io.ytcode.pathfinding.astar.Node.getF;
import static io.ytcode.pathfinding.astar.Node.getG;
import static io.ytcode.pathfinding.astar.Node.getX;
import static io.ytcode.pathfinding.astar.Node.getY;
import static io.ytcode.pathfinding.astar.Node.setGF;
import static io.ytcode.pathfinding.astar.Reachability.isReachable;
import static java.lang.Math.max;
import static java.lang.Math.min;

/** http://homepages.abdn.ac.uk/f.guerin/pages/teaching/CS1013/practicals/aStarTutorial.htm */
public class AStar {
  private static final Logger logger = LoggerFactory.getLogger(AStar.class);

  private final Nodes nodes;

  public AStar() {
    this.nodes = new Nodes();
  }

  public Path search(int sx, int sy, int ex, int ey, Grid map) {
    return search(sx, sy, ex, ey, map, false);
  }

  public Path search(int sx, int sy, int ex, int ey, Grid map, boolean smooth) {
    Path p = new Path();
    search(sx, sy, ex, ey, map, p, smooth);
    return p;
  }

  public void search(int sx, int sy, int ex, int ey, Grid map, Path path) {
    search(sx, sy, ex, ey, map, path, false);
  }

  public void search(int sx, int sy, int ex, int ey, Grid map, Path path, boolean smooth) {
    assert isCLean(map);
    if (!map.isWalkable(sx, sy)) {
      return;
    }

    if (!map.isWalkable(ex, ey)) {
      return;
    }

    if (sx == ex && sy == ey) {
      return;
    }

    path.clear();

    int endX = map.getWidth() - 1;
    int endY = map.getHeight() - 1;

    try {
      nodes.map = map; // 必须放在开始
      nodes.open(sx, sy, 0, hCost(sx, sy, ex, ey), DIRECTION_UP); // 起始点的方向不会被用到

      while (true) {
        long n = nodes.close();
        if (n == 0) {
          return;
        }

        int x = getX(n);
        int y = getY(n);

        if (x == ex && y == ey) {
          fillPath(ex, ey, sx, sy, path, map, smooth);
          return;
        }

        int pg = getG(n);

        int x1 = max(x - 1, 0);
        int x2 = min(x + 1, endX);
        int y1 = max(y - 1, 0);
        int y2 = min(y + 1, endY);

        open(x, y1, pg + COST_ORTHOGONAL, DIRECTION_UP, ex, ey, map);
        open(x, y2, pg + COST_ORTHOGONAL, DIRECTION_DOWN, ex, ey, map);
        open(x2, y, pg + COST_ORTHOGONAL, DIRECTION_LEFT, ex, ey, map);
        open(x1, y, pg + COST_ORTHOGONAL, DIRECTION_RIGHT, ex, ey, map);
        open(x2, y1, pg + COST_DIAGONAL, DIRECTION_LEFT_UP, ex, ey, map);
        open(x2, y2, pg + COST_DIAGONAL, DIRECTION_LEFT_DOWN, ex, ey, map);
        open(x1, y1, pg + COST_DIAGONAL, DIRECTION_RIGHT_UP, ex, ey, map);
        open(x1, y2, pg + COST_DIAGONAL, DIRECTION_RIGHT_DOWN, ex, ey, map);
      }
    } catch (Exception e) {
      logger.error("AStar.search: from {}-{} to {}-{}", sx, sy, ex, ey, e);
      path.clear();
    } finally {
      clear();
      assert isCLean(map);
    }
  }

  private void open(int x, int y, int g, int pd, int ex, int ey, Grid map) {
    int info = map.info(x, y);

    if (isUnwalkable(info)) {
      return;
    }

    /*
     * 方向是直角坐标系第一象限中的方向，即0,0点在左下角
     * 所以一个格子的坐下角点，左边，下边都属于这个格子，而上边，右边及右上角点都属于其他格子
     *
     * 假设在网格坐标系中，一个格子大小是100*100
     *
     * 点(0,0)是(0,0)格子的左下角点
     * 边(0,0-99)是(0,0)格子的左边
     * 边(0-99,0)是(0,0)格子的下边
     *
     * 点(100,100)是(1,1)格子的左下角点
     * 边(0-99,100)是(0,1)格子的下边
     * 边(100,0-99)是(1,0)格子的右边
     */
    switch (pd) {
      case DIRECTION_RIGHT_DOWN: // 往左上走
        if (!map.isWalkable(x + 1, y)) {
          return;
        }
        break;

      case DIRECTION_LEFT_UP: // 往右下走
        if (!map.isWalkable(x, y + 1)) {
          return;
        }
        break;
    }

    if (isNullNode(info)) {
      nodes.open(x, y, g, hCost(x, y, ex, ey), pd);
      return;
    }

    if (isClosedNode(info)) {
      return;
    }

    int idx = openNodeIdx(info);
    long n = nodes.getOpenNode(idx);

    int ng = getG(n);
    if (g >= ng) {
      return;
    }

    n = setGF(n, g, getF(n) - ng + g);
    nodes.openNodeParentChanged(n, idx, pd);
  }

  private void fillPath(int ex, int ey, int sx, int sy, Path path, Grid map, boolean smooth) {
    fillPath(ex, ey, path, map, smooth);
    int pd = map.nodeParentDirection(ex, ey);

    while (true) {
      switch (pd) {
        case DIRECTION_UP:
          ey++;
          break;

        case DIRECTION_DOWN:
          ey--;
          break;

        case DIRECTION_LEFT:
          ex--;
          break;

        case DIRECTION_RIGHT:
          ex++;
          break;

        case DIRECTION_LEFT_UP:
          ex--;
          ey++;
          break;

        case DIRECTION_LEFT_DOWN:
          ex--;
          ey--;
          break;

        case DIRECTION_RIGHT_UP:
          ex++;
          ey++;
          break;

        case DIRECTION_RIGHT_DOWN:
          ex++;
          ey--;
          break;

        default:
          throw new RuntimeException("illegal direction: " + pd);
      }

      if (ex == sx && ey == sy) {
        fillPath(ex, ey, path, map, smooth);
        return;
      }

      int ppd = map.nodeParentDirection(ex, ey);
      if (ppd != pd) {
        fillPath(ex, ey, path, map, smooth);
        pd = ppd;
      }
    }
  }

  private void fillPath(int x, int y, Path path, Grid map, boolean smooth) {
    if (!smooth) {
      path.add(x, y);
      return;
    }

    while (path.size() >= 2) {
      long p = path.get(1); // 这个点到上一个点是直达的，所以只用从上上个点开始
      int x2 = Point.getX(p);
      int y2 = Point.getY(p);

      // 即使到这个点不可达，但到其父节点还是有可能是可达的，这里直接结束是考虑性能和概率问题
      if (!isReachable(x, y, x2, y2, map)) {
        path.add(x, y);
        return;
      }

      path.remove();
    }
    path.add(x, y);
  }

  private void clear() {
    nodes.clear();
  }

  private boolean isCLean(Grid map) { // for test
    return nodes.isClean() && map.isClean();
  }
}
