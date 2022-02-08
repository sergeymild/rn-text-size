package com.reactnativerandomvaluesjsihelper.textSize;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewParent;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UIManager;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.RootViewUtil;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.common.UIManagerType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RNTextSizeModule {
  private static final String TAG = "RNTextSize";
  private static final float SPACING_ADDITION = 0f;
  private static final float SPACING_MULTIPLIER = 1f;

  public static Context mReactContext;
  private static Handler handler = new Handler(Looper.getMainLooper());

  public static double[] measure(View view) {
    View rootView = (View) RootViewUtil.getRootView(view);
    if (rootView == null) {
      double[] result = new double[6];
      result[0] = -1234567;
      return result;
    }

    int[] buffer = new int[4];
    computeBoundingBox(rootView, buffer);
    int rootX = buffer[0];
    int rootY = buffer[1];
    computeBoundingBox(view, buffer);
    buffer[0] -= rootX;
    buffer[1] -= rootY;

    double[] result = new double[6];
    result[0] = result[1] = 0;
    for (int i = 2; i < 6; ++i) result[i] = PixelUtil.toDIPFromPixel(buffer[i - 2]);

    return result;
  }

  private static void computeBoundingBox(View view, int[] outputBuffer) {
    RectF boundingBox = new RectF();
    boundingBox.set(0, 0, view.getWidth(), view.getHeight());
    mapRectFromViewToWindowCoords(view, boundingBox);

    outputBuffer[0] = Math.round(boundingBox.left);
    outputBuffer[1] = Math.round(boundingBox.top);
    outputBuffer[2] = Math.round(boundingBox.right - boundingBox.left);
    outputBuffer[3] = Math.round(boundingBox.bottom - boundingBox.top);
  }

  private static void mapRectFromViewToWindowCoords(View view, RectF rect) {
    Matrix matrix = view.getMatrix();
    if (!matrix.isIdentity()) {
      matrix.mapRect(rect);
    }

    rect.offset(view.getLeft(), view.getTop());

    ViewParent parent = view.getParent();
    while (parent instanceof View) {
      View parentView = (View) parent;

      rect.offset(-parentView.getScrollX(), -parentView.getScrollY());

      matrix = parentView.getMatrix();
      if (!matrix.isIdentity()) {
        matrix.mapRect(rect);
      }

      rect.offset(parentView.getLeft(), parentView.getTop());

      parent = parentView.getParent();
    }
  }

  public static double[] measureView(double rawViewId) {
    int viewId = (int) rawViewId;
    UIManagerModule uiManager = ((ReactApplicationContext) mReactContext).getNativeModule(UIManagerModule.class);
    Semaphore semaphore = new Semaphore(0);
    final double[][] measure = new double[1][1];
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          View view = uiManager.resolveView(viewId);
          if (view != null) {
            measure[0] = measure(view);
          }
        } catch (IllegalViewOperationException e) {
          e.printStackTrace();
        }
        semaphore.release();
      }
    });

    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return measure[0];
  }

  /**
   * Based on ReactTextShadowNode.java
   */
  // {height, width, lineCount, lastLineWidth}
  @Nullable
  public static double[] measure(String t, @Nullable String fontFamily, double fs, double w) {
    WritableMap specs = Arguments.createMap();
    specs.putString("text", t);
    specs.putDouble("fontSize", fs);
    specs.putDouble("width", w);
    specs.putBoolean("usePreciseWidth", true);
    final RNTextSizeConf conf = getConf(specs, true);

    final String _text = conf.getString("text");
    if (_text == null) {
      return new double[0];
    }

    final float density = getCurrentDensity();
    final float width = conf.getWidth(density);
    final boolean includeFontPadding = conf.includeFontPadding;

    if (_text.isEmpty()) {
      return new double[] {minimalHeight(density, includeFontPadding), 0, 0, 0};
    }

    final SpannableString text = (SpannableString) RNTextSizeSpannedText
      .spannedFromSpecsAndText(mReactContext, conf, new SpannableString(_text));

    final TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    Layout layout = null;
    try {
      final BoringLayout.Metrics boring = BoringLayout.isBoring(text, textPaint);
      int hintWidth = (int) width;

      if (boring == null) {
        // Not boring, ie. the text is multiline or contains unicode characters.
        final float desiredWidth = Layout.getDesiredWidth(text, textPaint);
        if (desiredWidth <= width) {
          hintWidth = (int) Math.ceil(desiredWidth);
        }
      } else if (boring.width <= width) {
        // Single-line and width unknown or bigger than the width of the text.
        layout = BoringLayout.make(
          text,
          textPaint,
          boring.width,
          Layout.Alignment.ALIGN_NORMAL,
          SPACING_MULTIPLIER,
          SPACING_ADDITION,
          boring,
          includeFontPadding);
      }

      if (layout == null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, hintWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setBreakStrategy(conf.getTextBreakStrategy())
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
            .setIncludePad(includeFontPadding)
            .setLineSpacing(SPACING_ADDITION, SPACING_MULTIPLIER)
            .build();
        } else {
          layout = new StaticLayout(
            text,
            textPaint,
            hintWidth,
            Layout.Alignment.ALIGN_NORMAL,
            SPACING_MULTIPLIER,
            SPACING_ADDITION,
            includeFontPadding
          );
        }
      }

      final double lineCount = layout.getLineCount();
      double rectWidth;
      double lastLineWidth = 0.0;
      if (conf.getBooleanOrTrue("usePreciseWidth")) {
        float lastWidth = 0f;
        // Layout.getWidth() returns the configured max width, we must
        // go slow to get the used one (and with the text trimmed).
        rectWidth = 0f;
        for (int i = 0; i < lineCount; i++) {
          lastWidth = layout.getLineMax(i);
          if (lastWidth > rectWidth) {
            rectWidth = lastWidth;
          }
        }
        lastLineWidth = lastWidth / density;
      } else {
        rectWidth = layout.getWidth();
      }

      return new double[]{ layout.getHeight() / density, rectWidth / density, lineCount, lastLineWidth};
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  // ============================================================================
  //
  //      Non-exposed instance & static methods
  //
  // ============================================================================

  @Nullable
  private static RNTextSizeConf getConf(final ReadableMap specs, boolean forText) {
    if (specs == null) {
      return null;
    }
    return new RNTextSizeConf(specs, forText);
  }

  /**
   * RN consistently sets the height at 14dp divided by the density
   * plus 1 if includeFontPadding when text is empty, so we do the same.
   */
  private static double minimalHeight(final float density, final boolean includeFontPadding) {
    final double height = 14.0 / density;
    return includeFontPadding ? height + 1.0 : height;
  }

  /**
   * Retuns the current density.
   */
  @SuppressWarnings("deprecation")
  private static float getCurrentDensity() {
    return DisplayMetricsHolder.getWindowDisplayMetrics().density;
  }
}
