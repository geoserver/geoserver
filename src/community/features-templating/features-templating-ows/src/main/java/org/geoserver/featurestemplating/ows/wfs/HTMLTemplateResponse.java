/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ows.wfs;

import static org.geoserver.featurestemplating.builders.VendorOptions.JSON_LD_SCRIPT;
import static org.geoserver.featurestemplating.builders.VendorOptions.LINK;
import static org.geoserver.featurestemplating.builders.VendorOptions.SCRIPT;
import static org.geoserver.featurestemplating.builders.VendorOptions.STYLE;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.featurestemplating.writers.XHTMLTemplateWriter;
import org.geoserver.featurestemplating.writers.XMLTemplateWriter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;

/** A template response able to write an HTML output format. */
public class HTMLTemplateResponse extends BaseTemplateGetFeatureResponse {

    private static final String ELEMENT_NAME = "HTML";

    public HTMLTemplateResponse(GeoServer gs, TemplateLoader configuration) {
        super(gs, configuration, TemplateIdentifier.HTML);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {}

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {
        String outputFormat = getMimeType(null, getFeature);
        try (XMLTemplateWriter writer = getOutputWriter(output, outputFormat)) {
            writer.startTemplateOutput(mapVendorOptionsToHints(featureCollection, outputFormat));
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
            throws IOException {}

    protected XHTMLTemplateWriter getOutputWriter(OutputStream output, String outputFormat)
            throws IOException {
        return (XHTMLTemplateWriter) helper.getOutputWriter(output, outputFormat);
    }

    private EncodingHints mapVendorOptionsToHints(
            FeatureCollectionResponse response, String outputFormat) throws ExecutionException {
        List<FeatureCollection> collectionList = response.getFeature();
        List<String> scripts = new ArrayList<>();
        List<String> styles = new ArrayList<>();
        List<FeatureCollection> jsonLdCollections = new ArrayList<>();
        EncodingHints encodingHints = new EncodingHints();
        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = helper.getFeatureTypeInfo(collection);
            RootBuilder root = configuration.getTemplate(fti, outputFormat);
            String script = root.getVendorOptions().get(SCRIPT, String.class, "");
            String style = root.getVendorOptions().get(VendorOptions.STYLE, String.class, "");
            if (!script.isEmpty()) scripts.add(script);
            if (!style.isEmpty()) styles.add(style);
            VendorOptions options = root.getVendorOptions();
            addLinks(options, encodingHints);
            addToJsonLdScriptNameList(jsonLdCollections, options, collection);
        }
        if (!scripts.isEmpty()) encodingHints.put(SCRIPT, scripts);
        if (!styles.isEmpty()) encodingHints.put(STYLE, styles);
        if (!jsonLdCollections.isEmpty()) encodingHints.put(JSON_LD_SCRIPT, jsonLdCollections);
        return encodingHints;
    }

    private void addToJsonLdScriptNameList(
            List<FeatureCollection> jsonLdScriptNames,
            VendorOptions options,
            FeatureCollection collection) {
        boolean jsonLdScript = options.get(JSON_LD_SCRIPT, Boolean.class, false);
        if (jsonLdScript) jsonLdScriptNames.add(collection);
    }

    private void addLinks(VendorOptions options, EncodingHints hints) {
        if (options.keySet().stream().anyMatch(k -> k.startsWith(LINK))) {
            for (String key : options.keySet()) {
                if (key != null && key.startsWith(LINK)) hints.put(key, options.get(key));
            }
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return TemplateIdentifier.HTML.getOutputFormat();
    }

    @Override
    protected void beforeEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.beforeEvaluation(writer, root, feature);
    }

    @Override
    protected void afterEvaluation(TemplateOutputWriter writer, RootBuilder root, Feature feature)
            throws IOException {
        super.afterEvaluation(writer, root, feature);
    }

    @Override
    protected boolean canHandleInternal(Operation operation) {
        boolean result = super.canHandleInternal(operation);
        if (result) {
            String outputFormat = identifier.getOutputFormat();
            boolean hasParam =
                    operation != null
                            && operation.getParameters() != null
                            && operation.getParameters().length > 0;
            if (hasParam) {
                String param = operation.getParameters()[0].toString();
                Request request = Dispatcher.REQUEST.get();
                // check it is OGCAPI Features collection request
                if (isFeaturesRequest(request, param, outputFormat)) {
                    Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
                    FeatureTypeInfo fti = catalog.getFeatureTypeByName(param);
                    // if no template for fti then this response object should not be used.
                    result = hasTemplate(fti);
                }
            }
        }
        return result;
    }

    private boolean hasTemplate(FeatureTypeInfo fti) {
        boolean result = true;
        try {
            RootBuilder template =
                    configuration.getTemplate(fti, TemplateIdentifier.HTML.getOutputFormat());
            if (template == null) result = false;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private boolean isFeaturesRequest(Request request, String ftName, String outputFormat) {
        return request != null
                && ftName != null
                && "FEATURES".equalsIgnoreCase(request.getService())
                && outputFormat != null;
    }

    @Override
    public String getCapabilitiesElementName() {
        return ELEMENT_NAME;
    }
}
