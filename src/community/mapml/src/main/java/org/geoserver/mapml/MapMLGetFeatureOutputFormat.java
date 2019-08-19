/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Input;
import org.geoserver.mapml.xml.InputType;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    public MapMLGetFeatureOutputFormat(GeoServer gs) {
        super(gs, MapMLConstants.FORMAT_NAME);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MapMLConstants.MAPML_MIME_TYPE;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollectionResponse,
            OutputStream out,
            Operation getFeature)
            throws IOException, ServiceException {

        String baseUrl = featureCollectionResponse.getBaseUrl();

        List<FeatureCollection> featureCollections = featureCollectionResponse.getFeatures();
        if (featureCollections.size() != 1) {
            throw new ServiceException(
                    "MapML OutputFormat does not support Multiple Feature Type output.");
        }
        FeatureCollection featureCollection = featureCollections.get(0);
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            throw new ServiceException("MapML OutputFormat does not support Complex Features.");
        }
        SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;

        LayerInfo layerInfo = gs.getCatalog().getLayerByName(fc.getSchema().getTypeName());
        ResourceInfo resourceInfo = layerInfo.getResource();
        MetadataMap layerMeta = resourceInfo.getMetadata();

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerInfo.getName());
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
        List<Link> links = head.getLinks();

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link link = new Link();
            link.setRel(RelType.LICENSE);
            if (licenseLink != null) {
                link.setHref(licenseLink);
            }
            if (licenseTitle != null) {
                link.setTitle(licenseTitle);
            }
            links.add(link);
        }

        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);
        Extent extent = new Extent();
        body.setExtent(extent);
        // extent.setUnits(projType);
        List<Object> extentList = extent.getInputOrDatalistOrLink();

        // zoom
        Input input = new Input();
        input.setName("z");
        input.setType(InputType.ZOOM);
        input.setValue("0");
        input.setMin("0");
        input.setMax("0");
        extentList.add(input);

        List<Feature> features = body.getFeatures();
        try (SimpleFeatureIterator iterator = fc.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                // convert feature to xml
                Feature f = MapMLGenerator.buildFeature(feature);
                features.add(f);
            }
        }

        // write to output
        OutputStreamWriter osw = new OutputStreamWriter(out, gs.getSettings().getCharset());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(mapml, result);
        osw.flush();
    }
}
