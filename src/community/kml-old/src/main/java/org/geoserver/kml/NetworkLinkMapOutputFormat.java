/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.XMLTransformerMap;

/**
 * 
 */
public class NetworkLinkMapOutputFormat extends AbstractMapOutputFormat {
    /**
     * Official KMZ mime type, tweaked to output NetworkLink
     */
    static final String KML_MIME_TYPE = KMLMapOutputFormat.MIME_TYPE + ";mode=networklink";

    static final String KMZ_MIME_TYPE = KMZMapOutputFormat.MIME_TYPE + ";mode=networklink";

    public static final String[] OUTPUT_FORMATS = { KML_MIME_TYPE, KMZ_MIME_TYPE };

    private WMS wms;

    public NetworkLinkMapOutputFormat(WMS wms) {
        super(KML_MIME_TYPE, OUTPUT_FORMATS);
        this.wms = wms;
    }

    /**
     * Initializes the KML encoder. None of the map production is done here, it is done in
     * writeTo(). This way the output can be streamed directly to the output response and not
     * written to disk first, then loaded in and then sent to the response.
     * 
     * @param mapContent
     *            WMSMapContext describing what layers, styles, area of interest etc are to be used
     *            when producing the map.
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    @SuppressWarnings("rawtypes")
    public XMLTransformerMap produceMap(WMSMapContent mapContent) throws ServiceException,
            IOException {
        KMLNetworkLinkTransformer transformer = new KMLNetworkLinkTransformer(wms, mapContent);
        transformer.setIndentation(3);
        Charset encoding = wms.getCharSet();
        transformer.setEncoding(encoding);
        Map fo = mapContent.getRequest().getFormatOptions();
        Boolean superoverlay = (Boolean) fo.get("superoverlay");
        if (superoverlay == null) {
            superoverlay = Boolean.FALSE;
        }
        transformer.setEncodeAsRegion(superoverlay);
        GetMapRequest request = mapContent.getRequest();
        boolean cachedMode = "cached".equals(KMLUtils.getSuperoverlayMode(request, wms));
        transformer.setCachedMode(cachedMode);

        String mimeType = request.getFormat();
        XMLTransformerMap wmsResponse = new XMLTransformerMap(mapContent, transformer, mapContent,
                mimeType);
        return wmsResponse;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KMLMapOutputFormat.KML_CAPABILITIES;
    }
}
