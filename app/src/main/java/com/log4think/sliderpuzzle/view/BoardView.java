package com.log4think.sliderpuzzle.view;

import java.util.ArrayList;
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
import com.log4think.sliderpuzzle.model.Board;
import com.log4think.sliderpuzzle.utils.Log;
import com.log4think.sliderpuzzle.utils.Utils;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class BoardView extends ViewGroup implements View.OnTouchListener {
  private static final String TAG = Log.tag(BoardView.class);

  private int childWidth, childHeight;
  private int cellPadding;
  private PointF lastDragPoint;
  private Board.Direction direction;
  private List<CellView> capturedViews;
  private CellView emptyView;
  private Board board;

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
    capturedViews = new ArrayList<CellView>();
    board = new Board(0, 0);
  }

  public void setBoardSize(int colCount, int rowCount) {
    board = new Board(colCount, rowCount);
    reset();
  }

  private void reset() {
    // load image
    List<Bitmap> slices = Utils.sliceBitmap(getContext(), R.drawable.globe,
        board.getColCount(), board.getRowCount());

    // generate sliced cell views
    board.clearView();
    CellView view = null;
    for (int i = 0; i < slices.size(); i++) {
      view = new CellView(getContext());
      view.setOnTouchListener(this);
      view.setImageBitmap(slices.get(i));
      board.addView(view);
    }
    // the last view is empty space
    if (view != null) {
      view.setEmpty(true);
      emptyView = view;
    }
    slices.clear();

    board.shuffle();

    // add views to board
    removeAllViews();
    for (View cell : board.getViews()) {
      addView(cell);
    }
  }

  private Point getCellViewTopLeft(CellView view) {
    return new Point(getPaddingLeft() + view.getCol() * childWidth,
        getPaddingTop() + view.getRow() * childHeight);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    Log.d(TAG, "onMeasure(%d, %d)", widthMeasureSpec, heightMeasureSpec);

    int rw = MeasureSpec.getSize(widthMeasureSpec);
    int rh = MeasureSpec.getSize(heightMeasureSpec);

    if (board.getColCount() * board.getRowCount() == 0) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    childWidth = (rw - getPaddingLeft() - getPaddingRight()) / board.getColCount();
    childHeight = (rh - getPaddingTop() - getPaddingBottom()) / board.getRowCount();

    // make the board to square
    childWidth = Math.min(childWidth, childHeight);
    childHeight = Math.min(childWidth, childHeight);

    // re-calculate dimension
    int vw = childWidth * board.getColCount() + getPaddingLeft() + getPaddingRight();
    int vh = childHeight * board.getRowCount() + getPaddingTop() + getPaddingBottom();
    setMeasuredDimension(vw, vh);

    Log.d(TAG, "onMeasure(%d, %d)", vw, vh);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    Log.d(TAG, "onLayout(%s， %d, %d, %d, %d)", changed, l, t, r, b);

    for (CellView view : board.getViews()) {
      view.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);

      Point p = getCellViewTopLeft(view);
      view.layout(p.x, p.y, p.x + childWidth, p.y + childHeight);
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    CellView view = (CellView) v;
    if (view.isEmpty() || !view.isInSameAxis(emptyView)) {
      return false;
    }

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        capturedViews = board.getCellsToEmptyView(view);
        direction = board.getEmptyCellDirection(view);
        break;

      case MotionEvent.ACTION_UP:
        if (getMovedDelta() > childWidth / 2 || getMovedDelta() > childHeight / 2 // moved half way
            || getMovedDelta() < 5 // click
        ) {
          // move empty space to touched cell
          emptyView.setCoord(view.getCol(), view.getRow());

          // move all other captured cells to new place
          board.moveCells(capturedViews, direction);
        }

        // move cell views to right place
        // for (CellView cell : board.getViews()) {
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

  private void animateMoveCells(List<CellView> views, int duration) {
    for (CellView view : views) {
      Point p = getCellViewTopLeft(view);
      view.animate().x(p.x).y(p.y).setDuration(duration);
    }
  }

  private void moveCapturedCells(MotionEvent event) {
    if (capturedViews == null || capturedViews.size() == 0) {
      return;
    }

    float dx = event.getRawX() - lastDragPoint.x;
    float dy = event.getRawY() - lastDragPoint.y;

    CellView cellView = capturedViews.get(0);
    Point p = getCellViewTopLeft(cellView);

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

  private int getMovedDelta() {
    if (capturedViews != null && capturedViews.size() > 0) {
      CellView view = capturedViews.get(0);
      Point p = getCellViewTopLeft(view);
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

  public int getCellPadding() {
    return cellPadding;
  }

  public void setCellPadding(int cellPadding) {
    this.cellPadding = cellPadding;
  }
}
