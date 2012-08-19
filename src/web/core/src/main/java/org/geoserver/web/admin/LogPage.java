/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.Level;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Shows the log file contents
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class LogPage extends GeoServerSecuredPage {
    static final String LINES = "lines";

    int lines = 1000;

    File logFile;

    public LogPage(PageParameters params) {
        Form form = new Form("form");
        add(form);
        
        /**
         * take geoserver log file location from Config as absolute path and only use if valid, 
         * otherwise fallback to (geoserver-root)/logs/geoserver.log as default.
         */
        String location = GeoServerExtensions.getProperty(LoggingUtils.GEOSERVER_LOG_LOCATION);
        if(location == null) {
            location= getGeoServerApplication().getGeoServer().getLogging().getLocation();
        }
        logFile = new File(location);
        
        if (!logFile.isAbsolute()) {
            // locate the geoserver.log file
            GeoServerDataDirectory dd = getGeoServerApplication().getBeanOfType(
                    GeoServerDataDirectory.class);
            logFile = new File(dd.root(), logFile.getPath());
        }
        
        if (!logFile.exists()) {
            error("Could not find the GeoServer log file: " + logFile.getAbsolutePath());
        }

        try {
            if (params.get(LINES) != null) {
                int lpv = params.get(LINES).toInt();
                if (lpv > 0) {
                    lines = lpv;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing the lines parameter: ", LINES);
        }

        form.add(new SubmitLink("refresh") {
            @Override
            public void onSubmit() {
                setResponsePage(LogPage.class, new PageParameters(LINES + "="
                        + String.valueOf(lines)));
            }
        });

        TextField lines = new TextField("lines", new PropertyModel(this, "lines"));
        lines.add(new MinimumValidator(1));
        form.add(lines);

        TextArea logs = new TextArea("logs", new GSLogsModel());
        logs.setOutputMarkupId(true);
        logs.setMarkupId("logs");
        add(logs);
        
        ResourceReference logResource = new LogResourceReference();
        CharSequence urlForLog = getRequestCycle().urlFor(logResource, null);
        ExternalLink link = new ExternalLink("download", urlForLog.toString());
    }

    public class GSLogsModel extends LoadableDetachableModel {

        @Override
        protected Object load() {
            BufferedReader br = null;
            try {
                // load the logs line by line, keep only the last 1000 lines
                LinkedList<String> lineList = new LinkedList<String>();

                br = new BufferedReader(new FileReader(logFile));
                String line;
                while ((line = br.readLine()) != null) {
                    lineList.addLast(line);
                    if (lineList.size() > LogPage.this.lines) {
                        lineList.removeFirst();
                    }
                }

                StringBuilder result = new StringBuilder();
                for (String logLine : lineList) {
                    result.append(logLine).append("\n");
                }
                return result;
            } catch (Exception e) {
                error(e);
                return e.getMessage();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

    }
    
    class LogResourceReference extends ResourceReference {

        public LogResourceReference() {
            super(LogResourceReference.class, "GeoServerLogFile");
        }

        @Override
        public IResource getResource() {
            return new ResourceStreamResource(new LogFileResource());
        }
        
    }
    
    class LogFileResource extends AbstractResourceStream {

        @Override
        public void close() throws IOException {
            // nothing to do I belive?
            
        }

        @Override
        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            try {
                return new FileInputStream(logFile);
            } catch (FileNotFoundException e) {
                throw new ResourceStreamNotFoundException(e);
            }
        }

       
        
    }
}
