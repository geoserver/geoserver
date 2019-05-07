/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.config.GeoServer;
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
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLGetFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    @Autowired private GeoServer geoServer;

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

        List<FeatureCollection> featureCollections = results.getFeature();
        if (featureCollections.size() > 1) {
            throw new ServiceException(
                    "MapML OutputFormat does not support Multiple Feature Type output.");
        }
        SimpleFeatureCollection fc = null;
        if (featureCollections.size() == 1) {
            FeatureCollection featureCollection = featureCollections.get(0);
            if (!(featureCollection instanceof SimpleFeatureCollection)) {
                throw new ServiceException("MapML OutputFormat does not support Complex Features.");
            }
            fc = (SimpleFeatureCollection) featureCollection;
        }

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
        // extent.setUnits(projType);
        List<Object> extentList = extent.getInputOrDatalistOrLink();

        if (fc != null) {
            List<Feature> features = body.getFeatures();
            try (SimpleFeatureIterator iterator = fc.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    // convert feature to xml
                    Feature f = MapMLGenerator.buildFeature(feature);
                    features.add(f);
                }
            }
        }

        OutputStreamWriter osw = new OutputStreamWriter(out, wms.getCharSet());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }
}
