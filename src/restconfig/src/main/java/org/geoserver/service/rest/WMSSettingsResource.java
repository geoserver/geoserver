/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import java.util.Map;

import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wms.WatermarkInfo;
import org.geoserver.wms.WatermarkInfoImpl;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class WMSSettingsResource extends ServiceSettingsResource {

    public WMSSettingsResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WMSSettingsHTMLFormat(request, response, this);
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
                    service = geoServer.getService(ws, WMSInfo.class);
                } else {
                    service = geoServer.getService(WMSInfo.class);
                }
                return service;
            }
            @Override
            protected Class<WMSInfo> getObjectClass() {
                return WMSInfo.class;
            }
        });
        WMSXStreamLoader.initXStreamPersister(persister);
    }

    static class WMSSettingsHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public WMSSettingsHTMLFormat(Request request, Response response, Resource resource) {
            super(SettingsInfo.class, request, response, resource);
        }

        @Override
        protected String getTemplateName(Object data) {
            return "wmsSettings";
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new ObjectToMapWrapper<WMSInfo>(WMSInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, WMSInfo wmsInfo) {
                    WorkspaceInfo workspaceInfo = wmsInfo.getWorkspace();
                    properties.put("workspaceName", workspaceInfo != null ? workspaceInfo.getName() : "NO_WORKSPACE");
                    properties.put("enabled", wmsInfo.isEnabled() ? "true" : "false");
                    properties.put("name", wmsInfo.getName());
                    properties.put("title", wmsInfo.getTitle());
                    properties.put("maintainer", wmsInfo.getMaintainer());
                    properties.put("abstract", wmsInfo.getAbstract());
                    properties.put("accessConstraints", wmsInfo.getAccessConstraints());
                    properties.put("fees", wmsInfo.getFees());
                    properties.put("versions", wmsInfo.getVersions());
                    properties.put("keywords", wmsInfo.getKeywords());
                    properties.put("metadataLink", wmsInfo.getMetadataLink());
                    properties.put("citeCompliant", wmsInfo.isCiteCompliant() ? "true" : "false");
                    properties.put("onlineResource", wmsInfo.getOnlineResource());
                    properties.put("schemaBaseURL", wmsInfo.getSchemaBaseURL());
                    properties.put("verbose", wmsInfo.isVerbose() ? "true" : "false");
                    properties.put("authorityURLs", wmsInfo.getAuthorityURLs() != null ? new CollectionModel(wmsInfo.getAuthorityURLs(), new ObjectToMapWrapper(AuthorityURL.class)) : "NO_AUTHORITY_URL");
                    properties.put("identifiers", wmsInfo.getIdentifiers() != null ? new CollectionModel(wmsInfo.getIdentifiers(), new ObjectToMapWrapper(LayerIdentifierInfo.class)) : "NO_IDENTIFIER");
                    properties.put("srsList", wmsInfo.getSRS().size() > 0 ? wmsInfo.getSRS() : "NO_SRSList");
                    properties.put("bboxForEachCRS", wmsInfo.isBBOXForEachCRS().toString());
                    properties.put("interpolation", wmsInfo.getInterpolation().name());
                    properties.put("kmlReflectorMode", wmsInfo.getMetadata().get("kmlReflectorMode") != null ? wmsInfo.getMetadata().get("kmlReflectorMode") : "NO_KMLREFLECTORMODE");
                    properties.put("kmlSuperoverlayMode", wmsInfo.getMetadata().get("kmlSuperoverlayMode") != null ? wmsInfo.getMetadata().get("kmlSuperoverlayMode") : "NO_KMLSUPEROVERLAY");
                    properties.put("kmlAttr", wmsInfo.getMetadata().get("kmlAttr") != null ? wmsInfo.getMetadata().get("kmlAttr").toString() : "NO_KMLATTR");
                    properties.put("kmlPlacemark", wmsInfo.getMetadata().get("kmlPlacemark") != null ? wmsInfo.getMetadata().get("kmlPlacemark").toString() : "NO_KMLPLACEMARK");
                    properties.put("kmlKmscore", wmsInfo.getMetadata().get("kmlKmscore") != null ? String.valueOf(wmsInfo.getMetadata().get("kmlKmscore")) : "NO_KMLKMSCORE");
                    properties.put("maxRequestMemory", String.valueOf(wmsInfo.getMaxRequestMemory()));
                    properties.put("maxRenderingTime", String.valueOf(wmsInfo.getMaxRenderingTime()));
                    properties.put("maxRenderindErrors", String.valueOf(wmsInfo.getMaxRenderingErrors()));
                    properties.put("watermarkEnabled", wmsInfo.getWatermark().isEnabled() ? "true" : "false");
                    properties.put("watermarkUrl", wmsInfo.getWatermark().getURL()!= null ? wmsInfo.getWatermark().getURL() : "NO_WATERMARK_URL");
                    properties.put("watermarkTransparency", String.valueOf(wmsInfo.getWatermark().getTransparency()));
                    properties.put("watermarkPosition", wmsInfo.getWatermark().getPosition());
                    properties.put("pngCompression", String.valueOf(wmsInfo.getMetadata().get("pngCompression")));
                    properties.put("jpegCompression", String.valueOf(wmsInfo.getMetadata().get("jpegCompression")));
                    properties.put("maxAllowedFrames", String.valueOf(wmsInfo.getMetadata().get("maxAllowedFrames")));
                    properties.put("maxAnimatorRenderingTime", String.valueOf(wmsInfo.getMetadata().get("maxAnimatorRenderingTime")));
                    properties.put("maxRenderingSize", String.valueOf(wmsInfo.getMetadata().get("maxRenderingSize")));
                    properties.put("framesDelay", String.valueOf(wmsInfo.getMetadata().get("framesDelay")));
                    properties.put("loopContinuosly", String.valueOf(wmsInfo.getMetadata().get("loopContinuosly")));
                    properties.put("svgAntiAlias", String.valueOf(wmsInfo.getMetadata().get("svgAntiAlias")));
                    properties.put("svgRenderer", String.valueOf(wmsInfo.getMetadata().get("svgRenderer")));
                }
            });
            return cfg;
        }
    }
}
