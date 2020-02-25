/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Resonse which handles an individual {@link SimpleFeature} and encodes it as gml.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class FeatureResponse extends WFSResponse {

    Catalog catalog;
    WFSConfiguration configuration;

    public FeatureResponse(GeoServer gs, WFSConfiguration configuration) {
        super(gs, SimpleFeature.class);

        this.catalog = gs.getCatalog();
        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {

        return "text/xml; subtype=gml/3.1.1";
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        // get the feature
        SimpleFeature feature = (SimpleFeature) value;
        SimpleFeatureType featureType = feature.getType();

        // grab the metadata
        FeatureTypeInfo meta = catalog.getFeatureTypeByName(featureType.getName());

        // create teh encoder
        Encoder encoder = new Encoder(configuration);
        encoder.setEncoding(Charset.forName(getInfo().getGeoServer().getSettings().getCharset()));
        encoder.encode(feature, new QName(meta.getNamespace().getURI(), meta.getName()), output);
    }
}
