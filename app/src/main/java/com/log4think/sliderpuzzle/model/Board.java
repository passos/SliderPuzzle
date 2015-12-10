package com.log4think.sliderpuzzle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.log4think.sliderpuzzle.utils.Utils;
import com.log4think.sliderpuzzle.view.CellView;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class Board {
  private List<CellView> cellViews;
  private int colCount, rowCount;

  public Board(int colCount, int rowCount) {
    this.colCount = colCount;
    this.rowCount = rowCount;
    this.cellViews = new ArrayList<CellView>();
  }

  public int getColCount() {
    return colCount;
  }

  public int getRowCount() {
    return rowCount;
  }

  public void addView(CellView view) {
    view.setIndex(cellViews.size());
    view.setCoord(view.getIndex() % colCount, view.getIndex() / colCount);
    cellViews.add(view);
  }

  public void clearView() {
    cellViews.clear();
  }

  public List<CellView> getViews() {
    return cellViews;
  }

  public CellView getView(int i) {
    return cellViews.get(i);
  }

  public CellView getView(int col, int row) {
    for (CellView view : cellViews) {
      if (view.getCol() == col && view.getRow() == row) {
        return view;
      }
    }
    return null;
  }

  /**
   * randomise cells' view
   */
  public void shuffle() {
    Collections.shuffle(cellViews);
  }

  public CellView getEmptyView() {
    for (CellView view : cellViews) {
      if (view.isEmpty()) {
        return view;
      }
    }
    return null;
  }

  /**
   * @param view
   * @return all cells between specific view and empty view
   */
  public List<CellView> getCellsToEmptyView(CellView view) {
    List<CellView> result = new ArrayList<CellView>();

    CellView emptyView = getEmptyView();
    if (emptyView.isAbove(view)) {
      for (int i = view.getRow(); i > emptyView.getRow(); i--) {
        result.add(getView(view.getCol(), i));
      }
    } else if (emptyView.isBelow(view)) {
      for (int i = view.getRow(); i < emptyView.getRow(); i++) {
        result.add(getView(view.getCol(), i));
      }
    } else if (emptyView.isToLeftOf(view)) {
      for (int i = view.getCol(); i > emptyView.getCol(); i--) {
        result.add(getView(i, view.getRow()));
      }
    } else if (emptyView.isToRightOf(view)) {
      for (int i = view.getCol(); i < emptyView.getCol(); i++) {
        result.add(getView(i, view.getRow()));
      }
    }

    return result;
  }

  /**
   * @return if all cells in right position
   */
  public boolean isMatched() {
    for (CellView view : cellViews) {
      if (view.getCol() != view.getIndex() % colCount
          || view.getRow() != view.getIndex() / colCount) {
        return false;
      }
    }
    return true;
  }

  public Direction getEmptyCellDirection(CellView cellView) {
    CellView emptyView = getEmptyView();

    if (emptyView.isAbove(cellView)) {
      return new Direction(0, -1);
    } else if (emptyView.isBelow(cellView)) {
      return new Direction(0, 1);
    } else if (emptyView.isToLeftOf(cellView)) {
      return new Direction(-1, 0);
    } else if (emptyView.isToRightOf(cellView)) {
      return new Direction(1, 0);
    } else {
      return new Direction(0, 0);
    }
  }

  public void moveCells(List<CellView> cellViews, Direction direction) {
    for (CellView view : cellViews) {
      view.setCoord(view.getCol() + direction.x, view.getRow() + direction.y);
    }
  }

  /**
   * a class descrive the movement direction
   * x: -1 left, +1 right
   * y: -1 top, +1 bottom
   */
  public class Direction {
    public int x, y;

    public Direction(int x, int y) {
      this.x = Utils.signum(x);
      this.y = Utils.signum(y);
    }
  }
}
