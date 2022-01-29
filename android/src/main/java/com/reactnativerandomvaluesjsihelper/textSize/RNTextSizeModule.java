package com.reactnativerandomvaluesjsihelper.textSize;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.DisplayMetricsHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RNTextSizeModule {
  private static final String TAG = "RNTextSize";
  private static final float SPACING_ADDITION = 0f;
  private static final float SPACING_MULTIPLIER = 1f;

  public static Context mReactContext;

  /**
   * Based on ReactTextShadowNode.java
   */
  @SuppressWarnings("unused")
  @Nullable
  public static double[] measure(String t, double fs, double w) {
    WritableMap specs = Arguments.createMap();
    specs.putString("text", t);
    specs.putDouble("fontSize", fs);
    specs.putDouble("width", w);
    final RNTextSizeConf conf = getConf(specs, true);

    final String _text = conf.getString("text");
    if (_text == null) {
      return new double[0];
    }

    final float density = getCurrentDensity();
    final float width = conf.getWidth(density);
    final boolean includeFontPadding = conf.includeFontPadding;

    if (_text.isEmpty()) {
      return new double[] {minimalHeight(density, includeFontPadding), 0};
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

      final int lineCount = layout.getLineCount();

      return new double[]{ layout.getHeight() / density, lineCount};
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
