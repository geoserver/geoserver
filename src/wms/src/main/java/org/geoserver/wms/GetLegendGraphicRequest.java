/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Holds the parsed parameters for a GetLegendGraphic WMS request.
 *
 * <p>The GET parameters of the GetLegendGraphic operation are defined as follows (from SLD 1.0
 * spec, ch.12):<br>
 *
 * <pre>
 * <table>
 *  <tr><td><b>Parameter</b></td><td><b>Required</b></td><td><b>Description</b></td></tr>
 *  <tr><td>VERSION </td><td>Required </td><td>Version as required by OGC interfaces.</td></tr>
 *  <tr><td>REQUEST </td><td>Required </td><td>Value must be  GetLegendRequest . </td></tr>
 *  <tr><td>LAYER </td><td>Required </td><td>Layer for which to produce legend graphic. A layergroup can be specified, too. In this case, STYLE and RULE parameters can have multiple values (separated by commas), one for each of the group layers.</td></tr>
 *  <tr><td>STYLE </td><td>Optional </td><td>Style of layer for which to produce legend graphic. If not present, the default style is selected. The style may be any valid style available for a layer, including non-SLD internally-defined styles. A list of styles separated by commas can be used to specify styles for single layers of a layergroup. To override default style only for some layers leave empty the not overridden ones in the list (ex. style1,,style3,style4 to use default style for layer 2).</td></tr>
 *  <tr><td>FEATURETYPE </td><td>Optional </td><td>Feature type for which to produce the legend graphic. This is not needed if the layer has only a single feature type. </td></tr>
 *  <tr><td>RULE </td><td>Optional </td><td>Rule of style to produce legend graphic for, if applicable. In the case that a style has multiple rules but no specific rule is selected, then the map server is obligated to produce a graphic that is representative of all of the rules of the style. A list of rules separated by commas can be used to specify rules for single layers of a layergroup. To specify rule only for some layers leave empty the not overridden ones in the list (ex. rule1,,rule3,rule4 to not specify rule for layer 2).</td></tr>
 *  <tr><td>SCALE </td><td>Optional </td><td>In the case that a RULE is not specified for a style, this parameter may assist the server in selecting a more appropriate representative graphic by eliminating internal rules that are outof- scope. This value is a standardized scale denominator, defined in Section 10.2</td></tr>
 *  <tr><td>SLD </td><td>Optional </td><td>This parameter specifies a reference to an external SLD document. It works in the same way as the SLD= parameter of the WMS GetMap operation. </td></tr>
 *  <tr><td>SLD_BODY </td><td>Optional </td><td>This parameter allows an SLD document to be included directly in an HTTP-GET request. It works in the same way as the SLD_BODY= parameter of the WMS GetMap operation.</td></tr>
 *  <tr><td>FORMAT </td><td>Required </td><td>This gives the MIME type of the file format in which to return the legend graphic. Allowed values are the same as for the FORMAT= parameter of the WMS GetMap request. </td></tr>
 *  <tr><td>WIDTH </td><td>Optional </td><td>This gives a hint for the width of the returned graphic in pixels. Vector-graphics can use this value as a hint for the level of detail to include. </td></tr>
 *  <tr><td>HEIGHT </td><td>Optional </td><td>This gives a hint for the height of the returned graphic in pixels. </td></tr>
 *  <tr><td>LANGUAGE </td><td>Optional </td><td>Permits to have internationalized text in legend output. </td></tr>
 *  <tr><td>EXCEPTIONS </td><td>Optional </td><td>This gives the MIME type of the format in which to return exceptions. Allowed values are the same as for the EXCEPTIONS= parameter of the WMS GetMap request.</td></tr>
 *  <tr><td>TRANSPARENT </td><td>Optional </td><td><code>true</code> if the legend image background should be transparent. Defaults to <code>false</code>.</td></tr>
 *  </table>
 * </pre>
 *
 * <p>There's also a custom {@code STRICT} parameter that defaults to {@code true} and controls
 * whether the mandatory parameters are to be checked. This is useful mainly to be able of
 * requesting a legend graphic for no layer in particular, so the LAYER parameter can be omitted.
 *
 * <p>The GetLegendGraphic operation itself is optional for an SLD-enabled WMS. It provides a
 * general mechanism for acquiring legend symbols, beyond the LegendURL reference of WMS
 * Capabilities. Servers supporting the GetLegendGraphic call might code LegendURL references as
 * GetLegendGraphic for interface consistency. Vendorspecific parameters may be added to
 * GetLegendGraphic requests and all of the usual OGC-interface options and rules apply. No XML-POST
 * method for GetLegendGraphic is presently defined.
 *
 * <p>In addition to the official parameters {@link #getLegendOptions()} is used to refining the
 * appearance of of the generated legend.
 *
 * <p>Finally as a data structure {@link GetLegendGraphic} is used to collect additional context.
 * Rendering environment {@link #getEnv()} and {@link #locale}. LayerInfo configuration settings are
 * available using methods like {@link #getTitle(Name)}.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetLegendGraphicRequest extends WMSRequest {

    /** Legend option to enable feature count matching */
    public static final String COUNT_MATCHED_KEY = "countMatched";

    /** Legend option to enable feature count matching */
    public static final String HIDE_EMPTY_RULES = "hideEmptyRules";

    /**
     * Details collected for an individual LegendGraphic including layer, title, style and optional
     * legend graphic.
     *
     * <p>This information is parsed from the GetLegendGraphicRequest and supplemented with layer
     * and style configuration as required. This information is provided as a data structure in
     * order to avoid duplicating logic in GetLegendGraphicKvpReader and
     * BufferedImageLegendGraphicBuild.
     *
     * <p>LegendRequest acts as simple data object with equality derived from layer name (enought to
     * allow it to behave well in a List). Note that LegendRequest is specific to a single {@link
     * GetLegendGraphicRequest} and should not be cached. It represents the state of the system at
     * the time of parsing.
     *
     * @author Jody Garnett (Boundless)
     */
    public static class LegendRequest {
        private String layer;
        private Name layerName;
        private FeatureType featureType;
        private String styleName;
        private String title;

        /** Optional rule used to refine presentation of style */
        private String rule;

        /** Style determined from a review of request parameters */
        private Style style;

        /** Optional legend info (from layer info or style info) */
        private LegendInfo legendInfo;

        /** Optional layer info (if available) */
        private LayerInfo layerInfo;

        /** Optional layer group info (if available ) */
        private LayerGroupInfo layerGroupInfo;

        /** LegendRequest for a style, no associated featureType. */
        public LegendRequest() {
            this.layer = "";
            this.featureType = null;
            this.layerName = new NameImpl("");
        }

        /**
         * LegendRequest for a feature type, additional details (title and legend graphic) provided
         * by MapLayerInfo.
         */
        public LegendRequest(FeatureType featureType) {
            if (featureType == null) {
                throw new NullPointerException("FeatureType required for LegendRequest");
            }
            this.featureType = featureType;
            this.layerName = featureType.getName();
        }

        /**
         * LegendRequest for a feature type, additional details (title and legend graphic) provided
         * by MapLayerInfo.
         *
         * @param layerName layerName distinct to featureType name
         */
        public LegendRequest(FeatureType featureType, Name layerName) {
            if (featureType == null) {
                throw new NullPointerException("FeatureType required for LegendRequest");
            }
            this.featureType = featureType;
            this.layerName = layerName;
        }

        public String getLayer() {
            return layer;
        }

        public void setLayer(String layerName) {
            this.layer = layerName;
        }

        public Name getLayerName() {
            return layerName;
        }

        public FeatureType getFeatureType() {
            return featureType;
        }

        public void setFeatureType(FeatureType featureType) {
            this.featureType = featureType;
        }

        public LayerGroupInfo getLayerGroupInfo() {
            return layerGroupInfo;
        }

        public void setLayerGroupInfo(LayerGroupInfo layerGroupInfo) {
            this.layerGroupInfo = layerGroupInfo;
        }

        public LayerInfo getLayerInfo() {
            return layerInfo;
        }

        public void setLayerInfo(LayerInfo layerInfo) {
            this.layerInfo = layerInfo;
        }
        /**
         * Optional rule name used when rendering this legend.
         *
         * @return rule name, or null if empty
         */
        public String getRule() {
            if ("".equals(rule)) {
                return null;
            }
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public String getStyleName() {
            return styleName;
        }

        public void setStyleName(String styleName) {
            this.styleName = styleName;
        }

        /**
         * Provided layer title, or layer name if not provided.
         *
         * <p>We choose a title with the following priority:
         *
         * <ul>
         *   <li>Layer Title
         *   <li>Layer Name - often obtained from native resource name
         * </ul>
         *
         * @return layer title if provided, or layer name as a default
         */
        public String getTitle() {
            if (title == null || "".equals(title)) {
                title = getLayerName().getLocalPart();
            }
            return title;
        }
        /**
         * Used to provide a legend title (from MapLayerInfo).
         *
         * <p>If the title is empty or null the layer name will be used.
         */
        public void setTitle(String title) {
            this.title = title;
        }
        /**
         * The Style object(s) for styling the legend graphic, or layer's default if not provided.
         * This style can be acquired by evaluating the STYLE parameter, which provides one of the
         * layer's named styles, the SLD parameter, which provides a URL for an external SLD
         * document, or the SLD_BODY parameter, which provides the SLD body in the request body.
         */
        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
            if (style != null) this.styleName = style.getName();
        }

        public LegendInfo getLegendInfo() {
            return legendInfo;
        }

        public void setLegendInfo(LegendInfo legendInfo) {
            this.legendInfo = legendInfo;
        }

        @Override
        public String toString() {
            return "LegendRequest [layer="
                    + layer
                    + ", name="
                    + layerName
                    + " styleName="
                    + styleName
                    + ", title="
                    + title
                    + ", legendInfo="
                    + legendInfo
                    + "]";
        }
    }

    public static final String SLD_VERSION = "1.0.0";

    /** default legend graphic width, in pixels, to apply if no WIDTH parameter was passed */
    public static final int DEFAULT_WIDTH = 20;

    /** default legend graphic height, in pixels, to apply if no WIDTH parameter was passed */
    public static final int DEFAULT_HEIGHT = 20;

    /**
     * The default image format in which to produce a legend graphic. Not really used when
     * performing user requests, since FORMAT is a mandatory parameter, but by now serves as a
     * default for expressing LegendURL layer attribute in GetCapabilities.
     */
    public static final String DEFAULT_FORMAT = "image/png";

    /** The featuretype(s) of the requested LAYER(s) */
    private List<LegendRequest> legends = new ArrayList<LegendRequest>();

    /**
     * should hold FEATURETYPE parameter value, though not used by now, since GeoServer WMS still
     * does not supports nested layers and layers has only a single feature type. This should change
     * in the future.
     */
    private String featureType;

    /**
     * holds the standarized scale denominator passed as the SCALE parameter value, or <code>-1.0
     * </code> if not provided
     */
    private double scale = -1d;

    /**
     * the mime type of the file format in which to return the legend graphic, as requested by the
     * FORMAT request parameter value.
     */
    private String format;

    /**
     * the width in pixels of the returned graphic, or <code>DEFAULT_WIDTH</code> if not provided
     */
    private int width = DEFAULT_WIDTH;

    /**
     * the height in pixels of the returned graphic, or <code>DEFAULT_HEIGHT</code> if not provided
     */
    private int height = DEFAULT_HEIGHT;

    /** mime type of the format in which to return exceptions information. */
    private String exceptionsFormat = GetMapRequest.SE_XML;

    /**
     * holds the geoserver-specific getLegendGraphic options for controlling things like the label
     * font, label font style, label font antialiasing, etc.
     */
    private Map<String, Object> legendOptions;

    /** Whether the legend graphic background shall be transparent or not. */
    private boolean transparent;

    private boolean strict = true;

    /** Optional locale to be used for text in output. */
    private Locale locale;

    /** Contains the parsed kvp items */
    private Map<String, Object> kvp;

    private WMS wms;

    public GetLegendGraphicRequest() {
        super("GetLegendGraphic");
        this.wms = WMS.get();
    }
    /**
     * Creates a new GetLegendGraphicRequest object.
     *
     * @param wms The WMS configuration object.
     */
    public GetLegendGraphicRequest(WMS wms) {
        super("GetLegendGraphic");
        this.wms = wms;
    }

    public String getExceptions() {
        return exceptionsFormat;
    }

    public void setExceptions(String exceptionsFormat) {
        this.exceptionsFormat = exceptionsFormat;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Legend details in order requested.
     *
     * @return legend in order requested
     */
    public List<LegendRequest> getLegends() {
        return legends;
    }

    /**
     * List of layer FeatureType in order requested. Helper for the monitoring module, which has to
     * work using reflection.
     *
     * @return layer FeatureType in order requested
     */
    public List<FeatureType> getLayers() {
        List<FeatureType> types = new ArrayList<>(legends.size());
        for (LegendRequest layer : legends) {
            FeatureType ft = layer.getFeatureType();
            if (ft != null) types.add(ft);
        }
        return types;
    }

    /**
     * Lookup LegendRequest by native FeatureType name.
     *
     * @return Matching LegendRequest
     */
    public LegendRequest getLegend(Name featureTypeName) {
        for (LegendRequest legend : legends) {
            if (featureTypeName.equals(legend.getLayerName())) {
                return legend;
            }
        }
        return null; // not found!
    }

    /** Used to clear {@link #legends} and configure with a feature type. */
    public void setLayer(FeatureType layer) {
        this.legends.clear();
        if (layer == null) {
            this.legends.add(new LegendRequest());
        } else {
            this.legends.add(new LegendRequest(layer));
        }
    }

    /** Shortcut used to set the rule for the first layer. */
    public void setRule(String rule) {
        // Will set rule for first LegendRequest
        if (!legends.isEmpty()) {
            legends.get(0).setRule(rule);
        }
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    /** Shortcut used to set the style for the first layer. */
    public void setStyle(Style style) {
        // this will set only the first LegendRequest
        if (legends.isEmpty()) {
            return;
        }
        legends.get(0).setStyle(style);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Returns the possibly empty set of key/value pair parameters to control some aspects of legend
     * generation.
     *
     * <p>These parameters are meant to be passed as the request parameter <code>"LEGEND_OPTIONS"
     * </code> with the format <code>LEGEND_OPTIONS=multiKey:val1,val2,val3;singleKey:val</code>.
     *
     * <p>The known options, all optional, are:
     *
     * <ul>
     *   <li><code>fontName</code>: name of the system font used for legend rule names. Defaults to
     *       "Sans-Serif"
     *   <li><code>fontStyle</code>: one of "plain", "italic" or "bold"
     *   <li><code>fontSize</code>: integer for the font size in pixels
     *   <li><code>fontColor</code>: a <code>String</code> that represents an opaque color as a
     *       24-bit integer
     *   <li><code>bgColor</code>: allows to override the legend background color
     *   <li><code>fontAntiAliasing</code>: a boolean indicating whether to use antia aliasing in
     *       font rendering. Anything of the following works: "yes", "true", "1". Anything else
     *       means false.
     *   <li><code>forceLabels</code>: "on" means labels will always be drawn, even if only one rule
     *       is available. "off" means labels will never be drawn, even if multiple rules are
     *       available.
     *   <li><code>forceTitles</code>: "off" means titles will never be drawn, even if multiple
     *       layers are available.
     *   <li><code>minSymbolSize</code>: a number defining the minimum size to be rendered for a
     *       symbol (defaults to 3).
     * </ul>
     *
     * @return Map<String,Object>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLegendOptions() {
        return (Map<String, Object>)
                (legendOptions == null ? Collections.emptyMap() : legendOptions);
    }

    /**
     * Sets the legend options parameters.
     *
     * @param legendOptions the key/value pair of legend options strings
     * @see #getLegendOptions()
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setLegendOptions(Map legendOptions) {
        this.legendOptions = legendOptions;
    }

    /**
     * Sets the value of the background transparency flag depending on the value of the <code>
     * TRANSPARENT</code> request parameter.
     *
     * @param transparentBackground whether the legend graphic background shall be transparent or
     *     not
     */
    public void setTransparent(boolean transparentBackground) {
        this.transparent = transparentBackground;
    }

    /**
     * Returns the value of the optional request parameter <code>TRANSPARENT</code>, which might be
     * either the literal <code>true</code> or <code>false</code> and specifies if the background of
     * the legend graphic to return shall be transparent or not.
     *
     * <p>If the <code>TRANSPARENT</code> parameter is not specified, this property defaults to
     * <code>false</code>.
     *
     * @return whether the legend graphic background shall be transparent or not
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Returns the value for the legacy {@code STRICT} parameter that controls whether LAYER is
     * actually required (if not, STYLE shall be provided)
     *
     * @return {@code true} by default, the value set thru {@link #setStrict(boolean)} otherwise
     */
    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /** SLD replacement */
    private Map<String, Object> env = new HashMap<String, Object>();

    /**
     * Map of strings that make up the SLD enviroment for variable substitution
     *
     * @return Map<String,Object>
     */
    public Map<String, Object> getEnv() {
        return env;
    }

    /** Sets the SLD environment substitution */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setEnv(Map enviroment) {
        this.env = enviroment;
    }

    /** Sets the optional Locale to be used for text in legend output. */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** Gets the locale to be used for text in output (null to use default locale). */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Parses and returns a legend option to a given type, if a value is present but cannot be
     * converted to the target type an exception will be thrown
     */
    public <T> T getLegendOption(String key, Class<T> optionClass) {
        if (legendOptions == null) {
            return null;
        }

        Object value = legendOptions.get(key);
        if (value == null) {
            return null;
        }

        T converted = Converters.convert(value, optionClass);
        if (converted == null) {
            throw new ServiceException(
                    "Invalid syntax for option "
                            + key
                            + ", cannot be convered to a "
                            + optionClass.getSimpleName());
        }
        return converted;
    }

    /** The parsed KVP map */
    public Map<String, Object> getKvp() {
        return kvp;
    }

    /** Sets the parsed KVP map */
    public void setKvp(Map<String, Object> kvp) {
        this.kvp = kvp;
    }

    public WMS getWms() {
        return wms;
    }

    public void setWms(WMS wms) {
        this.wms = wms;
    }
}
