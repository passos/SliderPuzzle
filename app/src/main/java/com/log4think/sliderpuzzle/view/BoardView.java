package com.log4think.sliderpuzzle.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.log4think.sliderpuzzle.R;
import com.log4think.sliderpuzzle.utils.Log;
import com.log4think.sliderpuzzle.utils.Utils;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class BoardView extends ViewGroup implements View.OnTouchListener {
  private static final String TAG = Log.tag(BoardView.class);

  private int childWidth, childHeight;
  private int cellPadding;
  private int colCount, rowCount;

  private List<CellView> cellViews;

  private PointF lastDragPoint;
  private Direction direction;
  private List<CellView> capturedViews;
  private CellView emptyView;

  public BoardView(Context context) {
    super(context);
    init();
  }

  public BoardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    childWidth = 0;
    childHeight = 0;
    colCount = 0;
    rowCount = 0;
    cellViews = new ArrayList<CellView>();
    capturedViews = new ArrayList<CellView>();
  }

  public void setBoardSize(int colCount, int rowCount) {
    this.colCount = colCount;
    this.rowCount = rowCount;
    reset();
  }

  private void reset() {
    // load image
    List<Bitmap> slices = Utils.sliceBitmap(getContext(), R.drawable.globe, colCount, rowCount);

    // generate sliced cell views
    cellViews.clear();
    for (int i = 0; i < slices.size(); i++) {
      CellView view = new CellView(getContext());
      view.setOnTouchListener(this);
      view.setImageBitmap(slices.get(i));
      view.setIndex(cellViews.size());
      view.setCoord(view.getIndex() % colCount, view.getIndex() / colCount);
      cellViews.add(view);
    }

    // set the last view to empty space
    if (cellViews.size() > 0) {
      emptyView = cellViews.get(cellViews.size() - 1);
      emptyView.setEmpty(true);
    }
    slices.clear();

    // shuffle the cells
    Collections.shuffle(cellViews);

    // add views to UI
    removeAllViews();
    for (View cell : cellViews) {
      addView(cell);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int rw = MeasureSpec.getSize(widthMeasureSpec);
    int rh = MeasureSpec.getSize(heightMeasureSpec);

    if (colCount * rowCount == 0) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    childWidth = (rw - getPaddingLeft() - getPaddingRight()) / colCount;
    childHeight = (rh - getPaddingTop() - getPaddingBottom()) / rowCount;

    // make the board to square
    childWidth = Math.min(childWidth, childHeight);
    childHeight = Math.min(childWidth, childHeight);

    // re-calculate dimension
    int vw = childWidth * colCount + getPaddingLeft() + getPaddingRight();
    int vh = childHeight * rowCount + getPaddingTop() + getPaddingBottom();
    setMeasuredDimension(vw, vh);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    for (CellView view : cellViews) {
      view.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);

      Point p = calculateCellViewPosition(view);
      view.layout(p.x, p.y, p.x + childWidth, p.y + childHeight);
    }
  }

  /**
   * the touch event handler of cell view
   * 
   * @param v cell view
   * @param event
   * @return
   */
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    CellView view = (CellView) v;
    if (view.isEmpty() || !view.isInSameAxis(emptyView)) {
      return false;
    }

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        capturedViews = getCellsToEmptyView(view);
        direction = getEmptyCellDirection(view);
        break;

      case MotionEvent.ACTION_UP:
        if (getMovedDelta() > childWidth / 2 || getMovedDelta() > childHeight / 2 // moved half way
            || getMovedDelta() < 5 // click
        ) {
          // move empty space to touched cell
          emptyView.setCoord(view.getCol(), view.getRow());

          // move all other captured cells to new place
          moveCells(capturedViews, direction);
        }

        // move cell views to right place
        animateMoveCells(capturedViews, 20);

        capturedViews.clear();
        capturedViews = null;
        lastDragPoint = null;
        direction = null;
        break;

      case MotionEvent.ACTION_MOVE:
        if (lastDragPoint != null) {
          moveCapturedCells(event);
        }
        lastDragPoint = new PointF(event.getRawX(), event.getRawY());
        break;
    }

    return true;
  }

  /**
   * @param view
   * @return calculate the position of the view
   */
  private Point calculateCellViewPosition(CellView view) {
    return new Point(getPaddingLeft() + view.getCol() * childWidth,
        getPaddingTop() + view.getRow() * childHeight);
  }

  /**
   * move the cells by animate
   * 
   * @param views
   * @param duration
   */
  private void animateMoveCells(List<CellView> views, int duration) {
    for (CellView view : views) {
      Point p = calculateCellViewPosition(view);
      view.animate().x(p.x).y(p.y).setDuration(duration);
    }
  }

  /**
   * move the captured cells with the touch event
   * 
   * @param event
   */
  private void moveCapturedCells(MotionEvent event) {
    if (capturedViews == null || capturedViews.size() == 0) {
      return;
    }

    float dx = event.getRawX() - lastDragPoint.x;
    float dy = event.getRawY() - lastDragPoint.y;

    CellView cellView = capturedViews.get(0);
    Point p = calculateCellViewPosition(cellView);

    if (direction.x != 0) {
      dy = 0;
      if (!Utils.isValueInRange(cellView.getX() + dx, p.x, p.x + direction.x * childWidth)) {
        return;
      }
    } else if (direction.y != 0) {
      dx = 0;
      if (!Utils.isValueInRange(cellView.getY() + dy, p.y, p.y + direction.y * childHeight)) {
        return;
      }
    }

    // set the cells position
    for (CellView view : capturedViews) {
      view.setX(view.getX() + dx);
      view.setY(view.getY() + dy);
    }
  }

  public CellView getEmptyView() {
    for (CellView view : cellViews) {
      if (view.isEmpty()) {
        return view;
      }
    }
    return null;
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

  private int getMovedDelta() {
    if (capturedViews != null && capturedViews.size() > 0) {
      CellView view = capturedViews.get(0);
      Point p = calculateCellViewPosition(view);
      int deltaX = (int) Math.abs(p.x - view.getX());
      int deltaY = (int) Math.abs(p.y - view.getY());

      if (deltaX != 0) {
        return deltaX;
      } else {
        return deltaY;
      }
    }
    return 0;
  }

  public void moveCells(List<CellView> cellViews, Direction direction) {
    for (CellView view : cellViews) {
      view.setCoord(view.getCol() + direction.x, view.getRow() + direction.y);
    }
  }

  public void setCellPadding(int cellPadding) {
    this.cellPadding = cellPadding;
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
