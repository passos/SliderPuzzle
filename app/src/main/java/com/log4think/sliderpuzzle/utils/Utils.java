package com.log4think.sliderpuzzle.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

/**
 * @author liujinyu <simon.jinyu.liu@gmail.com>
 */
public class Utils {

  public static int randInt(int maxValue) {
    return new Random().nextInt(maxValue);
  }

  public static int randColor() {
    return Color.rgb(randInt(255), randInt(255), randInt(255));
  }

  public static List<Bitmap> sliceBitmap(Context context, int resourceId, int colCount, int rowCount) {
    // Drawable globe = ContextCompat.getDrawable(context, resourceId);
    // if (globe == null) {
    // return Collections.emptyList();
    // }
    // Bitmap bitmap = ((BitmapDrawable) globe).getBitmap();

    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
    return sliceBitmap(bitmap, colCount, rowCount);
  }

  public static List<Bitmap> sliceBitmap(Bitmap image, int colCount, int rowCount) {
    int sliceWidth = image.getWidth() / colCount;
    int sliceHeight = image.getHeight() / rowCount;

    // slice as square
    sliceWidth = Math.min(sliceWidth, sliceHeight);
    sliceHeight = Math.min(sliceWidth, sliceHeight);

    List<Bitmap> slices = new ArrayList<Bitmap>();
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < colCount; col++) {
        int x = col * sliceWidth;
        int y = row * sliceHeight;
        Bitmap bitmap = Bitmap.createBitmap(image, x, y, sliceWidth, sliceHeight);
        slices.add(bitmap);
      }
    }

    return slices;
  }

  /**
   * return the sign of the x
   *
   * @param x
   * @return 1 if x > 0, -1 if x < 0, 0 if x is 0
   */
  public static int signum(int x) {
    return x > 0 ? 1 : (x < 0 ? -1 : 0);
  }

  public static int getValueInRange(int value, int x, int y) {
    if (value < Math.min(x, y))
      return x;
    if (value > Math.max(x, y))
      return y;
    return value;
  }

  public static boolean isValueInRange(float value, float x, float y) {
    return (value < Math.max(x, y)) && (value > Math.min(x, y));
  }
}
