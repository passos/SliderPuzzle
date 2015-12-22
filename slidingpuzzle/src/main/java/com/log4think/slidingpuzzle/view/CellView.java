package com.log4think.slidingpuzzle.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.log4think.slidingpuzzle.utils.Log;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class CellView extends ImageView {
  private static final String TAG = Log.tag(CellView.class);

  private boolean empty;
  private int index;
  private int col, row;

  public CellView(Context context) {
    super(context);
    init();
  }

  public CellView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CellView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    // if (!isInEditMode()) {
    // // setLayerType(View.LAYER_TYPE_HARDWARE, null);
    // setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    // }

    // setBackgroundColor(Utils.randColor());
  }

  // @Override
  // protected void onDraw(Canvas canvas) {
  // super.onDraw(canvas);
  //
  // }

  public boolean isAbove(CellView view) {
    return this.col == view.col && this.row < view.row;
  }

  public boolean isBelow(CellView view) {
    return this.col == view.col && this.row > view.row;
  }

  public boolean isToLeftOf(CellView view) {
    return this.col < view.col && this.row == view.row;
  }

  public boolean isToRightOf(CellView view) {
    return this.col > view.col && this.row == view.row;
  }

  public boolean isInSameAxis(CellView view) {
    return this.col == view.col || this.row == view.row;
  }

  public boolean isEmpty() {
    return empty;
  }

  public int getCol() {
    return col;
  }

  public int getRow() {
    return row;
  }

  public void setCoord(int col, int row) {
    Log.d(TAG, "cell[%d] coordinate [%d, %d] to [%d, %d]", index, this.col, this.row, col, row);
    this.col = col;
    this.row = row;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public void setEmpty(boolean empty) {
    this.empty = empty;
    if (empty) {
      setBackground(null);
      setImageAlpha(0);
    }
  }
}
