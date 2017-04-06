package org.geoserver.rest;

import freemarker.template.ObjectWrapper;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.rest.catalog.CatalogController;
import org.geoserver.config.*;
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
import java.util.Arrays;

/**
 * Settings controller
 *
 * Provides access to global settings, local settings, and contact info
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/settings")
public class LocalSettingsController extends GeoServerController {

    @Autowired
    public LocalSettingsController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public RestWrapper<SettingsInfo> getLocalSettings(@PathVariable String workspaceName) {

        if (workspaceName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            if (settingsInfo == null) {
                settingsInfo = new SettingsInfoImpl();
                settingsInfo.setVerbose(false);
            }
            return wrapObject(settingsInfo, SettingsInfo.class);
        }
        throw new RestException("Workspace " + workspaceName + " not found", HttpStatus.BAD_REQUEST);
    }

    @PostMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    public String createLocalSettings(@PathVariable String workspaceName, @RequestBody SettingsInfo settingsInfo) {
        String name = "";
        if (workspaceName != null) {
            Catalog catalog = geoServer.getCatalog();
            WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
            settingsInfo.setWorkspace(workspaceInfo);
            geoServer.add(settingsInfo);
            geoServer.save(geoServer.getSettings(workspaceInfo));
            name = settingsInfo.getWorkspace().getName();
        }
        return name;
    }

    @PutMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE })
    public void setLocalSettings(@PathVariable String workspaceName, @RequestBody SettingsInfo settingsInfo) {
        if (workspaceName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
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

    @DeleteMapping
    public void deleteLocalSetings(@PathVariable String workspaceName) {
        if (workspaceName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            geoServer.remove(settingsInfo);
        }
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return SettingsInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected String getTemplateName(Object object) {
        return "localSettings";
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Arrays.asList(WorkspaceInfo.class));
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("contact", ContactInfo.class);
    }

}
