/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Font;

/** Font properties (like size and colors) for attributes labeling. */
public class AttributeGlobeFonts {

    private final int fontSize;
    private final String titleFontName;
    private final String valueFontName;
    private final String titleColorCode;
    private final String valueColorCode;

    private final Font titleFont;
    private final Font valueFont;
    private final Color titleColor;
    private final Color valueColor;

    /**
     * Main constructor.
     *
     * @param fontSize the font size for title and values
     * @param titleFontName font name for titles
     * @param valueFontName font name for values, can be null and the same font name as title will
     *     be used
     * @param titleColorCode color hex RGB code for titles
     * @param valueColorCode color hex RGB code for values. Can be null and the same color as titles
     *     will be used
     */
    public AttributeGlobeFonts(
            int fontSize,
            String titleFontName,
            String valueFontName,
            String titleColorCode,
            String valueColorCode) {
        this.fontSize = fontSize;
        this.titleFontName = requireNonNull(titleFontName);
        this.valueFontName = valueFontName;
        this.titleColorCode = requireNonNull(titleColorCode);
        this.valueColorCode = valueColorCode;
        this.titleFont = new Font(titleFontName, Font.BOLD, fontSize);
        this.valueFont =
                new Font(
                        valueFontName != null ? valueFontName : titleFontName,
                        Font.PLAIN,
                        fontSize);
        this.titleColor = Color.decode(titleColorCode);
        this.valueColor = Color.decode(valueColorCode != null ? valueColorCode : titleColorCode);
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getTitleFontName() {
        return titleFontName;
    }

    public String getValueFontName() {
        return valueFontName;
    }

    public String getTitleColorCode() {
        return titleColorCode;
    }

    public String getValueColorCode() {
        return valueColorCode;
    }

    public Font getTitleFont() {
        return titleFont;
    }

    public Font getValueFont() {
        return valueFont;
    }

    public Color getTitleColor() {
        return titleColor;
    }

    public Color getValueColor() {
        return valueColor;
    }

    @Override
    public String toString() {
        return "AttributeGlobeFonts [fontSize="
                + fontSize
                + ", titleFontName="
                + titleFontName
                + ", valueFontName="
                + valueFontName
                + ", titleColorCode="
                + titleColorCode
                + ", valueColorCode="
                + valueColorCode
                + ", titleFont="
                + titleFont
                + ", valueFont="
                + valueFont
                + ", titleColor="
                + titleColor
                + ", valueColor="
                + valueColor
                + "]";
    }

    /** Returns the default attributes font. */
    public static AttributeGlobeFonts getDefault() {
        return new AttributeGlobeFonts(14, "Dialog", null, "#004b91", "#595959");
    }
}
