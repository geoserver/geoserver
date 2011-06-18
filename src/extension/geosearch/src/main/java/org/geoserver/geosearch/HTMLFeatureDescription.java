package org.geoserver.geosearch;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.opengis.feature.simple.SimpleFeature;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class HTMLFeatureDescription extends AbstractFeatureDescription {
    private final DataFormat format =
        new FreemarkerFormat("featurepage.ftl", HTMLFeatureDescription.class, MediaType.TEXT_HTML);

    private String GEOSERVER_BASE_URL;

    public void handle(Request req, Response resp){
        GEOSERVER_BASE_URL = getBaseURL(req);

        if (req.getMethod().equals(Method.GET)){
            doGet(req, resp);
        } else {
            resp.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        }
    }

    public void doGet(Request req, Response resp){
        String namespace = (String)req.getAttributes().get("namespace");
        SimpleFeature f = findFeature(req);
        
        resp.setEntity(format.toRepresentation(buildContext(namespace, f)));
    }

    private Map buildContext(String namespace, SimpleFeature f) {
        Map m = new HashMap();
        FeatureTemplate t = new FeatureTemplate();
        
        m.put(
                "typeName", 
                namespace  
                + ":"
                + f.getType().getName().getLocalPart()
             );

        m.put("kmllink", buildURL(GEOSERVER_BASE_URL, f.getIdentifier().toString() + "_goto.kml", null, URLType.SERVICE));
        
        m.put("rawkmllink",  buildURL(GEOSERVER_BASE_URL,  f.getIdentifier().toString() + ".kml?raw=true", null, URLType.SERVICE));
        
        try {
            m.put("name", t.title(f));
        } catch (IOException e) {
            m.put("name", f.getIdentifier().toString());
        }

        try {
            m.put("description", t.description(f));
        } catch (IOException e) {
            m.put("description", "");
        }

        return m;
    }
}
