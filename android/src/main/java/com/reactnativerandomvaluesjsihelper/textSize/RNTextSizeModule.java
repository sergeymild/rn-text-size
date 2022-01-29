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
  public static ReadableMap measure(@Nullable final ReadableMap specs) {
    final RNTextSizeConf conf = getConf(specs, true);
    if (conf == null) {
      return null;
    }

    final String _text = conf.getString("text");
    if (_text == null) {
      return null;
    }

    final float density = getCurrentDensity();
    final float width = conf.getWidth(density);
    final boolean includeFontPadding = conf.includeFontPadding;

    final WritableMap result = Arguments.createMap();
    if (_text.isEmpty()) {
      result.putInt("width", 0);
      result.putDouble("height", minimalHeight(density, includeFontPadding));
      result.putInt("lastLineWidth", 0);
      result.putInt("lineCount", 0);
      return result;
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
      float rectWidth;

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
        result.putDouble("lastLineWidth", lastWidth / density);
      } else {
        rectWidth = layout.getWidth();
      }

      result.putDouble("width", Math.min(rectWidth / density, width));
      result.putDouble("height", layout.getHeight() / density);
      result.putInt("lineCount", lineCount);

      Integer lineInfoForLine = conf.getIntOrNull("lineInfoForLine");
      if (lineInfoForLine != null && lineInfoForLine >= 0) {
        final int line = Math.min(lineInfoForLine, lineCount);
        final WritableMap info = Arguments.createMap();
        info.putInt("line", line);
        info.putInt("start", layout.getLineStart(line));
        info.putInt("end", layout.getLineVisibleEnd(line));
        info.putDouble("bottom", layout.getLineBottom(line) / density);
        info.putDouble("width", layout.getLineMax(line) / density);
        result.putMap("lineInfo", info);
      }

      return result;
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
