/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.BaseRequest;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

/** Produces a Feature response in HTML. */
public class GetFeatureHTMLOutputFormat extends WFSGetFeatureOutputFormat {

    private static final String FORMAT = "text/html";

    private final GeoServer geoServer;
    private final FreemarkerTemplateSupport templateSupport;

    public GetFeatureHTMLOutputFormat(GeoServerResourceLoader loader, GeoServer geoServer) {
        super(geoServer, BaseRequest.HTML_MIME);
        this.geoServer = geoServer;
        this.templateSupport = new FreemarkerTemplateSupport(loader, geoServer);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return FORMAT;
    }

    @Override
    protected void write(
            FeatureCollectionResponse response, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        // if there is only one feature type loaded, we allow for header/footer customization,
        // otherwise we stick with the generic ones
        FeatureTypeInfo referenceFeatureType = null;
        List<FeatureCollection> collections = response.getFeature();
        if (collections.size() == 1) {
            referenceFeatureType = getResource(collections.get(0));
        }

        Template header = templateSupport.getTemplate(referenceFeatureType, "header.ftl");
        Template footer = templateSupport.getTemplate(referenceFeatureType, "footer.ftl");

        try (OutputStreamWriter osw = new OutputStreamWriter(output)) {
            try {
                header.process(response, osw);
            } catch (TemplateException e) {
                String msg = "Error occured processing header template.";
                throw (IOException) new IOException(msg).initCause(e);
            }

            // process content template for all feature collections found
            for (int i = 0; i < collections.size(); i++) {
                FeatureCollection fc = collections.get(i);
                if (fc != null && fc.size() > 0) {
                    Template content = null;
                    FeatureTypeInfo typeInfo = getResource(fc);
                    if (!(fc.getSchema() instanceof SimpleFeatureType)) {
                        // if there is a specific template for complex features, use that.
                        content = templateSupport.getTemplate(typeInfo, "complex_content.ftl");
                    }
                    if (content == null) {
                        content = templateSupport.getTemplate(typeInfo, "content.ftl");
                    }
                    try {
                        content.process(fc, osw);
                    } catch (TemplateException e) {
                        String msg =
                                "Error occurred processing content template "
                                        + content.getName()
                                        + " for "
                                        + typeInfo.prefixedName();
                        throw (IOException) new IOException(msg).initCause(e);
                    }
                }
            }

            // if a template footer was loaded (ie, there were only one feature
            // collection), process it
            if (footer != null) {
                try {
                    footer.process(response, osw);
                } catch (TemplateException e) {
                    String msg = "Error occured processing footer template.";
                    throw (IOException) new IOException(msg).initCause(e);
                }
            }
            osw.flush();
        } finally {
            // close any open iterators
            FreemarkerTemplateSupport.FC_FACTORY.purge();
        }
    }

    private FeatureTypeInfo getResource(FeatureCollection collection) {
        FeatureTypeInfo info = null;
        if (collection instanceof TypeInfoCollectionWrapper) {
            info = ((TypeInfoCollectionWrapper) collection).getFeatureTypeInfo();
        }
        if (info == null && collection.getSchema() != null) {
            info = geoServer.getCatalog().getFeatureTypeByName(collection.getSchema().getName());
        }
        return info;
    }
}
