/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.MultiValueMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;

public class GML2OutputFormat2 extends WFSGetFeatureOutputFormat {

    Catalog catalog;
    GeoServerResourceLoader resourceLoader;

    public GML2OutputFormat2(GeoServer gs) {
        super(gs, new HashSet(Arrays.asList(new Object[] {"gml2", "text/xml; subtype=gml/2.1.2"})));

        this.catalog = gs.getCatalog();
        this.resourceLoader = catalog.getResourceLoader();
    }

    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/2.1.2";
    }

    public String getCapabilitiesElementName() {
        return "GML2";
    }

    protected void write(
            FeatureCollectionResponse results, OutputStream output, Operation getFeature)
            throws ServiceException, IOException {

        // declare wfs schema location
        GetFeatureRequest gft = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

        List featureCollections = results.getFeature();

        // round up the info objects for each feature collection
        MultiValueMap ns2metas = new MultiValueMap();

        for (Iterator fc = featureCollections.iterator(); fc.hasNext(); ) {
            SimpleFeatureCollection features = (SimpleFeatureCollection) fc.next();
            SimpleFeatureType featureType = features.getSchema();

            // load the metadata for the feature type
            String namespaceURI = featureType.getName().getNamespaceURI();
            FeatureTypeInfo meta =
                    catalog.getFeatureTypeByName(namespaceURI, featureType.getTypeName());
            if (meta == null)
                throw new WFSException(
                        gft,
                        "Could not find feature type "
                                + namespaceURI
                                + ":"
                                + featureType.getTypeName()
                                + " in the GeoServer catalog");

            NamespaceInfo ns = catalog.getNamespaceByURI(namespaceURI);
            ns2metas.put(ns, meta);
        }

        Collection<FeatureTypeInfo> featureTypes = ns2metas.values();

        // create the encoder
        ApplicationSchemaXSD xsd =
                new ApplicationSchemaXSD(
                        null,
                        catalog,
                        gft.getBaseUrl(),
                        org.geotools.wfs.v1_0.WFS.getInstance(),
                        featureTypes);
        Configuration configuration =
                new ApplicationSchemaConfiguration(
                        xsd, new org.geotools.wfs.v1_0.WFSConfiguration_1_0());

        Encoder encoder = new Encoder(configuration);
        // encoder.setEncoding(wfs.getCharSet());

        encoder.setSchemaLocation(
                org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                buildSchemaURL(gft.getBaseUrl(), "wfs/1.0.0/WFS-basic.xsd"));

        // declare application schema namespaces
        Map<String, String> params =
                params("service", "WFS", "version", "1.0.0", "request", "DescribeFeatureType");
        for (Iterator i = ns2metas.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();

            NamespaceInfo ns = (NamespaceInfo) entry.getKey();
            String namespaceURI = ns.getURI();

            Collection metas = (Collection) entry.getValue();

            StringBuffer typeNames = new StringBuffer();

            for (Iterator m = metas.iterator(); m.hasNext(); ) {
                FeatureTypeInfo meta = (FeatureTypeInfo) m.next();
                typeNames.append(meta.prefixedName());

                if (m.hasNext()) {
                    typeNames.append(",");
                }
            }

            // set the schema location
            params.put("typeName", typeNames.toString());
            encoder.setSchemaLocation(
                    namespaceURI, buildURL(gft.getBaseUrl(), "wfs", params, URLType.RESOURCE));
        }

        encoder.encode(results.getAdaptee(), org.geotools.wfs.v1_0.WFS.FeatureCollection, output);
    }
}
