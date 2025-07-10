/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geoserver.catalog.DimensionInfo.CUSTOM_DIM_PREFIX;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.media.jai.Interpolation;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Version;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Represents a WMS GetMap request. as a extension to the WMS spec 1.1.
 *
 * @author Gabriel Roldan
 * @author Simone Giannecchini
 * @version $Id$
 */
public class GetMapRequest extends WMSRequest implements Cloneable {

    static final Color DEFAULT_BG = Color.white;

    public static final String SE_XML = "SE_XML";

    /** set of mandatory request's parameters */
    private MandatoryParameters mandatoryParams = new MandatoryParameters();

    /** set of optionals request's parameters */
    private OptionalParameters optionalParams = new OptionalParameters();

    /** format options */
    private Map<String, Object> formatOptions = new CaseInsensitiveMap<>(new HashMap<>());

    /** SLD replacement */
    private Map<String, Object> env = new HashMap<>();

    /** sql view parameters */
    private List<Map<String, String>> viewParams = null;

    private Map<String, String> httpRequestHeaders;

    public GetMapRequest() {
        super("GetMap");
    }

    public Envelope getBbox() {
        return this.mandatoryParams.bbox;
    }

    public java.awt.Color getBgColor() {
        return this.optionalParams.bgColor;
    }

    /**
     * DJB: spec says SRS is *required*, so if they dont specify one, we should throw an error instead we use "NONE" -
     * which is no-projection. Previous behavior was to the WSG84 lat/long (4326)
     *
     * @return request CRS, or <code>null</code> if not set. TODO: make CRS manditory as for spec conformance
     */
    public CoordinateReferenceSystem getCrs() {
        return this.optionalParams.crs;
    }

    public String getSRS() {
        return this.optionalParams.srs;
    }

    public String getExceptions() {
        return this.optionalParams.exceptions;
    }

    public String getFormat() {
        return this.mandatoryParams.format;
    }

    /** Map of String,Object which contains kvp's which are specific to a particular output format. */
    public Map<String, Object> getFormatOptions() {
        return formatOptions == null ? Collections.emptyMap() : formatOptions;
    }

    /** Map of strings that make up the SLD enviroment for variable substitution */
    public Map<String, Object> getEnv() {
        return env;
    }

    /** Map of strings that contain the parameter values for SQL views */
    public List<Map<String, String>> getViewParams() {
        return viewParams;
    }

    public int getHeight() {
        return this.mandatoryParams.height;
    }

    /** @return the non null list of layers, may be empty */
    public List<MapLayerInfo> getLayers() {
        List<MapLayerInfo> layers = mandatoryParams.layers;
        return layers;
    }

    /**
     * Gets a list of the styles to be returned by the server.
     *
     * @return A list of {@link Style}
     */
    public List<Style> getStyles() {
        return this.mandatoryParams.styles;
    }

    /**
     * Gets a list of the interpolation methods to be returned by the server.
     *
     * @return A list of {@link Interpolation}
     */
    public List<Interpolation> getInterpolations() {
        return this.optionalParams.interpolationMethods;
    }

    /**
     * Gets the uri specified by the "SLD" parameter.
     *
     * <p>This parameter is an alias for "STYLE_URL".
     */
    public URI getSld() {
        return getStyleUrl();
    }

    /**
     * Gets the url specified by the "STYLE_URL" parameter.
     *
     * <p>This parameter is used to point to a remote style via url.
     */
    public URI getStyleUrl() {
        return this.optionalParams.styleUrl;
    }

    /**
     * Gets the string specified the "SLD_BODY" parameter.
     *
     * <p>This parameter is an alias for "STYLE_BODY".
     */
    public String getSldBody() {
        return getStyleBody();
    }

    /**
     * Gets the String specified by the "STYLE_BODY" parameter.
     *
     * <p>This parameter is used to directly supply a complete style in the request.
     */
    public String getStyleBody() {
        return this.optionalParams.styleBody;
    }

    /**
     * Gets the string specified by the "SLD_VERSION" parameter.
     *
     * <p>This parameter is an alias for "STYLE_VERSION".
     */
    public String getSldVersion() {
        return getStyleVersion();
    }

    /**
     * Gets the String specified by the "STYLE_VERSION" parameter.
     *
     * <p>This parameter is used to supply a version of the style language being specified. It only applies when the
     * style is being supplied directly in the request with one of the "STYLE_URL", "STYLE_BODY" parameters.
     */
    public String getStyleVersion() {
        return this.optionalParams.styleVersion;
    }

    /** Returns {@link #getStyleVersion()} as a Version object, or null if no version is set. */
    public Version styleVersion() {
        return getStyleVersion() != null ? new Version(getStyleVersion()) : null;
    }

    /** Gets the string specified by the "STYLE_FORMAT" parameter. */
    public String getStyleFormat() {
        return this.optionalParams.styleFormat;
    }

    /**
     * Gets the value of the "VALIDATESCHEMA" parameter which controls wether the value of the "SLD_BODY" paramter is
     * schema validated.
     */
    public Boolean getValidateSchema() {
        return this.optionalParams.validateSLD;
    }

    /**
     * Gets a list of the filters that will be applied to each layer before rendering
     *
     * @return A list of {@link Filter}.
     */
    public List<Filter> getFilter() {
        return this.optionalParams.filters;
    }

    /**
     * Gets a list of the cql filtesr that will be applied to each layer before rendering.
     *
     * @return A list of {@link Filter}.
     */
    public List<Filter> getCQLFilter() {
        return this.optionalParams.cqlFilters;
    }

    /**
     * Gets a list of the feature ids that will be used to filter each layer before rendering.
     *
     * @return A list of {@link String}.
     */
    public List<FeatureId> getFeatureId() {
        return this.optionalParams.featureIds;
    }

    public boolean isTransparent() {
        return this.optionalParams.transparent;
    }

    /**
     * <a href="http://wiki.osgeo.org/index.php/WMS_Tiling_Client_Recommendation">WMS-C specification</a> tiling hint
     */
    public boolean isTiled() {
        return this.optionalParams.tiled;
    }

    public Point2D getTilesOrigin() {
        return this.optionalParams.tilesOrigin;
    }

    public int getBuffer() {
        return this.optionalParams.buffer;
    }

    public IndexColorModel getPalette() {
        return this.optionalParams.icm;
    }

    public int getWidth() {
        return this.mandatoryParams.width;
    }

    /**
     * @return The time request parameter. The list may contain {@link Date} or {@link DateRange} objects, or null to
     *     indicate the default value
     */
    public List<Object> getTime() {
        return this.optionalParams.time;
    }

    /**
     * Returns the chosen elevations. The list may contain {@link Date} or {@link NumberRange} objects, or null to
     * indicate the default value
     */
    public List<Object> getElevation() {
        return this.optionalParams.elevation;
    }

    /** Returs the feature version optional parameter */
    public String getFeatureVersion() {
        return this.optionalParams.featureVersion;
    }

    /** Returns the remote OWS type */
    public String getRemoteOwsType() {
        return optionalParams.remoteOwsType;
    }

    /** Returs the remote OWS URL */
    public URL getRemoteOwsURL() {
        return optionalParams.remoteOwsURL;
    }

    public void setBbox(Envelope bbox) {
        this.mandatoryParams.bbox = bbox;
    }

    public void setBgColor(java.awt.Color bgColor) {
        this.optionalParams.bgColor = bgColor;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.optionalParams.crs = crs;
    }

    public void setSRS(String srs) {
        this.optionalParams.srs = srs;
    }

    public void setExceptions(String exceptions) {
        this.optionalParams.exceptions = exceptions;
    }

    /**
     * Sets the GetMap request value for the FORMAT parameter, which is the MIME type for the kind of image required.
     */
    public void setFormat(String format) {
        this.mandatoryParams.format = format;
    }

    /**
     * Sets the format options.
     *
     * @param formatOptions A map of String,Object
     * @see #getFormatOptions()
     */
    public void setFormatOptions(Map<String, Object> formatOptions) {
        this.formatOptions = formatOptions;
    }

    /** Sets the SLD environment substitution */
    public void setEnv(Map<String, Object> enviroment) {
        this.env = enviroment;
    }

    /** Sets the SQL views parameters */
    public void setViewParams(List<Map<String, String>> viewParams) {
        this.viewParams = viewParams;
    }

    public void setHeight(int height) {
        this.mandatoryParams.height = height;
    }

    public void setHeight(Integer height) {
        this.mandatoryParams.height = height.intValue();
    }

    public void setLayers(List<MapLayerInfo> layers) {
        this.mandatoryParams.layers = layers == null ? Collections.emptyList() : layers;
    }

    public void setStyles(List<Style> styles) {
        this.mandatoryParams.styles = styles == null ? Collections.emptyList() : new ArrayList<>(styles);
    }

    /** Sets interpolations methods for layers. */
    public void setInterpolations(List<Interpolation> interpolations) {
        this.optionalParams.interpolationMethods = interpolations == null ? Collections.emptyList() : interpolations;
    }

    /**
     * Style resource defined by the "SLD" parameter.
     *
     * <p>The style resource can be provided by either {@link #setSld(URI)} or {@link #setStyleUrl(URI)}.
     *
     * @param sld Style resource defined by "SLD" parameter.
     */
    public void setSld(URI sld) {
        setStyleUrl(sld);
    }

    /**
     * Style resource defined by the "STYLE_URL" parameter.
     *
     * <p>The style resource can be provided by either {@link #setSld(URI)} or {@link #setStyleUrl(URI)}.
     *
     * @param styleUri Style resource defined by "STYLE_URL" parameter.
     */
    public void setStyleUrl(URI styleUri) {
        this.optionalParams.styleUrl = styleUri;
    }

    /**
     * Style document defined by the "SLD_BODY" parameter.
     *
     * <p>The sld document contents can be provided by either {@link #setSldBody(String)} or
     * {@link #setStyleBody(String)}.
     *
     * @param sldBody Contents forming SLD document to use
     */
    public void setSldBody(String sldBody) {
        setStyleBody(sldBody);
    }

    /**
     * Style documented defined by the "STYLE_BODY" parameter.
     *
     * <p>The style document contents can be provided by either {@link #setSldBody(String)} or
     * {@link #setStyleBody(String)}.
     *
     * @param styleBody Contents forming style document to use
     */
    public void setStyleBody(String styleBody) {
        this.optionalParams.styleBody = styleBody;
    }

    /** Sets the string specified by the "SLD_VERSION" parameter */
    public void setSldVersion(String sldVersion) {
        setStyleVersion(sldVersion);
    }

    /** Sets the style document version specified by the "STYLE_VERSION" parameter. */
    public void setStyleVersion(String styleVersion) {
        this.optionalParams.styleVersion = styleVersion;
    }

    /** Sets the style document format specified by the "STYLE_FORMAT" parameter */
    public void setStyleFormat(String styleFormat) {
        this.optionalParams.styleFormat = styleFormat;
    }

    /**
     * Sets the flag to validate the style document.
     *
     * @param validateSLD Use true to enable style document validation
     */
    public void setValidateSchema(Boolean validateSLD) {
        this.optionalParams.validateSLD = validateSLD;
    }

    /**
     * Sets a list of filters, one for each layer
     *
     * @param filters A list of {@link Filter}.
     */
    public void setFilter(List<Filter> filters) {
        this.optionalParams.filters = filters;
    }

    /**
     * Sets a list of filters ( cql ), one for each layer.
     *
     * @param cqlFilters A list of {@link Filter}.
     */
    public void setCQLFilter(List<Filter> cqlFilters) {
        this.optionalParams.cqlFilters = cqlFilters;
    }

    /**
     * Sets a list of feature ids, one for each layer.
     *
     * @param featureIds A list of {@link String}.
     */
    public void setFeatureId(List<FeatureId> featureIds) {
        this.optionalParams.featureIds = featureIds;
    }

    public void setTransparent(boolean transparent) {
        this.optionalParams.transparent = transparent;
    }

    public void setTransparent(Boolean transparent) {
        this.optionalParams.transparent = (transparent != null) ? transparent.booleanValue() : false;
    }

    public void setBuffer(int buffer) {
        this.optionalParams.buffer = buffer;
    }

    public void setPalette(IndexColorModel icm) {
        this.optionalParams.icm = icm;
    }

    public void setBuffer(Integer buffer) {
        this.optionalParams.buffer = (buffer != null) ? buffer.intValue() : 0;
    }

    public void setTiled(boolean tiled) {
        this.optionalParams.tiled = tiled;
    }

    public void setTiled(Boolean tiled) {
        this.optionalParams.tiled = (tiled != null) ? tiled.booleanValue() : false;
    }

    public void setTilesOrigin(Point2D origin) {
        this.optionalParams.tilesOrigin = origin;
    }

    public void setWidth(int width) {
        this.mandatoryParams.width = width;
    }

    public void setWidth(Integer width) {
        this.mandatoryParams.width = width.intValue();
    }

    /** Sets the time request parameter (a list of Date or DateRange objects) */
    public void setTime(List<Object> time) {
        this.optionalParams.time = new ArrayList<>(time);
    }

    /** Sets the elevation request parameter. */
    public void setElevation(double elevation) {
        this.optionalParams.elevation = new ArrayList<>();
        this.optionalParams.elevation.add(elevation);
    }

    /** Sets the elevation set as a request parameter. */
    public void setElevation(List<Object> elevation) {
        this.optionalParams.elevation = new ArrayList<>(elevation);
    }

    /** Sets the feature version optional param */
    public void setFeatureVersion(String featureVersion) {
        this.optionalParams.featureVersion = featureVersion;
    }

    public void setRemoteOwsType(String remoteOwsType) {
        this.optionalParams.remoteOwsType = remoteOwsType;
    }

    public void setRemoteOwsURL(URL remoteOwsURL) {
        this.optionalParams.remoteOwsURL = remoteOwsURL;
    }

    /**
     * Sets the maximum number of features to fetch in this request.
     *
     * <p>This property only applies if the reqeust is for a vector layer.
     */
    public void setMaxFeatures(Integer maxFeatures) {
        this.optionalParams.maxFeatures = maxFeatures;
    }

    /** The maximum number of features to fetch in this request. */
    public Integer getMaxFeatures() {
        return this.optionalParams.maxFeatures;
    }

    /**
     * Sets the offset or start index at which to start returning features in the request.
     *
     * <p>It is used in conjunction with {@link #getMaxFeatures()} to page through a feature set. This property only
     * applies if the request is for a vector layer.
     */
    public void setStartIndex(Integer startIndex) {
        this.optionalParams.startIndex = startIndex;
    }

    /** The offset or start index at which to start returning features in the request. */
    public Integer getStartIndex() {
        return this.optionalParams.startIndex;
    }

    public double getAngle() {
        return this.optionalParams.angle;
    }

    /** Sets the map rotation */
    public void setAngle(double rotation) {
        this.optionalParams.angle = rotation;
    }

    public List<List<SortBy>> getSortBy() {
        return this.optionalParams.sortBy;
    }

    public List<SortBy[]> getSortByArrays() {
        if (this.optionalParams.sortBy == null) {
            return null;
        } else {
            return this.optionalParams.sortBy.stream()
                    .map(l -> l.toArray(new SortBy[l.size()]))
                    .collect(Collectors.toList());
        }
    }

    public void setSortBy(List<List<SortBy>> sortBy) {
        this.optionalParams.sortBy = sortBy;
    }

    public ScaleComputationMethod getScaleMethod() {
        return this.optionalParams.scaleMethod;
    }

    /** Sets the scale computation method ({@link ScaleComputationMethod#OGC} by default) */
    public void setScaleMethod(ScaleComputationMethod scaleMethod) {
        this.optionalParams.scaleMethod = scaleMethod;
    }

    /** @return the clip */
    public Geometry getClip() {
        return this.optionalParams.clip;
    }

    /** @param clip the clip to set */
    public void setClip(Geometry clip) {
        this.optionalParams.clip = clip;
    }

    private static class MandatoryParameters implements Cloneable {
        /** ordered list of requested layers */
        List<MapLayerInfo> layers = Collections.emptyList();

        /**
         * ordered list of requested layers' styles, in a one to one relationship with <code>layers
         * </code>
         */
        List<Style> styles = Collections.emptyList();

        Envelope bbox;

        int width;

        int height;

        String format;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    private static class OptionalParameters implements Cloneable {

        /** the map's background color requested, or the default (white) if not specified */
        Color bgColor = DEFAULT_BG;

        /** from SRS (1.1) or CRS (1.2) param */
        CoordinateReferenceSystem crs;

        /** EPSG code for the SRS */
        String srs;

        /** vendor extensions, allows to filter each layer with a user defined filter */
        List<Filter> filters;

        /** cql filters */
        List<Filter> cqlFilters;

        /** feature id filters */
        List<FeatureId> featureIds;

        /** Layer sorting */
        List<List<SortBy>> sortBy;

        String exceptions = SE_XML;

        boolean transparent = false;

        /**
         * Tiling hint, according to the <a
         * href="http://wiki.osgeo.org/index.php/WMS_Tiling_Client_Recommendation">WMS-C specification</a>
         */
        boolean tiled;

        /**
         * Temporary hack since finding a good tiling origin would require us to compute the bbox on the fly TODO:
         * remove this once we cache the real bbox of vector layers
         */
        public Point2D tilesOrigin;

        /** the rendering buffer, in pixels * */
        int buffer;

        /** The palette used for rendering, if any */
        IndexColorModel icm;

        /**
         * time parameter, a list since many pattern setup can be possible, see for example
         * http://mapserver.gis.umn.edu/docs/howto/wms_time_support/#time-patterns. Can contain {@link Date} or
         * {@link DateRange} objects.
         */
        List<Object> time = Collections.emptyList();

        /** elevation parameter, can also be a list, can contain {@link Double} or {@link NumberRange} */
        List<Object> elevation = Collections.emptyList();

        /** URI provided by STYLE_URL parameter or (SLD_URL parameter). */
        URI styleUrl;

        /** Style document provided by STYLE_BODY parameter (or SLD_BODY parameter). */
        String styleBody;

        /** STYLE_VERSION parameter */
        String styleVersion;

        /** STYLE_FORMAT parameter */
        String styleFormat = SLDHandler.FORMAT;

        /** flag to validate SLD parameter */
        Boolean validateSLD = Boolean.FALSE;

        /** feature version (for versioned requests) */
        String featureVersion;

        /** Remote OWS type */
        String remoteOwsType;

        /** Remote OWS url */
        URL remoteOwsURL;

        /** paging parameters */
        Integer maxFeatures;

        Integer startIndex;

        /** map rotation */
        double angle;

        /** scale computation method */
        ScaleComputationMethod scaleMethod;

        /** by layer interpolation methods * */
        List<Interpolation> interpolationMethods = Collections.emptyList();

        /** polgon wkt to clip WMS response * */
        Geometry clip;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * Standard override of toString()
     *
     * @return a String representation of this request.
     */
    @Override
    public String toString() {
        StringBuffer returnString = new StringBuffer("\nGetMap Request");
        returnString.append("\n version: " + version);
        returnString.append("\n output format: " + mandatoryParams.format);
        returnString.append("\n width height: " + mandatoryParams.width + "," + mandatoryParams.height);
        returnString.append("\n bbox: " + mandatoryParams.bbox);
        returnString.append("\n layers: ");

        for (Iterator<MapLayerInfo> i = mandatoryParams.layers.iterator(); i.hasNext(); ) {
            returnString.append(i.next().getName());
            if (i.hasNext()) {
                returnString.append(",");
            }
        }

        returnString.append("\n styles: ");

        for (Iterator<Style> it = mandatoryParams.styles.iterator(); it.hasNext(); ) {
            Style s = it.next();
            returnString.append(s.getName());

            if (it.hasNext()) {
                returnString.append(",");
            }
        }

        // returnString.append("\n inside: " + filter.toString());
        return returnString.toString();
    }

    public String getHttpRequestHeader(String headerName) {
        return httpRequestHeaders == null ? null : httpRequestHeaders.get(headerName);
    }

    public void putHttpRequestHeader(String headerName, String value) {
        if (httpRequestHeaders == null) {
            httpRequestHeaders = new CaseInsensitiveMap<>(new HashMap<>());
        }
        httpRequestHeaders.put(headerName, value);
    }

    @Override
    public Object clone() {
        try {
            GetMapRequest copy = (GetMapRequest) super.clone();
            copy.mandatoryParams = (MandatoryParameters) mandatoryParams.clone();
            copy.optionalParams = (OptionalParameters) optionalParams.clone();

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unexpected, could not clone GetMapRequest", e);
        }
    }

    public List<String> getCustomDimension(String dimensionName) {
        if (getRawKvp() != null) {
            String key = CUSTOM_DIM_PREFIX + dimensionName;
            Object value = getRawKvp().get(key);
            if (value instanceof String) {
                String s = (String) value;
                final ArrayList<String> values = new ArrayList<>(1);
                if (s.indexOf(",") > 0) {
                    String[] elements = s.split("\\s*,\\s*");
                    values.addAll(Arrays.asList(elements));
                } else {
                    values.add(s);
                }
                return values;
            }
        }

        return null;
    }
}
