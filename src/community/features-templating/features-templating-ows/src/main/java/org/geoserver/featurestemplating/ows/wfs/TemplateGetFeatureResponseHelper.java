/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.utils.FeatureTypeInfoUtils;
import org.geoserver.featurestemplating.writers.GMLTemplateWriter;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.JSONLDWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.featurestemplating.writers.XHTMLTemplateWriter;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
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

    private static final String ESCAPE_CHARS = "escapeCharacters";

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
                                new JsonFactory().createGenerator(output, JsonEncoding.UTF8),
                                format);
                break;
            case JSONLD:
                outputWriter =
                        new JSONLDWriter(
                                new JsonFactory().createGenerator(output, JsonEncoding.UTF8));
                break;
            case GML2:
            case GML31:
            case GML32:
            case HTML:
                XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
                // we do ourselves the escaping in the template writer since the one provided in the
                // XMLStreamWriter implementation
                // doesn't handle all the characters.
                if (xMLOutputFactory.isPropertySupported(ESCAPE_CHARS))
                    xMLOutputFactory.setProperty(ESCAPE_CHARS, false);
                try {
                    XMLStreamWriter xMLStreamWriter =
                            xMLOutputFactory.createXMLStreamWriter(output, "UTF-8");

                    if (format.equals(TemplateIdentifier.HTML))
                        outputWriter = new XHTMLTemplateWriter(xMLStreamWriter, output);
                    else outputWriter = new GMLTemplateWriter(xMLStreamWriter, version);
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

    FeatureTypeInfo getFirstFeatureTypeInfo(Object request) {
        FeatureTypeInfo result;
        if (request instanceof GetFeatureInfoRequest)
            result = getFirstFeatureTypeInfo((GetFeatureInfoRequest) request);
        else result = getFirstFeatureTypeInfo(GetFeatureRequest.adapt(request));
        return result;
    }

    FeatureTypeInfo getFirstFeatureTypeInfo(GetFeatureInfoRequest request) {
        Optional<MapLayerInfo> op =
                request.getQueryLayers().stream().filter(ml -> ml != null).findFirst();
        if (!op.isPresent()) return null;
        return op.get().getFeature();
    }

    FeatureTypeInfo getFirstFeatureTypeInfo(GetFeatureRequest request) {
        Query query = request.getQueries().get(0);
        QName typeName = query.getTypeNames().get(0);
        return getFeatureTypeInfo(
                new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }

    FeatureTypeInfo getFeatureTypeInfo(FeatureCollection collection) {
        return FeatureTypeInfoUtils.getFeatureTypeInfo(catalog, collection);
    }

    public FeatureTypeInfo getFeatureType(String collectionId) {
        return FeatureTypeInfoUtils.getFeatureTypeInfo(catalog, collectionId);
    }

    FeatureTypeInfo getFeatureTypeInfo(Name name) {
        return FeatureTypeInfoUtils.getFeatureTypeInfo(catalog, name);
    }
}
