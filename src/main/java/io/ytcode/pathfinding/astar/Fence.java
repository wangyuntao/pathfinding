package io.ytcode.pathfinding.astar;

public interface Fence {
  boolean isReachable(int x1, int y1, int x2, int y2);
}
