/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import java.util.Map;

import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.format.DataFormat;
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
public class GlobalSettingsResource extends AbstractCatalogResource {

    protected GeoServer geoServer;

    public GlobalSettingsResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
        this.geoServer = geoServer;
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new GlobalSettingsHTMLFormat(request, response, this);
    }

    @Override
    public boolean allowPut() {
        return allowExisting();
    }

    private boolean allowExisting() {
        return geoServer.getGlobal().getSettings() != null;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        return geoServer.getGlobal();
    }

    @Override
    public void handleObjectPut(Object object) throws Exception {
        GeoServerInfo geoServerInfo = (GeoServerInfo) object;
        GeoServerInfo original = geoServer.getGlobal();
        ContactInfo contactInfo = original.getSettings().getContact();
        OwsUtils.copy(geoServerInfo, original, GeoServerInfo.class);
        original.getSettings().setContact(contactInfo);
        geoServer.save(original);
    }

    static class GlobalSettingsHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public GlobalSettingsHTMLFormat(Request request, Response response, Resource resource) {
            super(GeoServerInfo.class, request, response, resource);
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new ObjectToMapWrapper<GeoServerInfo>(GeoServerInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, GeoServerInfo info) {
                    SettingsInfo settingsInfo = info.getSettings();
                    ContactInfo contactInfo = settingsInfo.getContact();
                    JAIInfo jaiInfo = info.getJAI();
                    CoverageAccessInfo covInfo = info.getCoverageAccess();
                    properties.put("contactPerson",
                            contactInfo.getContactPerson() != null ? contactInfo.getContactPerson()
                                    : "");
                    properties.put(
                            "contactOrganization",
                            contactInfo.getContactOrganization() != null ? contactInfo
                                    .getContactOrganization() : "");
                    properties.put(
                            "contactPosition",
                            contactInfo.getContactPosition() != null ? contactInfo
                                    .getContactPosition() : "");
                    properties.put("addressType",
                            contactInfo.getAddressType() != null ? contactInfo.getAddressType()
                                    : "");
                    properties.put("address",
                            contactInfo.getAddress() != null ? contactInfo.getAddress() : "");
                    properties.put("addressCity",
                            contactInfo.getAddressCity() != null ? contactInfo.getAddressCity()
                                    : "");
                    properties.put("addressState",
                            contactInfo.getAddressState() != null ? contactInfo.getAddressState()
                                    : "");
                    properties.put(
                            "addressPostalCode",
                            contactInfo.getAddressPostalCode() != null ? contactInfo
                                    .getAddressPostalCode() : "");
                    properties.put(
                            "addressCountry",
                            contactInfo.getAddressCountry() != null ? contactInfo
                                    .getAddressCountry() : "");
                    properties.put("contactVoice",
                            contactInfo.getContactVoice() != null ? contactInfo.getContactVoice()
                                    : "");
                    properties.put(
                            "contactFacsimile",
                            contactInfo.getContactFacsimile() != null ? contactInfo
                                    .getContactFacsimile() : "");
                    properties.put("contactEmail",
                            contactInfo.getContactEmail() != null ? contactInfo.getContactEmail()
                                    : "");
                    properties.put("verbose", settingsInfo.isVerbose() ? "true" : "false");
                    properties.put("verboseExceptions", settingsInfo.isVerboseExceptions() ? "true"
                            : "false");
                    properties.put("numDecimals", String.valueOf(settingsInfo.getNumDecimals()));
                    properties.put("charset", settingsInfo.getCharset());
                    properties.put(
                            "onlineResource",
                            settingsInfo.getOnlineResource() != null ? settingsInfo
                                    .getOnlineResource() : "");
                    properties.put("proxyBaseUrl",
                            settingsInfo.getProxyBaseUrl() != null ? settingsInfo.getProxyBaseUrl()
                                    : "");
                    properties.put("allowInterpolation", jaiInfo.getAllowInterpolation() ? "true"
                            : "false");
                    properties.put("recycling", jaiInfo.isRecycling() ? "true" : "false");
                    properties.put("tilePriority", String.valueOf(jaiInfo.getTilePriority()));
                    properties.put("tileThreads", String.valueOf(jaiInfo.getTileThreads()));
                    properties.put("memoryCapacity", jaiInfo.getMemoryCapacity());
                    properties.put("memoryThreshold", jaiInfo.getMemoryThreshold());
                    properties.put("imageIOCache", jaiInfo.isImageIOCache() ? "true" : "false");
                    properties.put("pngEncoderType", jaiInfo.getPngEncoderType().toString());
                    properties.put("jpegAcceleration", jaiInfo.isJpegAcceleration() ? "true"
                            : "false");
                    properties.put("allowNativeMosaic", jaiInfo.isAllowNativeMosaic() ? "true"
                            : "false");
                    properties.put("maxPoolSize", String.valueOf(covInfo.getMaxPoolSize()));
                    properties.put("corePoolSize", String.valueOf(covInfo.getCorePoolSize()));
                    properties.put("keepAliveTime", String.valueOf(covInfo.getKeepAliveTime()));
                    properties.put("queueType", covInfo.getQueueType());
                    properties.put("imageIOCacheThreshold",
                            String.valueOf(covInfo.getImageIOCacheThreshold()));
                }
            });
            return cfg;
        }
    }
}
