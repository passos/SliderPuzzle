package com.log4think.sliderpuzzle.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.v4.widget.ViewDragHelper;
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
public class PuzzleView extends ViewGroup {
  private static final String TAG = Log.tag(PuzzleView.class);

  private int childWidth, childHeight;
  private int cellPadding;
  private int colCount, rowCount;

  private List<CellView> cellViews;

  private ViewDragHelper dragHelper;
  private PointF lastDragPoint;
  private Direction direction;
  private List<CellView> capturedViews;
  private CellView emptyView;

  public PuzzleView(Context context) {
    super(context);
    init();
  }

  public PuzzleView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PuzzleView(Context context, AttributeSet attrs, int defStyleAttr) {
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
    dragHelper = ViewDragHelper.create(this, 1.0f, dragHelperCallback);
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
      view.setImageBitmap(slices.get(i));
      view.setIndex(i);
      view.setCoord(i % colCount, i / colCount);
      cellViews.add(view);
    }

    // set the last view to empty space
    if (cellViews.size() > 0) {
      emptyView = cellViews.get(cellViews.size() - 1);
      emptyView.setEmpty(true);
    }
    slices.clear();

    // shuffle the cells
    // Collections.shuffle(cellViews);
    // for (int i = 0; i < cellViews.size(); i++) {
    // CellView view = cellViews.get(i);
    // view.setCoord(i % colCount, i / colCount);
    // }

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

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    return dragHelper.shouldInterceptTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    dragHelper.processTouchEvent(event);
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
      // view.layout(p.x, p.y, p.x + childWidth, p.y + childHeight);
    }
  }

  /**
   * move the captured cells with the touch event
   * 
   * @param dx, dy
   */
  private void moveCapturedCells(float dx, float dy) {
    if (capturedViews == null || capturedViews.size() == 0) {
      return;
    }

    // set the cells position
    for (CellView view : capturedViews) {
      view.setTranslationX(dx);
      view.setTranslationY(dy);
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

  private ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {
    @Override
    public boolean tryCaptureView(View child, int pointerId) {
      CellView view = (CellView) child;
      boolean result = !view.isEmpty() && view.isInSameAxis(emptyView);
      Log.d(TAG, "tryCaptureView (%d, %d): %s", view.getCol(), view.getRow(), result);
      return result;
    }

    @Override
    public void onViewCaptured(View capturedChild, int activePointerId) {
      CellView view = (CellView) capturedChild;

      capturedViews = getCellsToEmptyView(view);
      direction = getEmptyCellDirection(view);
      Log.d(TAG, "onViewCaptured: %d", capturedViews.size());
    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
      CellView view = (CellView) capturedViews.get(0);

      if (getMovedDelta() > childWidth / 2 || getMovedDelta() > childHeight / 2 // moved half way
          || getMovedDelta() < 5 // click
      ) {
        // move empty space to touched cell
        emptyView.setCoord(view.getCol(), view.getRow());

        // move all other captured cells to new place
        moveCells(capturedViews, direction);

        Log.d(TAG, "onViewReleased: half way or click");
      }

      // move cell views to right place
      Point p = calculateCellViewPosition(emptyView);
      emptyView.layout(p.x, p.y, p.x + childWidth, p.y + childHeight);
      animateMoveCells(capturedViews, 20);

      capturedViews.clear();
      capturedViews = null;
      lastDragPoint = null;
      direction = null;
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      Log.d(TAG, "onViewPositionChanged: lt(%d, %d) - xy(%.0f, %.0f)",
          changedView.getLeft(), changedView.getTop(), changedView.getX(), changedView.getY());
      moveCapturedCells(dx, dy);
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
      if (capturedViews == null || capturedViews.size() == 0 || direction.y == 0) {
        return child.getTop();
      }

      CellView cellView = (CellView) child;
      Point p = calculateCellViewPosition(cellView);

      Log.d(TAG, "clampViewPositionVertical: %d/%.1f ~ [%d, %d], direction: (%d, %d)", top,
          child.getY(), p.y, p.y + direction.y * childHeight, direction.x, direction.y);
      return Utils.getValueInRange(top, p.y, p.y + direction.y * childHeight);
    }

    @Override
    public int clampViewPositionHorizontal(View child, int left, int dx) {
      if (capturedViews == null || capturedViews.size() == 0 || direction.x == 0) {
        return child.getLeft();
      }

      CellView cellView = (CellView) child;
      Point p = calculateCellViewPosition(cellView);

      Log.d(TAG, "clampViewPositionHorizontal: %d ~ [%d - %d], direction(%d, %d)", left,
          p.x, p.x + direction.x * childWidth, direction.x, direction.y);
      return Utils.getValueInRange(left, p.x, p.x + direction.x * childWidth);
    }
  };
}
