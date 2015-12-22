package com.log4think.slidingpuzzle.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.log4think.slidingpuzzle.R;
import com.log4think.slidingpuzzle.utils.Log;
import com.log4think.slidingpuzzle.utils.Utils;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class BoardView extends ViewGroup implements View.OnTouchListener {
  private static final String TAG = Log.tag(BoardView.class);
  private static final int MAX_SHUFFLE_STEPS = 50;
  private static final int CELL_ANIMATION_DURATION_SHORT = 100;

  private int childWidth, childHeight;
  private int cellPadding;
  private int colCount, rowCount;

  private List<CellView> cellViews;
  private Direction capturedDirection;
  private PointF lastDragPoint;
  private List<CellView> capturedViews;
  private CellView emptyView;
  private int activePointerId;
  private List<Direction> puzzlePath;

  private Handler handler = new Handler();
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

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
    colCount = 1;
    rowCount = 1;
    activePointerId = MotionEvent.INVALID_POINTER_ID;
    cellViews = new ArrayList<CellView>();
    capturedViews = new ArrayList<CellView>();
    puzzlePath = new ArrayList<Direction>();
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
      view.setIndex(i);
      view.setCoord(i % colCount, i / colCount);
      view.setImageBitmap(slices.get(i));
      cellViews.add(view);
    }

    // set the last view to empty space
    if (cellViews.size() > 0) {
      emptyView = cellViews.get(cellViews.size() - 1);
      emptyView.setEmpty(true);
    }
    slices.clear();

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

  private PointF getRawXY(View v, MotionEvent event) {
    return getRawXY(v, event, MotionEventCompat.getActionIndex(event));
  }

  private PointF getRawXY(View v, MotionEvent event, int pointerIndex) {
    final int location[] = {0, 0};
    v.getLocationOnScreen(location);
    if (pointerIndex < MotionEventCompat.getPointerCount(event)) {
      final float x = MotionEventCompat.getX(event, pointerIndex) + location[0];
      final float y = MotionEventCompat.getY(event, pointerIndex) + location[1];
      return new PointF(x, y);
    } else {
      return null;
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return !isEnabled();
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
    if (!isEnabled() && view.isEmpty() || !view.isInSameAxis(emptyView)) {
      return false;
    }

    final int action = MotionEventCompat.getActionMasked(event);

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
          capturedDirection = getEmptyCellDirection(view);
          capturedViews = getCellsToEmptyView(view);
          lastDragPoint = getRawXY(v, event);
          activePointerId = MotionEventCompat.getPointerId(event, 0);
        }
        break;
      }

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP: {
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
          break;
        }

        activePointerId = MotionEvent.INVALID_POINTER_ID;

        if (getMovedDelta() > childWidth / 2 || getMovedDelta() > childHeight / 2 // moved half way
            || getMovedDelta() < 5 // click
        ) {
          // move empty cell to touched cell
          Direction direction = getNegativeDirection(capturedDirection);
          for (int i = 0; i < capturedViews.size(); i++) {
            moveEmptyCell(direction);
            puzzlePath.add(direction);
          }

          // move all other captured cells to new place
          // moveCells(capturedViews, capturedDirection);
        } else {
          // move cell views to original place
          animateMoveCells(capturedViews, CELL_ANIMATION_DURATION_SHORT);
        }

        capturedDirection = null;
        capturedViews = null;
        lastDragPoint = null;
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
          break;
        }

        final int pointerIndex = MotionEventCompat.findPointerIndex(event, activePointerId);
        PointF p = null;
        try {
          p = getRawXY(v, event, pointerIndex);
        } catch (Exception e) {}
        if (p == null)
          break;

        if (lastDragPoint != null) {
          moveCapturedCells(p.x - lastDragPoint.x, p.y - lastDragPoint.y);
        }

        lastDragPoint = p;
        break;
      }


      case MotionEvent.ACTION_POINTER_UP: {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

        if (pointerId == activePointerId) {
          // choose another pointer
          final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
          if (newPointerIndex < MotionEventCompat.getPointerCount(event)) {
            lastDragPoint = getRawXY(v, event, newPointerIndex);
            activePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
          }
        }
        break;
      }
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
    if (views != null) {
      for (CellView view : views) {
        animateMoveCell(view, duration);
      }
    }
  }

  /**
   * move the cells by animate
   *
   * @param view
   * @param duration
   */
  private void animateMoveCell(CellView view, int duration) {
    Point p = calculateCellViewPosition(view);
    view.animate().x(p.x).y(p.y).setDuration(duration);
  }

  /**
   * move the captured cells with the touch event
   *
   * @param dx
   * @param dy
   */
  private void moveCapturedCells(float dx, float dy) {
    if (capturedViews == null || capturedViews.size() == 0) {
      return;
    }

    CellView cellView = capturedViews.get(0);
    Point p = calculateCellViewPosition(cellView);

    if (capturedDirection.x != 0) {
      dy = 0;
      if (!Utils.isValueInRange(cellView.getX() + dx, p.x, p.x + capturedDirection.x * childWidth)) {
        return;
      }
    } else if (capturedDirection.y != 0) {
      dx = 0;
      if (!Utils.isValueInRange(cellView.getY() + dy, p.y, p.y + capturedDirection.y * childHeight)) {
        return;
      }
    }

    // set the cells position
    for (CellView view : capturedViews) {
      view.setX(view.getX() + dx);
      view.setY(view.getY() + dy);
    }
  }

  private CellView getEmptyView() {
    for (CellView view : cellViews) {
      if (view.isEmpty()) {
        return view;
      }
    }
    return null;
  }

  private CellView getView(int col, int row) {
    if (col < 0 || col > colCount - 1 || row < 0 || row > rowCount - 1) {
      return null;
    }

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
  private List<CellView> getCellsToEmptyView(CellView view) {
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

  private Direction getEmptyCellDirection(CellView cellView) {
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
    if (capturedViews == null || capturedViews.size() == 0) {
      return 0;
    }

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

  private Direction getRandomDirection() {
    switch (Utils.randInt(4)) {
      case 0:
        return new Direction(0, 1);
      case 1:
        return new Direction(1, 0);
      case 2:
        return new Direction(0, -1);
      default:
        return new Direction(-1, 0);
    }
  }

  private Direction getNegativeDirection(Direction direction) {
    return new Direction(-direction.x, -direction.y);
  }

  private void moveCells(List<CellView> cellViews, Direction direction) {
    if (cellViews != null) {
      for (CellView view : cellViews) {
        view.setCoord(view.getCol() + direction.x, view.getRow() + direction.y);
      }
    }
  }

  public CellView moveEmptyCell(Direction direction) {
    if (emptyView == null) {
      return null;
    }

    int targetCol = emptyView.getCol() + direction.x;
    int targetRow = emptyView.getRow() + direction.y;

    CellView view = getView(targetCol, targetRow);
    if (view != null) {
      view.setCoord(emptyView.getCol(), emptyView.getRow());
      emptyView.setCoord(targetCol, targetRow);

      Point p = calculateCellViewPosition(emptyView);
      emptyView.layout(p.x, p.y, p.x + childWidth, p.y + childHeight);

      animateMoveCell(view, CELL_ANIMATION_DURATION_SHORT);

      if (onMovedListener != null) {
        onMovedListener.onMoved();
      }
    }
    return view;
  }

  public void setCellPadding(int cellPadding) {
    this.cellPadding = cellPadding;
  }

  /**
   * Shuffle the puzzle cells
   */
  public void shuffle() {
    if (!isEnabled() || colCount == 0 || rowCount == 0 || emptyView == null) {
      return;
    }

    setEnabled(false);

    Direction lastNegativeDirection = puzzlePath.isEmpty() ?
        new Direction(0, 0) : getNegativeDirection(puzzlePath.get(puzzlePath.size() - 1));

    int steps = Utils.randInt(MAX_SHUFFLE_STEPS) + 10;
    while (steps > 0) {
      Direction direction = getRandomDirection();
      while (direction.equals(lastNegativeDirection)) {
        direction = getRandomDirection();
      }
      CellView view = moveEmptyCell(direction);
      if (view != null) {
        puzzlePath.add(direction);
        lastNegativeDirection = getNegativeDirection(direction);
        steps--;
      }
    }

    setEnabled(true);
  }

  public void restore() {
    if (!isEnabled()) {
      return;
    }

    setEnabled(false);

    executorService.execute(
        new Runnable() {
          @Override
          public void run() {
            for (int i = puzzlePath.size() - 1; i >= 0; i--) {
              final Direction direction = getNegativeDirection(puzzlePath.get(i));
              puzzlePath.remove(i);

              handler.post(new Runnable() {
                @Override
                public void run() {
                  moveEmptyCell(direction);
                }
              });

              try {
                Thread.sleep(300);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }

            // finish
            puzzlePath.clear();
            handler.post(new Runnable() {
              @Override
              public void run() {
                setEnabled(true);
              }
            });
          }
        });
  }

  public int getPuzzleSteps() {
    return puzzlePath.size();
  }


  /**
   * a class used to describe the movement direction
   * x: -1 left, +1 right
   * y: -1 top, +1 bottom
   */
  public class Direction {
    public int x, y;

    public Direction(int x, int y) {
      this.x = Utils.signum(x);
      this.y = Utils.signum(y);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;

      if (o instanceof Direction) {
        Direction d = (Direction) o;
        return this.x == d.x && this.y == d.y;
      } else {
        return false;
      }
    }
  }

  public interface OnMovedListener {
    void onMoved();
  }

  public void setOnMovedListener(OnMovedListener onMovedListener) {
    this.onMovedListener = onMovedListener;
  }

  private OnMovedListener onMovedListener;

}
