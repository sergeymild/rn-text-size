package com.reactnativerandomvaluesjsihelper.textSize;

import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.DisplayMetricsHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class RNTextSizeModule {
    private static final float SPACING_ADDITION = 0f;
    private static final float SPACING_MULTIPLIER = 1f;

    private static final String E_MISSING_TEXT = "E_MISSING_TEXT";
    private static final String E_MISSING_PARAMETER = "E_MISSING_PARAMETER";
    private static final String E_UNKNOWN_ERROR = "E_UNKNOWN_ERROR";

    // It's important to pass the ANTI_ALIAS_FLAG flag to the constructor rather than setting it
    // later by calling setFlags. This is because the latter approach triggers a bug on Android 4.4.2.
    // The bug is that unicode emoticons aren't measured properly which causes text to be clipped.
    private static final TextPaint sTextPaintInstance = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);


    /**
     * Based on ReactTextShadowNode.java
     */
    @SuppressWarnings("unused")
    public static double measure(String text, double fontSize, double width) {


        final String _text = text;
        if (_text == null) return 14;

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
                            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
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
          WritableMap map = Arguments.createMap();
          map.putDouble("height", 14);
          return map;
        }
    }


    // ============================================================================
    //
    //      Non-exposed instance & static methods
    //
    // ============================================================================

    /**
     * RN consistently sets the height at 14dp divided by the density
     * plus 1 if includeFontPadding when text is empty, so we do the same.
     */
    private double minimalHeight(final float density, final boolean includeFontPadding) {
        final double height = 14.0 / density;
        return includeFontPadding ? height + 1.0 : height;
    }

    /**
     * This is for 'fontFromFontStyle', makes the minimal info required.
     * @param suffix The font variant
     * @param fontSize Font size in SP
     * @param letterSpacing Sugest this to user
     * @return map with specs
     */
    private WritableMap makeFontSpecs(String suffix, int fontSize, double letterSpacing, boolean upcase) {
        final WritableMap map = Arguments.createMap();
        final String roboto = "sans-serif";

        // In Android, the fontFamily determines the weight
        map.putString("fontFamily", suffix != null ? (roboto + suffix) : roboto);
        map.putInt("fontSize", fontSize);
        map.putDouble("letterSpacing", letterSpacing);
        map.putString("textTransform", "uppercase");

        return map;
    }

    private WritableMap makeFontSpecs(String suffix, int fontSize, double letterSpacing) {
        return makeFontSpecs(suffix, fontSize, letterSpacing, false);
    }

    @Nonnull
    private WritableMap fontInfoFromTypeface(
            @Nonnull final TextPaint textPaint,
            @Nonnull final Typeface typeface,
            @Nonnull final RNTextSizeConf conf
    ) {
        // Info is always in unscaled values
        final float density = getCurrentDensity();
        final Paint.FontMetrics metrics = new Paint.FontMetrics();
        final float lineHeight = textPaint.getFontMetrics(metrics);

        final WritableMap info = Arguments.createMap();
        info.putString("fontFamily", conf.getString("fontFamily"));
        info.putString("fontWeight", typeface.isBold() ? "bold" : "normal");
        info.putString("fontStyle", typeface.isItalic() ? "italic" : "normal");
        info.putDouble("fontSize", textPaint.getTextSize() / density);
        info.putDouble("leading", metrics.leading / density);
        info.putDouble("ascender", metrics.ascent / density);
        info.putDouble("descender", metrics.descent / density);
        info.putDouble("top", metrics.top / density);
        info.putDouble("bottom", metrics.bottom / density);
        info.putDouble("lineHeight", lineHeight / density);
        info.putInt("_hash", typeface.hashCode());
        return info;
    }

    /**
     * Retuns the current density.
     */
    @SuppressWarnings("deprecation")
    private static float getCurrentDensity() {
        return DisplayMetricsHolder.getWindowDisplayMetrics().density;
    }

    private static final String[] FILE_EXTENSIONS = {".ttf", ".otf"};
    private static final String FONTS_ASSET_PATH = "fonts";

    private String[] fontsInAssets = null;

    /**
     * Set the font names in assets/fonts into the target array.
     * @param destArr Target
     */
    private void getFontsInAssets(@Nonnull WritableArray destArr) {
        String[] srcArr = fontsInAssets;

        if (srcArr == null) {
            final AssetManager assetManager = mReactContext.getAssets();
            ArrayList<String> tmpArr = new ArrayList<>();

            if (assetManager != null) {
                try {
                    String[] list = assetManager.list(FONTS_ASSET_PATH);

                    if (list != null) {
                        for (String spec : list) {
                            addFamilyToArray(tmpArr, spec);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            Collections.sort(tmpArr, String.CASE_INSENSITIVE_ORDER);
            fontsInAssets = srcArr = tmpArr.toArray(new String[0]);
        }

        for (String name : srcArr) {
            destArr.pushString(name);
        }
    }

    private void addFamilyToArray(
            @Nonnull final List<String> outArr,
            @Nonnull final String spec
    ) {
        for (String ext : FILE_EXTENSIONS) {
            if (spec.endsWith(ext)) {
                final String name = spec.substring(0, spec.length() - ext.length());

                if (!outArr.contains(name)) {
                    outArr.add(name);
                }
                break;
            }
        }
    }
}
