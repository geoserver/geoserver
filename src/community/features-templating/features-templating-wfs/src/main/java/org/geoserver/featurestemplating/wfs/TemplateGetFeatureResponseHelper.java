/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wfs;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GMLTemplateWriter;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.JSONLDWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * Helper class for Template responses which mainly allows to obtain a writer according to the
 * specified format and provides some methods to retrieve a ${@link FeatureTypeInfo}
 */
public class TemplateGetFeatureResponseHelper {

    private Catalog catalog;

    private TemplateIdentifier format;

    TemplateGetFeatureResponseHelper(Catalog catalog, TemplateIdentifier format) {
        this.catalog = catalog;
        this.format = format;
    }

    TemplateOutputWriter getOutputWriter(OutputStream output) throws IOException {
        return getOutputWriter(output, null);
    }

    TemplateOutputWriter getOutputWriter(OutputStream output, String version) throws IOException {
        TemplateOutputWriter outputWriter;
        switch (format) {
            case JSON:
            case GEOJSON:
                outputWriter =
                        new GeoJSONWriter(
                                new JsonFactory().createGenerator(output, JsonEncoding.UTF8));
                break;
            case JSONLD:
                outputWriter =
                        new JSONLDWriter(
                                new JsonFactory().createGenerator(output, JsonEncoding.UTF8));
                break;
            case GML2:
            case GML31:
            case GML32:
                XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
                try {
                    XMLStreamWriter xMLStreamWriter =
                            xMLOutputFactory.createXMLStreamWriter(output);
                    outputWriter = new GMLTemplateWriter(xMLStreamWriter, version);
                    break;

                } catch (XMLStreamException e) {
                    throw new IOException(e);
                }
            default:
                outputWriter = null;
                break;
        }
        return outputWriter;
    }

    FeatureTypeInfo getFirstFeatureTypeInfo(GetFeatureRequest request) {
        Query query = request.getQueries().get(0);
        QName typeName = query.getTypeNames().get(0);
        return getFeatureTypeInfo(
                new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }

    FeatureTypeInfo getFeatureTypeInfo(FeatureCollection collection) {
        if (collection instanceof TypeInfoCollectionWrapper)
            return ((TypeInfoCollectionWrapper) collection).getFeatureTypeInfo();
        else return getFeatureTypeInfo(collection.getSchema().getName());
    }

    public FeatureTypeInfo getFeatureType(String collectionId) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        return featureType;
    }

    FeatureTypeInfo getFeatureTypeInfo(Name name) {
        return catalog.getFeatureTypeByName(name);
    }
}
