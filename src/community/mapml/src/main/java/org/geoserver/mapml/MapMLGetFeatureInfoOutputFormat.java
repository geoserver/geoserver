/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLGetFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.mapml");

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    private WMS wms;

    public MapMLGetFeatureInfoOutputFormat(WMS wms) {
        super(MapMLConstants.MAPML_MIME_TYPE);
        this.wms = wms;
    }

    @Override
    public void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {

        String baseUrl = request.getBaseUrl();

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle("GetFeatureInfo Results");
        Base base = new Base();
        base.setHref(baseUrl + "mapml");
        head.setBase(base);
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("utf-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MapMLConstants.MAPML_MIME_TYPE); // ;projection=" + projType.value());
        metas.add(meta);
        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);
        Extent extent = new Extent();
        body.setExtent(extent);

        List<FeatureCollection> featureCollections = results.getFeature();
        SimpleFeatureCollection fc;
        if (featureCollections.size() > 0) {
            int lastFeatureCollection = featureCollections.size() - 1;
            if (!(featureCollections.get(lastFeatureCollection)
                    instanceof SimpleFeatureCollection)) {
                throw new ServiceException("MapML OutputFormat does not support Complex Features.");
            }
            fc = (SimpleFeatureCollection) featureCollections.get(lastFeatureCollection);
            List<Feature> features = body.getFeatures();
            SimpleFeatureIterator iterator = fc.features();
            while (iterator.hasNext()) {
                SimpleFeature feature;
                try {
                    // this can throw due to the feature not fitting into
                    // the extent of the CRS when it's transformed. Could
                    // see if in such a case we could just return the
                    // scalar properties
                    feature = iterator.next();
                    // convert feature to xml
                    Feature f = MapMLGenerator.buildFeature(feature);
                    features.add(f);
                    break; // stop after one feature
                } catch (IllegalStateException e) {
                    LOGGER.log(Level.INFO, "Error transforming feature.");
                }
            }
        }

        OutputStreamWriter osw = new OutputStreamWriter(out, wms.getCharSet());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }
}
