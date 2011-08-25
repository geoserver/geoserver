/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import java.awt.image.RenderedImage;
import java.util.logging.Logger;

import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RenderedImageMap;

/**
 * GIF Animated reflecting service.
 * <p>
 * This is the main entry point to easily create animations.<br/>
 * The reflector is able to parse incomplete WMS GetMap requests containing at least:<br/>
 * <ul>
 * <li>A multivalued request supported output format</li>
 * <li>An "aparam" animation parameter</li>
 * <li>An "avalues" list of values for the animation parameter</li>
 * </ul>
 * 
 * </p>
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class Animator {

    private static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.vfny.geoserver.wms.responses.map.anim");

    /** 
     * default 'format' value
     * - used if no output format has been found on the GetMap request
     **/
    public static final String GIF_ANIMATED_FORMAT = "image/gif;subtype=animated";

    /**
     * web map service
     */
    WebMapService wms;

    /**
     * The WMS configuration
     */
    WMS wmsConfiguration;

    /**
     * The prototype Constructor
     * 
     * @param wms
     * @param wmsConfiguration
     */
    public Animator(WebMapService wms, WMS wmsConfiguration) {
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;
    }

    /**
     * Produce method. 
     * Returns the full animation WebMap request.
     * 
     * @param request
     * @param wms
     * @param wmsConfiguration
     * @return
     * @throws Exception
     */
    public static org.geoserver.wms.WebMap produce(GetMapRequest request, WebMapService wms,
            WMS wmsConfiguration) throws Exception {

        // initializing the catalog of frames. The method analyzes the main request looking for
        // 'aparam' and 'avalues' and initializes the list of frames to be produced.
        FrameCatalog frameCatalog = initRequestManager(request, wms, wmsConfiguration);

        if (frameCatalog == null) {
            throw new RuntimeException("Animator initialization error!");
        }

        // initializing the catalog visitor. This takes care of producing single
        // RenderedImages.
        FrameCatalogVisitor visitor = new FrameCatalogVisitor();
        frameCatalog.getFrames(visitor);
        RenderedImage imageList = visitor.produce(frameCatalog.getWmsConfiguration());

        // set rest of the wms defaults
        request = DefaultWebMapService.autoSetMissingProperties(request);

        // Setup AnimGifOUTputFormat as default if not specified
        if (request.getFormat() == null) {
            request.setFormat(GIF_ANIMATED_FORMAT);
        }

        WebMap wmsResponse = wms.getMap(request);

        return new RenderedImageMap(((RenderedImageMap) wmsResponse).getMapContext(), imageList,
                wmsResponse.getMimeType());
    }

    /**
     * Initializes the Animator engine.
     * 
     * @param request
     * @param wmsConfiguration 
     * @return
     */
    private static FrameCatalog initRequestManager(GetMapRequest request, WebMapService wms, WMS wmsConfiguration) {
        return new FrameCatalog(request, wms, wmsConfiguration);
    }

}