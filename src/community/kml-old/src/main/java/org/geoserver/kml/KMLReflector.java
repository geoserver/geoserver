/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSRequest;
import org.geoserver.wms.WMSRequests;
import org.geoserver.wms.WebMapService;

/**
 * KML reflecting service.
 * <p>
 * This
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * 
 */
public class KMLReflector {
    private static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.vfny.geoserver.wms.responses.map.kml");

    /** default 'format' value */
    public static final String FORMAT = KMLMapOutputFormat.MIME_TYPE;

    private static Map<String, Map<String, String>> MODES;

    static {
        Map temp = new HashMap();
        Map options;

        options = new HashMap();
        options.put("superoverlay", true);
        temp.put("superoverlay", options);

        options = new HashMap();
        options.put("superoverlay", false);
        options.put("regionatemode", null);
        options.put("kmscore", null);
        temp.put("download", options);

        options = new HashMap();
        options.put("superoverlay", false);
        temp.put("refresh", options);

        MODES = temp;
    }

    /**
     * web map service
     */
    WebMapService wms;

    /**
     * The WMS configuration
     */
    WMS wmsConfiguration;

    public KMLReflector(WebMapService wms, WMS wmsConfiguration) {
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;
    }

    // public void wms(GetMapRequest request, HttpServletResponse response) throws Exception {
    // doWms(request, response, wms, wmsConfiguration);
    // }

    public static org.geoserver.wms.WebMap doWms(GetMapRequest request, WebMapService wms,
            WMS wmsConfiguration) throws Exception {
        // set the content disposition
        StringBuffer filename = new StringBuffer();
        boolean containsRasterData = false;
        boolean isRegionatingFriendly = true;
        for (int i = 0; i < request.getLayers().size(); i++) {
            MapLayerInfo layer = request.getLayers().get(i);
            String name = layer.getName();

            containsRasterData = containsRasterData
                    || (layer.getType() == MapLayerInfo.TYPE_RASTER);

            if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                isRegionatingFriendly = isRegionatingFriendly
                        && layer.getFeature().getFeatureSource(null, null).getQueryCapabilities()
                                .isReliableFIDSupported();
            } else if (layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
                isRegionatingFriendly = isRegionatingFriendly
                        && layer.getRemoteFeatureSource().getQueryCapabilities()
                                .isReliableFIDSupported();
            }

            // strip off prefix
            int j = name.indexOf(':');
            if (j > -1) {
                name = name.substring(j + 1);
            }

            filename.append(name + "_");
        }

        // setup the default mode
        Map<String, String> rawKvp = request.getRawKvp();
        String mode = KvpUtils.caseInsensitiveParam(rawKvp, "mode", wmsConfiguration.getKmlReflectorMode());

        if (!MODES.containsKey(mode)) {
            throw new ServiceException("Unknown KML mode: " + mode);
        }

        Map modeOptions = new HashMap(MODES.get(mode));

        if ("superoverlay".equals(mode)) {
            String submode = KvpUtils.caseInsensitiveParam(request.getRawKvp(), "superoverlay_mode",
                    wmsConfiguration.getKmlSuperoverlayMode());

            if ("raster".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "raster");
            } else if ("overview".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "overview");
            } else if ("hybrid".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "hybrid");
            } else if ("auto".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "auto");
            } else if ("cached".equalsIgnoreCase(submode)) {
                modeOptions.put("overlaymode", "cached");
            } else {
                throw new ServiceException("Unknown overlay mode: " + submode);
            }
        }

        // first set up some of the normal wms defaults
        if (request.getWidth() < 1) {
            request.setWidth(mode.equals("refresh") || containsRasterData ? 1024 : 256);
        }

        if (request.getHeight() < 1) {
            request.setHeight(mode.equals("refresh") || containsRasterData ? 1024 : 256);
        }

        // Force srs to lat/lon for KML output.
        request.setSRS("EPSG:4326");

        // set rest of the wms defaults
        request = DefaultWebMapService.autoSetMissingProperties(request);

        // set some kml specific defaults
        Map fo = request.getFormatOptions();

        KvpUtils.merge(fo, modeOptions);
        
        organizeFormatOptionsParams(request.getRawKvp(), fo);
        
        if (fo.get("kmattr") == null) {
            fo.put("kmattr", wmsConfiguration.getKmlKmAttr());
        }
        if (fo.get("kmscore") == null) {
            fo.put("kmscore", wmsConfiguration.getKmScore());
        }
        if (fo.get("kmplacemark") == null) {
            fo.put("kmplacemark", wmsConfiguration.getKmlPlacemark());
        }

        // set the format
        // TODO: create a subclass of GetMapRequest to store these values

        Boolean superoverlay = (Boolean) fo.get("superoverlay");
        if (superoverlay == null)
            superoverlay = Boolean.FALSE;
        String formatExtension = ".kmz";
        if (superoverlay) {
            request.setFormat(KMZMapOutputFormat.MIME_TYPE);
            request.setBbox(KMLUtils.expandToTile(request.getBbox()));
        } else if (mode.equals("refresh") || containsRasterData) {
            request.setFormat(KMZMapOutputFormat.MIME_TYPE);
        } else if (!Arrays.asList(KMZMapOutputFormat.OUTPUT_FORMATS).contains(request.getFormat())) {
            // if the user did not explicitly request kml give them back KMZ
            request.setFormat(KMLMapOutputFormat.MIME_TYPE);
            formatExtension = ".kml";
        }

        // response.setContentType(request.getFormat());

        org.geoserver.wms.WebMap wmsResponse;
        if (!"download".equals(mode)) {
            if (KMLMapOutputFormat.MIME_TYPE.equals(request.getFormat())) {
                request.setFormat(NetworkLinkMapOutputFormat.KML_MIME_TYPE);
            } else {
                request.setFormat(NetworkLinkMapOutputFormat.KMZ_MIME_TYPE);
            }
        }

        wmsResponse = wms.getMap(request);

        filename.setLength(filename.length() - 1);
        String contentDisposition = "attachment; filename=" + filename.toString() + formatExtension;
        wmsResponse.setResponseHeader("Content-Disposition", contentDisposition);

        return wmsResponse;
    }
    
    
    /**
     * Copy all the format_options parameters from the kvp map and put them into the formatOptions map. If a parameter is already present in
     * formatOption map it will be preserved.
     * 
     * @param kvp
     * @param formatOptions
     * @throws Exception 
     */
    public static void organizeFormatOptionsParams(Map<String, String> kvp,
            Map<String, Object> formatOptions) throws Exception {

        WMSRequests.mergeEntry(kvp, formatOptions, "legend");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmscore");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmattr");
        WMSRequests.mergeEntry(kvp, formatOptions, "kmltitle");
//        WMSRequests.mergeEntry(kvp, formatOptions, "superoverlay");

    }

}
