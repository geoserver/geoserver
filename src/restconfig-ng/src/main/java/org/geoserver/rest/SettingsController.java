package org.geoserver.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;

/**
 * Settings controller
 *
 * Provides access to global settings, local settings, and contact info
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class SettingsController extends GeoServerController {

    @Autowired
    public SettingsController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(value = "/settings", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<GeoServerInfo> getGlobalSettings() {
        return wrapObject(geoServer.getGlobal(), GeoServerInfo.class);
    }

    @PutMapping(value = "/settings", consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void setGlobalSettings(@RequestBody GeoServerInfo geoServerInfo) {
        GeoServerInfo original = geoServer.getGlobal();
        OwsUtils.copy(geoServerInfo, original, GeoServerInfo.class);
        geoServer.save(original);
    }

    @GetMapping(value = "/settings/contact", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<ContactInfo> getContact() {
        if (geoServer.getSettings().getContact() == null) {
            throw new ResourceNotFoundException("No contact information available");
        }
        return wrapObject(geoServer.getGlobal().getSettings().getContact(), ContactInfo.class);
    }

    @PutMapping(value = "/settings/contact", consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void setContact(@RequestBody ContactInfo contactInfo) {
        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        ContactInfo original = geoServerInfo.getSettings().getContact();
        OwsUtils.copy(contactInfo, original, ContactInfo.class);
        geoServer.save(geoServerInfo);
    }

    @GetMapping(value = "/workspaces/{wsName}/settings", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<SettingsInfo> getLocalSettings(@PathVariable String wsName) {
        if (wsName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(wsName);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            if (settingsInfo == null) {
                settingsInfo = new SettingsInfoImpl();
                settingsInfo.setVerbose(false);
            }
            return wrapObject(settingsInfo, SettingsInfo.class);
        }
        throw new RestException("Workspace " + wsName + " not found", HttpStatus.BAD_REQUEST);
    }

    @PostMapping(value = "/workspaces/{wsName}/settings", consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public String createLocalSettings(@PathVariable String wsName, @RequestBody SettingsInfo settingsInfo) {
        String name = "";
        if (wsName != null) {
            Catalog catalog = geoServer.getCatalog();
            WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(wsName);
            settingsInfo.setWorkspace(workspaceInfo);
            geoServer.add(settingsInfo);
            geoServer.save(geoServer.getSettings(workspaceInfo));
            name = settingsInfo.getWorkspace().getName();
        }
        return name;
    }

    @PutMapping(value = "/workspaces/{wsName}/settings", consumes = {
            MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void setLocalSettings(@PathVariable String wsName, @RequestBody SettingsInfo settingsInfo) {
        if (wsName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(wsName);
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

    @DeleteMapping(value = "/workspaces/{wsName}/settings")
    public void deleteLocalSetings(@PathVariable String wsName) {
        if (wsName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(wsName);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            geoServer.remove(settingsInfo);
        }
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ContactInfo.class.isAssignableFrom(methodParameter.getParameterType()) ||
                GeoServerInfo.class.isAssignableFrom(methodParameter.getParameterType()) ||
                SettingsInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("contact", ContactInfo.class);
    }

}
