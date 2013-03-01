/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.sequence.FolderSequenceFactory;
import org.geoserver.kml.sequence.SequenceList;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.logging.Logging;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;

/**
 * Handles a GetMap request that spects a map in KML format.
 * 
 * @author James Macgill
 */
public class KMLMapOutputFormat implements GetMapOutputFormat {
    /** standard logger */
    protected static final Logger LOGGER = Logging.getLogger(KMLMapOutputFormat.class);

    static final MapProducerCapabilities KML_CAPABILITIES = new MapProducerCapabilities(false,
            false, true, true, null);

    /**
     * Official KML mime type
     */
    public static final String MIME_TYPE = "application/vnd.google-earth.kml+xml";

    private Set<String> OUTPUT_FORMATS = Collections.unmodifiableSet(new HashSet<String>(Arrays
            .asList(MIME_TYPE, "application/vnd.google-earth.kml", "kml",
                    "application/vnd.google-earth.kml xml")));

    private WMS wms;

    public KMLMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    /**
     * @return {@code "application/vnd.google-earth.kml+xml"}
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Produce the actual map ready for outputing.
     * 
     * @param map WMSMapContext describing what layers, styles, area of interest etc are to be used
     *        when producing the map.
     * 
     * @see GetMapOutputFormat#produceMap(WMSMapContent)
     */
    public KMLMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {

        GetMapRequest request = mapContent.getRequest();

        // prepare kml, document and folder
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();
        String kmltitle = (String) request.getFormatOptions().get("kmltitle");
        document.setName(kmltitle);

        // initialize the kml encoding context
        KmlEncodingContext context = new KmlEncodingContext(mapContent, request, wms);

        // get the callbacks for the document and let them loose
        List<KmlDecorator> decorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : decorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException("Coding error in decorator " + decorator
                        + ", document objects cannot be set to null");
            }
        }

        // create a generator that will generate a folder for each layer
        SequenceList<Feature> folders = new SequenceList<Feature>(
                new FolderSequenceFactory(context));
        document.setFeature(folders);

        KMLMap map = new KMLMap(mapContent, kml);
        map.setContentDispositionHeader(mapContent, ".kml");
        return map;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return KML_CAPABILITIES;
    }
}
