/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.response;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.*;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.impl.JsonBuilderContext;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;

/** Encodes features in json-ld output format */
public class JSONLDGetFeatureResponse extends WFSGetFeatureOutputFormat {

    /** The MIME type for a JSON-LD response* */
    public static final String MIME = "application/ld+json";

    private JsonLdConfiguration configuration;

    public JSONLDGetFeatureResponse(GeoServer gs, JsonLdConfiguration configuration) {
        super(gs, MIME);
        this.configuration = configuration;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        FeatureTypeInfo info =
                getFeatureType(GetFeatureRequest.adapt(getFeature.getParameters()[0]));
        try (JsonLdGenerator writer =
                new JsonLdGenerator(new JsonFactory().createGenerator(output, JsonEncoding.UTF8))) {

            RootBuilder rootBuilder = configuration.getTemplate(info);

            rootBuilder.startJsonLd(writer);
            for (FeatureCollection collection : featureCollection.getFeature()) {
                FeatureIterator iterator = collection.features();
                try {
                    while (iterator.hasNext()) {
                        JsonBuilderContext context = new JsonBuilderContext(iterator.next());
                        rootBuilder.evaluate(writer, context);
                    }
                } finally {
                    iterator.close();
                }
            }
            rootBuilder.endJsonLd(writer);
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ioex) {
                throw new ServiceException(ioex);
            }
        }
    }

    private FeatureTypeInfo getFeatureType(GetFeatureRequest request) {
        Query query = request.getQueries().get(0);
        QName typeName = query.getTypeNames().get(0);
        return gs.getCatalog()
                .getFeatureTypeByName(
                        new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME;
    }
}
