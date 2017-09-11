/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.WMSLayer;
import org.geotools.map.WMTSMapLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.crs.WrappingProjectionHandler;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 
 * @see RawMapResponse
 */
public class OpenLayersMapOutputFormat implements GetMapOutputFormat {
    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(OpenLayersMapOutputFormat.class);

    /**
     * The mime type for the response header
     */
    public static final String MIME_TYPE = "text/html; subtype=openlayers";

    /**
     * System property name to toggle OL3 support.
     */
    public static final String ENABLE_OL3 = "ENABLE_OL3";

    /**
     * The formats accepted in a GetMap request for this producer and stated in getcaps
     */
    private static final Set<String> OUTPUT_FORMATS = new HashSet<String>(Arrays.asList(
            "application/openlayers", "openlayers", MIME_TYPE));
    
    /** 
     * Default capabilities for OpenLayers format.
     * 
     * <p>
     * <ol>
     *         <li>tiled = supported</li>
     *         <li>multipleValues = unsupported</li>
     *         <li>paletteSupported = supported</li>
     *         <li>transparency = supported</li>
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES= new MapProducerCapabilities(true, false, true, true, null);

    /**
     * Set of parameters that we can ignore, since they are not part of the OpenLayers WMS request
     */
    private static final Set<String> ignoredParameters;

    static {
        ignoredParameters = new HashSet<String>();
        ignoredParameters.add("REQUEST");
        ignoredParameters.add("TILED");
        ignoredParameters.add("BBOX");
        ignoredParameters.add("SERVICE");
        ignoredParameters.add("VERSION");
        ignoredParameters.add("FORMAT");
        ignoredParameters.add("WIDTH");
        ignoredParameters.add("HEIGHT");
        ignoredParameters.add("SRS");
    }

    /**
     * static freemaker configuration
     */
    private static Configuration cfg;

    static {
        cfg = new Configuration();
        cfg.setClassForTemplateLoading(OpenLayersMapOutputFormat.class, "");
        BeansWrapper bw = new BeansWrapper();
        bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        cfg.setObjectWrapper(bw);
    }

    /**
     * wms configuration
     */
    private WMS wms;

    public OpenLayersMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    public RawMap produceMap(WMSMapContent mapContent)
            throws ServiceException, IOException {
        try {
            // create the template
            String templateName;
            boolean useOpenLayers3 = isOL3Enabled(mapContent) && browserSupportsOL3(mapContent);
            if(useOpenLayers3) {
                templateName = "OpenLayers3MapTemplate.ftl";
            } else {
                templateName = "OpenLayers2MapTemplate.ftl";                
            }
            Template template = cfg.getTemplate(templateName);
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("context", mapContent);
            boolean hasOnlyCoverages = hasOnlyCoverages(mapContent);
            map.put("pureCoverage", hasOnlyCoverages);
            map.put("supportsFiltering", supportsFiltering(mapContent));
            map.put("styles", styleNames(mapContent));
            GetMapRequest request = mapContent.getRequest();
            map.put("request", request);
            map.put("yx", String.valueOf(isWms13FlippedCRS(request.getCrs())));
            map.put("maxResolution", new Double(getMaxResolution(mapContent.getRenderingArea())));
            ProjectionHandler handler = null;
            try {
                handler = ProjectionHandlerFinder.getHandler(
                        new ReferencedEnvelope(request.getCrs()), 
                        request.getCrs(), wms.isContinuousMapWrappingEnabled());
            } catch (MismatchedDimensionException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
            map.put("global", String.valueOf(
                    handler != null && handler instanceof WrappingProjectionHandler));

            String baseUrl = ResponseUtils.buildURL(request.getBaseUrl(), "/", null, URLType.RESOURCE);
            String queryString = null;
            // remove query string from baseUrl
            if (baseUrl.indexOf("?") > 0) {
                int idx = baseUrl.indexOf("?");
                queryString = baseUrl.substring(idx); // include question mark
                baseUrl = baseUrl.substring(0, idx); // leave out question mark
            }
            map.put("baseUrl", canonicUrl(baseUrl));

            // TODO: replace service path with call to buildURL since it does this
            // same dance
            String servicePath = "wms";
            if (LocalPublished.get() != null) {
                servicePath = LocalPublished.get().getName() + "/" + servicePath;
            }
            if (LocalWorkspace.get() != null) {
                servicePath = LocalWorkspace.get().getName() + "/" + servicePath;
            }
            // append query string to servicePath
            if (queryString != null) {
                servicePath += queryString;
            }
            map.put("servicePath", servicePath);

            map.put("parameters", getLayerParameter(request.getRawKvp()));
            map.put("units", useOpenLayers3 ? getOL3Units(request) : getOL2Units(request));

            if (mapContent.layers().size() == 1) {
                map.put("layerName", mapContent.layers().get(0).getTitle());
            } else {
                map.put("layerName", "Geoserver layers");
            }

            template.setOutputEncoding("UTF-8");
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            template.process(map, new OutputStreamWriter(buff, Charset.forName("UTF-8")));
            RawMap result = new RawMap(mapContent, buff, MIME_TYPE);
            return result;
        } catch (TemplateException e) {
            throw new ServiceException(e);
        }
    }

    private boolean isOL3Enabled(WMSMapContent mapContent) {
        GetMapRequest req = mapContent.getRequest();

        // check format options
        Object enableOL3 = Converters.convert(req.getFormatOptions().get(ENABLE_OL3), Boolean.class);
        if (enableOL3 == null) {
            // check system property
            enableOL3 = GeoServerExtensions.getProperty(ENABLE_OL3);
        }

        // enable by default
        return enableOL3 == null || Converters.convert(enableOL3, Boolean.class);
    }

    private boolean browserSupportsOL3(WMSMapContent mc) {
        String agent = mc.getRequest().getHttpRequestHeader("USER-AGENT");
        if(agent == null) {
            // play it safe
            return false;
        }
        
        Pattern MSIE_PATTERN = Pattern.compile("MSIE (\\d+)\\.");
        Matcher matcher = MSIE_PATTERN.matcher(agent);
        if(!matcher.matches()) {
            return true;
        } else {
            return Integer.valueOf(matcher.group(1)) > 8;
        }
    }

    private boolean isWms13FlippedCRS(CoordinateReferenceSystem crs) {
        try {
            String code = CRS.lookupIdentifier(crs, false);
            if (!code.contains("EPSG:")) {
                code = "EPGS:" + code;
            }
            code = WMS.toInternalSRS(code, WMS.version("1.3.0"));
            CoordinateReferenceSystem crs13 = CRS.decode(code);
            return CRS.getAxisOrder(crs13) == AxisOrder.NORTH_EAST;
        } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Failed to determine CRS axis order, assuming is EN", e);
            return false;
        }
    }

    /**
     * Guesses if the map context is made only of coverage layers by looking at the wrapping feature
     * type. Ugly, if you come up with better means of doing so, fix it.
     * 
     * @param mapContent
     *
     */
    private boolean hasOnlyCoverages(WMSMapContent mapContent) {
        for (Layer layer : mapContent.layers()) {
            FeatureType schema = layer.getFeatureSource().getSchema();
            boolean grid = schema.getName().getLocalPart().equals("GridCoverage")
                    && schema.getDescriptor("geom") != null && schema.getDescriptor("grid") != null
                    && !(layer instanceof WMSLayer)
                    && !(layer instanceof WMTSMapLayer);
            if (!grid)
                return false;
        }
        return true;
    }

    /**
     * Helper method that checks if filtering support should be activated.
     *
     * If the map contains at least one layer that is queryable, filtering should be activated.
     */
    private boolean supportsFiltering(WMSMapContent mapContent) {
        // returns TRUE if at least one layer supports filtering
        return mapContent.layers().stream().anyMatch(layer -> {
            if (layer instanceof FeatureLayer) {
                // vector layers support filtering
                return true;
            }
            if (!(layer instanceof GridReaderLayer)) {
                // filtering is not support for the remaining types
                return false;
            }
            // let's see if this coverage type supports filtering
            GeneralParameterValue[] readParams = ((GridReaderLayer) layer).getParams();
            if (readParams == null || readParams.length == 0) {
                // filtering is not supported
                return false;
            }
            for (GeneralParameterValue readParam : readParams) {
                if (readParam.getDescriptor().getName().getCode().equalsIgnoreCase("FILTER")) {
                    // the reader of this layer supports filtering
                    return true;
                }
            }
            // filtering is not supported
            return false;
        });
    }

    private List<String> styleNames(WMSMapContent mapContent) {
        if (mapContent.layers().size() != 1 || mapContent.getRequest() == null)
            return Collections.emptyList();

        MapLayerInfo info = mapContent.getRequest().getLayers().get(0);
        return info.getOtherStyleNames();
    }

    /**
     * OL does support only a limited number of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown. From the OL guide: possible values are "degrees" (or
     * "dd"), "m", "ft", "km", "mi", "inches".
     * 
     * @param request
     *
     */
    private String getOL2Units(GetMapRequest request) {
        CoordinateReferenceSystem crs = request.getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            final String degreeSign = "\u00B0";
            if (degreeSign.equals(unit) || "degrees".equals(unit) || "dd".equals(unit))
                result = "degrees";
            else if ("m".equals(unit) || "meters".equals(unit))
                result = "m";
            else if ("km".equals(unit) || "kilometers".equals(unit))
                result = "mi";
            else if ("in".equals(unit) || "inches".equals(unit))
                result = "inches";
            else if ("ft".equals(unit) || "feets".equals(unit))
                result = "ft";
            else if ("mi".equals(unit) || "miles".equals(unit))
                result = "mi";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }
    
    /**
     * OL3 does support a very limited set of unit types, we have to try and return one of those,
     * otherwise the scale won't be shown. 
     * 
     * @param request
     *
     */
    private String getOL3Units(GetMapRequest request) {
        CoordinateReferenceSystem crs = request.getCrs();
        // first rough approximation, meters for projected CRS, degrees for the
        // others
        String result = crs instanceof ProjectedCRS ? "m" : "degrees";
        try {
            String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
            // use the unicode escape sequence for the degree sign so its not
            // screwed up by different local encodings
            if ("ft".equals(unit) || "feets".equals(unit))
                result = "feet";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to determine unit of measure", e);
        }
        return result;
    }

    /**
     * Returns a list of maps with the name and value of each parameter that we have to forward to
     * OpenLayers. Forwarded parameters are all the provided ones, besides a short set contained in
     * {@link #ignoredParameters}.
     * 
     * 
     * 
     * @param rawKvp
     *
     */
    private List<Map<String, String>> getLayerParameter(Map<String, String> rawKvp) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>(rawKvp.size());

        for (Map.Entry<String, String> en : rawKvp.entrySet()) {
            String paramName = en.getKey();

            if (ignoredParameters.contains(paramName.toUpperCase())) {
                continue;
            }

            // this won't work for multi-valued parameters, but we have none so
            // far (they are common just in HTML forms...)
            Map<String, String> map = new HashMap<String, String>();
            map.put("name", paramName);
            map.put("value", en.getValue());
            result.add(map);
        }

        return result;
    }

    /**
     * Makes sure the url does not end with "/", otherwise we would have URL lik
     * "http://localhost:8080/geoserver//wms?LAYERS=..." and Jetty 6.1 won't digest them...
     * 
     * @param baseUrl
     *
     */
    private String canonicUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            return baseUrl;
        }
    }

    private double getMaxResolution(ReferencedEnvelope areaOfInterest) {
        double w = areaOfInterest.getWidth();
        double h = areaOfInterest.getHeight();

        return ((w > h) ? w : h) / 256;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }

}
