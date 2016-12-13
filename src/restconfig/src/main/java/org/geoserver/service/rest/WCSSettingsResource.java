/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.service.rest;

import java.util.Map;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSInfoImpl;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class WCSSettingsResource extends ServiceSettingsResource {

    public WCSSettingsResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WCSSettingsHTMLFormat(request, response, this);
    }

    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setHideFeatureTypeAttributes();
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected ServiceInfo getServiceObject() {
                String workspace = getAttribute("workspace");
                ServiceInfo service;
                if (workspace != null) {
                    WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
                    service = geoServer.getService(ws, WCSInfo.class);
                } else {
                    service = geoServer.getService(WCSInfo.class);
                }
                return service;
            }
            @Override
            protected Class<WCSInfo> getObjectClass() {
                return WCSInfo.class;
            }
        });
        WCSXStreamLoader.initXStreamPersister(persister);
    }

    static class WCSSettingsHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public WCSSettingsHTMLFormat(Request request, Response response, Resource resource) {
            super(SettingsInfo.class, request, response, resource);
        }

        @Override
        protected String getTemplateName(Object data) {
            return "wcsSettings";
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new ObjectToMapWrapper<WCSInfo>(WCSInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, WCSInfo wcsInfo) {
                    WorkspaceInfo workspaceInfo = wcsInfo.getWorkspace();
                    properties.put("workspaceName", workspaceInfo != null ? workspaceInfo.getName() : "NO_WORKSPACE");
                    properties.put("enabled", wcsInfo.isEnabled() ? "true" : "false");
                    properties.put("name", wcsInfo.getName());
                    properties.put("title", wcsInfo.getTitle());
                    properties.put("maintainer", wcsInfo.getMaintainer());
                    properties.put("abstract", wcsInfo.getAbstract());
                    properties.put("accessConstraints", wcsInfo.getAccessConstraints());
                    properties.put("fees", wcsInfo.getFees());
                    properties.put("versions", wcsInfo.getVersions());
                    properties.put("keywords", wcsInfo.getKeywords());
                    properties.put("metadataLink", wcsInfo.getMetadataLink());
                    properties.put("citeCompliant", wcsInfo.isCiteCompliant() ? "true" : "false");
                    properties.put("onlineResource", wcsInfo.getOnlineResource());
                    properties.put("schemaBaseURL", wcsInfo.getSchemaBaseURL());
                    properties.put("verbose", wcsInfo.isVerbose() ? "true" : "false");
                    properties.put("isSubsamplingEnabled", wcsInfo.isSubsamplingEnabled() ? "true"
                            : "false");
                    properties.put("overviewPolicy", wcsInfo.getOverviewPolicy());
                    properties.put("maxInputMemory", String.valueOf(wcsInfo.getMaxInputMemory()));
                    properties.put("maxOutputMemory", String.valueOf(wcsInfo.getMaxOutputMemory()));
                }
            });
            return cfg;
        }
    }
}
