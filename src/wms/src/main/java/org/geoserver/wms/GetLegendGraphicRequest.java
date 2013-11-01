/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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

import org.geotools.styling.Style;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Holds the parsed parameters for a GetLegendGraphic WMS request.
 * 
 * <p>
 * The GET parameters of the GetLegendGraphic operation are defined as follows (from SLD 1.0 spec,
 * ch.12):<br>
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
 * </p>
 * <p>
 * There's also a custom {@code STRICT} parameter that defaults to {@code true} and controls whether
 * the mandatory parameters are to be checked. This is useful mainly to be able of requesting a
 * legend graphic for no layer in particular, so the LAYER parameter can be omitted.
 * </p>
 * <p>
 * The GetLegendGraphic operation itself is optional for an SLD-enabled WMS. It provides a general
 * mechanism for acquiring legend symbols, beyond the LegendURL reference of WMS Capabilities.
 * Servers supporting the GetLegendGraphic call might code LegendURL references as GetLegendGraphic
 * for interface consistency. Vendorspecific parameters may be added to GetLegendGraphic requests
 * and all of the usual OGC-interface options and rules apply. No XML-POST method for
 * GetLegendGraphic is presently defined.
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetLegendGraphicRequest extends WMSRequest {

    public static final String SLD_VERSION = "1.0.0";

    /**
     * default legend graphic width, in pixels, to apply if no WIDTH parameter was passed
     */
    public static final int DEFAULT_WIDTH = 20;

    /**
     * default legend graphic height, in pixels, to apply if no WIDTH parameter was passed
     */
    public static final int DEFAULT_HEIGHT = 20;

    /**
     * The default image format in which to produce a legend graphic. Not really used when
     * performing user requests, since FORMAT is a mandatory parameter, but by now serves as a
     * default for expressing LegendURL layer attribute in GetCapabilities.
     */
    public static final String DEFAULT_FORMAT = "image/png";

    /** The featuretype(s) of the requested LAYER(s) */
    private List<FeatureType> layers=new ArrayList<FeatureType>();
    
    /** The featuretype name -> title association map */
    private Map<Name,String> titles=new HashMap<Name,String>();

    /**
     * The Style object(s) for styling the legend graphic, or layer's default if not provided. This
     * style can be aquired by evaluating the STYLE parameter, which provides one of the layer's
     * named styles, the SLD parameter, which provides a URL for an external SLD document, or the
     * SLD_BODY parameter, which provides the SLD body in the request body.
     */
    private List<Style> styles=new ArrayList<Style>();

    /**
     * should hold FEATURETYPE parameter value, though not used by now, since GeoServer WMS still
     * does not supports nested layers and layers has only a single feature type. This should change
     * in the future.
     */
    private String featureType;

    /** holds RULE parameter value(s), or <code>null</code> if not provided */
    private List<String> rules=new ArrayList<String>();

    /**
     * holds the standarized scale denominator passed as the SCALE parameter value, or
     * <code>-1.0</code> if not provided
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
    private Map legendOptions;

    /**
     * Whether the legend graphic background shall be transparent or not.
     */
    private boolean transparent;

    private boolean strict = true;
    
    /**
     * Optional locale to be used for text in output.
     * 
     */
    private Locale locale;

    /**
     * Creates a new GetLegendGraphicRequest object.
     * 
     * @param wms
     *            The WMS configuration object.
     */
    public GetLegendGraphicRequest() {
        super("GetLegendGraphic");
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

    public List<FeatureType> getLayers() {
        return layers;
    }

    public void setLayers(List<FeatureType> layers) {
        this.layers = layers;
    }
    
    public void setTitle(Name featureTypeName,String title) {
        titles.put(featureTypeName, title);
    }
    
    public String getTitle(Name featureTypeName) {
        return titles.get(featureTypeName);
    }
    
    public void setLayer(FeatureType layer) {
    	this.layers.clear();
    	this.layers.add(layer);
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }
    
    public void setRule(String rule) {
        this.rules.clear();
        this.rules.add(rule);
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public List<Style> getStyles() {
        return styles;
    }

    public void setStyles(List<Style> styles) {
        this.styles = styles;
    }
    
    public void setStyle(Style style) {
    	this.styles.clear();
        this.styles.add(style);
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
     * <p>
     * These parameters are meant to be passed as the request parameter
     * <code>"LEGEND_OPTIONS"</code> with the format
     * <code>LEGEND_OPTIONS=multiKey:val1,val2,val3;singleKey:val</code>.
     * </p>
     * <p>
     * The known options, all optional, are:
     * <ul>
     * <li><code>fontName</code>: name of the system font used for legend rule names. Defaults to
     * "Sans-Serif"
     * <li><code>fontStyle</code>: one of "plain", "italic" or "bold"
     * <li><code>fontSize</code>: integer for the font size in pixels
     * <li><code>fontColor</code>: a <code>String</code> that represents an opaque color as a 24-bit
     * integer
     * <li><code>bgColor</code>: allows to override the legend background color
     * <li><code>fontAntiAliasing</code>: a boolean indicating whether to use antia aliasing in font
     * rendering. Anything of the following works: "yes", "true", "1". Anything else means false.
     * <li><code>forceLabels</code>: "on" means labels will always be drawn, even if only one rule
     * is available. "off" means labels will never be drawn, even if multiple rules are available.
     * <li><code>forceTitles</code>: "off" means titles will never be drawn, even if multiple layers
     * are available.
     * <li><code>minSymbolSize</code>: a number defining the minimum size to be rendered for a 
     * symbol (defaults to 3).
     * 
     * </ul>
     * </p>
     * 
     * @return
     */
    public Map getLegendOptions() {
        return legendOptions == null ? Collections.EMPTY_MAP : legendOptions;
    }

    /**
     * Sets the legend options parameters.
     * 
     * @param legendOptions
     *            the key/value pair of legend options strings
     * @see #getLegendOptions()
     */
    public void setLegendOptions(Map legendOptions) {
        this.legendOptions = legendOptions;
    }

    /**
     * Sets the value of the background transparency flag depending on the value of the
     * <code>TRANSPARENT</code> request parameter.
     * 
     * @param transparentBackground
     *            whether the legend graphic background shall be transparent or not
     */
    public void setTransparent(boolean transparentBackground) {
        this.transparent = transparentBackground;
    }

    /**
     * Returns the value of the optional request parameter <code>TRANSPARENT</code>, which might be
     * either the literal <code>true</code> or <code>false</code> and specifies if the background of
     * the legend graphic to return shall be transparent or not.
     * <p>
     * If the <code>TRANSPARENT</code> parameter is not specified, this property defaults to
     * <code>false</code>.
     * </p>
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
    private Map /* <String,Object> */env = new HashMap();

    

    /**
     * Map of strings that make up the SLD enviroment for variable substitution
     *
     * @return
     */
    public Map getEnv() {
        return env;
    }

    /**
     * Sets the SLD environment substitution
     *
     * @param enviroment
     */
    public void setEnv(Map enviroment) {
        this.env = enviroment;
    }

    /**
     * Sets the optional Locale to be used for text in legend output.
     * 
     * @param locale
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    
    /**
     * Gets the locale to be used for text in output
     * (null to use default locale).
     * @return
     */
    public Locale getLocale() {
        return this.locale;
    }
}
