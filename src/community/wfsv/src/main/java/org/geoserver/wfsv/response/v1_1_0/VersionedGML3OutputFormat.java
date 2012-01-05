/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.wfs.BaseRequestType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfsv.VersionedFeatureCollectionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Response;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfsv.xml.v1_1_0.WFSVConfiguration;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Works just like {@link GML3OutputFormat}, but refers to DescribeVersionedFeatureType and encodes
 * a VersionedFeatureCollection
 * 
 * @author Andrea Aime
 *
 */
public class VersionedGML3OutputFormat extends Response {

    private GeoServer gs;
    private Catalog catalog;
    private WFSVConfiguration configuration;

    public VersionedGML3OutputFormat(GeoServer gs,
            WFSVConfiguration configuration) {
        super(VersionedFeatureCollectionType.class,
                new HashSet<String>(Arrays.asList(new String[] { "gml3",
                        "text/xml; subtype=gml/3.1.1" })));

        this.gs = gs;
        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/3.1.1";
    }

    public void write(Object value, OutputStream output, Operation getFeature)
            throws ServiceException, IOException {
        VersionedFeatureCollectionType results = (VersionedFeatureCollectionType) value;
        List featureCollections = results.getFeature();

        // round up the info objects for each feature collection
        HashMap /* <String,Set> */ns2metas = new HashMap();

        for (Iterator fc = featureCollections.iterator(); fc.hasNext();) {
            SimpleFeatureCollection features = (SimpleFeatureCollection) fc.next();
            SimpleFeatureType featureType = features.getSchema();

            // load the metadata for the feature type
            String namespaceURI = featureType.getName().getNamespaceURI();
            FeatureTypeInfo meta = catalog.getFeatureTypeByName(namespaceURI, featureType.getTypeName() );

            if (meta == null)
                throw new WFSException("Could not find feature type "
                        + namespaceURI + ":" + featureType.getTypeName()
                        + " in the GeoServer catalog");

            // add it to the map
            Set metas = (Set) ns2metas.get(namespaceURI);

            if (metas == null) {
                metas = new HashSet();
                ns2metas.put(namespaceURI, metas);
            }

            metas.add(meta);
        }

        WFSInfo wfs = gs.getService(WFSInfo.class);
        GeoServerInfo global = wfs.getGeoServer().getGlobal();
        Encoder encoder = new Encoder(configuration, configuration.schema());
        encoder.setEncoding(Charset.forName( gs.getSettings().getCharset() ));

        // declare wfs schema location
        BaseRequestType gft = (BaseRequestType) getFeature.getParameters()[0];

        encoder.setSchemaLocation(org.geoserver.wfsv.xml.v1_1_0.WFSV.NAMESPACE,
                buildSchemaURL(gft.getBaseUrl(), "wfs/1.1.0/wfs.xsd"));

        // declare application schema namespaces
        for (Iterator i = ns2metas.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();

            String namespaceURI = (String) entry.getKey();
            Set metas = (Set) entry.getValue();

            StringBuffer typeNames = new StringBuffer();

            for (Iterator m = metas.iterator(); m.hasNext();) {
                FeatureTypeInfo meta = (FeatureTypeInfo) m.next();
                typeNames.append(meta.getName());

                if (m.hasNext()) {
                    typeNames.append(",");
                }
            }

            // set the schema location
            Map<String, String> params = params("service", "WFS",
                    "version", "1.1.0", 
                    "request", "DescribeFeatureType",
                    "typeName", typeNames.toString());
            encoder.setSchemaLocation(namespaceURI, buildURL(gft.getBaseUrl(), "wfs", params, URLType.SERVICE));
        }

        encoder.encode(results,
                    org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION, output);
       
    }

    public boolean canHandle(Operation operation) {
        // GetVersionedFeature operation?
        if ("GetVersionedFeature".equalsIgnoreCase(operation.getId())) {
            // also check that the resultType is "results"
            GetFeatureType request = (GetFeatureType) OwsUtils.parameter(
                    operation.getParameters(), GetFeatureType.class);

            if (request.getResultType() == ResultTypeType.RESULTS_LITERAL) {
                return true;
            }
        }

        return false;
    }

}
