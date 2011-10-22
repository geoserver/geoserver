/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.response.v1_1_0;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geotools.data.FeatureDiffReader;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * WFS output format for a GetDiff operation whose output format is a WFS 1.1
 * transaction
 *
 * @author Andrea Aime, TOPP
 *
 */
public class GetDiffHtmlOutputFormat extends Response {
    private static Configuration templateConfig;

    static {
        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
        templateConfig = new Configuration();
        templateConfig.setObjectWrapper(new FeatureDiffWrapper());
    }

    public GetDiffHtmlOutputFormat() {
        super(FeatureDiffReader[].class, "HTML");
    }

    public String getMimeType(Object value, Operation operation)
        throws ServiceException {
        return "text/html";
    }

    public boolean canHandle(Operation operation) {
        return "GetDiff".equalsIgnoreCase(operation.getId());
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        FeatureDiffReader[] diffReaders = (FeatureDiffReader[]) value;

        // setup template subsystem
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(getClass());
        templateLoader.setFeatureType(diffReaders[0].getSchema());

        Template template = null;

        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            template = templateConfig.getTemplate("wfsvGetDiff.ftl");
        }

        try {
            template.setOutputEncoding("UTF-8");
            template.process(diffReaders, new OutputStreamWriter(output, Charset.forName("UTF-8")));
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        } finally {
        	for (int i = 0; i < diffReaders.length; i++) {
				diffReaders[i].close();
			}
        }
    }
}
