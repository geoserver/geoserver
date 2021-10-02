/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import static org.geoserver.featurestemplating.builders.EncodingHints.NAMESPACES;
import static org.geoserver.featurestemplating.builders.EncodingHints.SCHEMA_LOCATION;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import net.opengis.wfs.GetFeatureType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.GMLTemplateWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.featurestemplating.writers.XMLTemplateWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;

/** A template response able to write a GML output format according to gml version. */
public class GMLTemplateResponse extends BaseTemplateGetFeatureResponse {

    public GMLTemplateResponse(
            GeoServer gs, TemplateLoader configuration, TemplateIdentifier identifier) {
        super(gs, configuration, identifier);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {
        String typeName = typeInfo.getName();
        ((GMLTemplateWriter) writer).setTypeName(typeName);
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        String outputFormat = getMimeType(null, getFeature);
        try (GMLTemplateWriter writer = getOutputWriter(output, outputFormat)) {
            setNamespacesAndSchemaLocations(featureCollection, writer, outputFormat);
            writer.startTemplateOutput(null);
            writeAdditionalFields(writer, featureCollection, getFeature);
            iterateFeatureCollection(writer, featureCollection, getFeature);
            writer.endTemplateOutput(null);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected void writeAdditionalFieldsInternal(
            TemplateOutputWriter writer,
            FeatureCollectionResponse featureCollection,
            Operation getFeature,
            BigInteger featureCount,
            ReferencedEnvelope bounds)
            throws IOException {
        try {
            writer.writeCollectionCounts(featureCount);
            writer.writeNumberReturned();
            writer.writeTimeStamp();
            if (bounds != null) writer.writeCollectionBounds(bounds);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected GMLTemplateWriter getOutputWriter(OutputStream output, String outputFormat)
            throws IOException {
        return (GMLTemplateWriter) helper.getOutputWriter(output, outputFormat);
    }

    private void setNamespacesAndSchemaLocations(
            FeatureCollectionResponse response, XMLTemplateWriter writer, String outputFormat)
            throws ExecutionException {
        List<FeatureCollection> collectionList = response.getFeature();
        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
            RootBuilder root = configuration.getTemplate(fti, outputFormat);
            Map<String, String> namespaces =
                    root.getVendorOptions().get(VendorOptions.NAMESPACES, Map.class, new HashMap());
            Map<String, String> namespaces2 =
                    (Map<String, String>) root.getEncodingHints().get(NAMESPACES);
            if (namespaces2 != null) namespaces.putAll(namespaces2);
            String schemaLocation =
                    (String) root.getVendorOptions().get(VendorOptions.SCHEMA_LOCATION);
            if (schemaLocation == null)
                schemaLocation = (String) root.getEncodingHints().get(SCHEMA_LOCATION);
            if (namespaces != null) writer.addNamespaces(namespaces);
            if (schemaLocation != null) writer.addSchemaLocations(schemaLocation);
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (operation != null) {
            Object[] parameters = operation.getParameters();
            if (parameters.length > 0) {
                Object param = parameters[0];
                if (param instanceof GetFeatureType) {
                    return ((GetFeatureType) param).getOutputFormat();
                }
            }
        }
        return TemplateIdentifier.GML32.getOutputFormat();
    }

    @Override
    protected void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.beforeEvaluation(writer, root, feature);
        ((GMLTemplateWriter) writer).startFeatureMember();
    }

    @Override
    protected void afterEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.afterEvaluation(writer, root, feature);
        ((GMLTemplateWriter) writer).endFeatureMember();
    }
}
