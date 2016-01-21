/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.Level;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Shows the log file contents
 * 
 * @author Andrea Aime - OpenGeo
 */
public class LogPage extends GeoServerSecuredPage {
    static final String LINES = "lines";

    int lines = 1000;

    File logFile;

    public LogPage(PageParameters params) {
        @SuppressWarnings("rawtypes")
        Form<?> form = new Form("form");
        add(form);
        
        /**
         * take geoserver log file location from Config as absolute path and only use if valid, 
         * otherwise fallback to (geoserver-root)/logs/geoserver.log as default.
         */
        String location = GeoServerExtensions.getProperty(LoggingUtils.GEOSERVER_LOG_LOCATION);
        if(location == null) {
            location= getGeoServerApplication().getGeoServer().getLogging().getLocation();
        }
        if (location == null) {
            GeoServerResourceLoader loader = getGeoServerApplication().getResourceLoader();
            logFile = loader.get("logs").get("geoserver.log").file();         
            location = logFile.getAbsolutePath();
        } else {
            logFile = new File(location);
            if (!logFile.isAbsolute()) {
                // locate the geoserver.log file
                GeoServerDataDirectory dd = getGeoServerApplication().getBeanOfType(
                        GeoServerDataDirectory.class);
                logFile = dd.get(Paths.convert(logFile.getPath())).file();
            }
        }
        
        
        if (!logFile.exists()) {
            error("Could not find the GeoServer log file: " + logFile.getAbsolutePath());
        }

        try {
            if (params.getNamedKeys().contains(LINES)) {
                if (params.get(LINES).toInt() > 0) {
                    lines = params.get(LINES).toInt();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing the lines parameter: ", params.get(LINES).toString());
        }

        form.add(new SubmitLink("refresh") {
            @Override
            public void onSubmit() {
                setResponsePage(LogPage.class, new PageParameters().add(LINES, lines));
            }
        });

        TextField lines = new TextField("lines", new PropertyModel(this, "lines"));
        lines.add(RangeValidator.minimum(1));
        form.add(lines);

        TextArea logs = new TextArea("logs", new GSLogsModel());
        logs.setOutputMarkupId(true);
        logs.setMarkupId("logs");
        add(logs);

        add(new Link("download") {

            @Override
            public void onClick() {
                IResourceStream stream = new FileResourceStream(logFile){
                    public String getContentType() {
                        return "text/plain";
                    }
                };
                ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, "geoserver.log");
                handler.setContentDisposition(ContentDisposition.ATTACHMENT);

                RequestCycle.get().scheduleRequestHandlerAfterCurrent(handler);
            }
        });

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
}
