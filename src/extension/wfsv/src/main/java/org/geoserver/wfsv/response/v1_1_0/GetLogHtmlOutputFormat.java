/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfsv.GetLogType;

import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * Html, templatized output format for the GetLog response
 * @author Andrea Aime, TOPP
 *
 */
public class GetLogHtmlOutputFormat extends Response {
    private static Configuration templateConfig;

    static {
        //initialize the template engine, this is static to maintain a cache 
        // over instantiations of kml writer
        templateConfig = new Configuration();
        templateConfig.setObjectWrapper(new FeatureISODateWrapper());
    }

    public GetLogHtmlOutputFormat() {
        super(FeatureCollectionType.class, "HTML");
    }

    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/html";
    }

    public boolean canHandle(Operation operation) {
        if ("GetLog".equalsIgnoreCase(operation.getId())) {
            //also check that the resultType is "results"
            GetLogType request = (GetLogType) OwsUtils.parameter(operation.getParameters(),
                    GetLogType.class);

            return request.getResultType() == ResultTypeType.RESULTS_LITERAL;
        }

        return false;
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        FeatureCollectionType fct = (FeatureCollectionType) value;
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fct.getFeature().get(0);

        // setup template subsystem
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(getClass());
        templateLoader.setFeatureType(fc.getSchema());

        Template template = null;

        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            template = templateConfig.getTemplate("wfsvGetLog.ftl");
        }

        try {
            template.setOutputEncoding("UTF-8");
            template.process(fc, new OutputStreamWriter(output, Charset.forName("UTF-8")));
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }
}
