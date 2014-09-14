/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class LocalSettingsResource extends AbstractCatalogResource {

    private GeoServer geoServer;

    public LocalSettingsResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
        this.geoServer = geoServer;
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new LocalSettingsHTMLFormat(request, response, this);
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        return allowExisting();
    }

    private boolean allowExisting() {
        String workspace = getAttribute("workspace");
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspace);
        if (ws != null) {
            return geoServer.getSettings(ws) != null;
        }
        return geoServer.getSettings(ws) == null;
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspace);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            if (settingsInfo == null) {
                settingsInfo = new SettingsInfoImpl();
                settingsInfo.setVerbose(false);
            }
            return settingsInfo;
        }
        throw new RestletException("Workspace " + workspace + " not found",
                Status.CLIENT_ERROR_BAD_REQUEST);
    }

    @Override
    protected String handleObjectPost(Object obj) throws Exception {
        String name = "";
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            Catalog catalog = geoServer.getCatalog();
            WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspace);
            SettingsInfo settings = (SettingsInfo) obj;
            settings.setWorkspace(workspaceInfo);
            geoServer.add(settings);
            geoServer.save(geoServer.getSettings(workspaceInfo));
            name = settings.getWorkspace().getName();
        }
        return name;
    }

    @Override
    protected void handleObjectPut(Object obj) throws Exception {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspace);
            SettingsInfo settingsInfo = (SettingsInfo) obj;
            SettingsInfo original = geoServer.getSettings(workspaceInfo);
            if (original == null) {
                settingsInfo.setWorkspace(workspaceInfo);
                geoServer.add(settingsInfo);
                geoServer.save(geoServer.getSettings(workspaceInfo));
            } else {
                OwsUtils.copy(settingsInfo, original, SettingsInfo.class);
                original.setWorkspace(workspaceInfo);
                geoServer.save(original);
            }
        }
    }

    @Override
    public void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        if (workspace != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspace);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            geoServer.remove(settingsInfo);
        }
    }

    static class LocalSettingsHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public LocalSettingsHTMLFormat(Request request, Response response, Resource resource) {
            super(SettingsInfo.class, request, response, resource);
        }

        @Override
        protected String getTemplateName(Object data) {
            return "localSettings";
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new ObjectToMapWrapper<SettingsInfo>(SettingsInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model,
                        SettingsInfo settingsInfo) {
                    WorkspaceInfo workspaceInfo = settingsInfo.getWorkspace();
                    properties.put("workspaceName",
                            settingsInfo.getWorkspace() != null ? workspaceInfo.getName()
                                    : "NO_WORKSPACE");
                    ContactInfo contactInfo = settingsInfo.getContact();
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
                    properties.put("proxyBaseUrl",
                            settingsInfo.getProxyBaseUrl() != null ? settingsInfo.getProxyBaseUrl()
                                    : "");

                }
            });
            return cfg;
        }
    }
}
