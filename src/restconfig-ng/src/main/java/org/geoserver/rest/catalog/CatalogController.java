/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base controller for catalog info requests
 */
public abstract class CatalogController extends RestBaseController {
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String TEXT_JSON = "text/json";
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final MediaType MEDIATYPE_TEXT_JSON = MediaType.valueOf(CatalogController.TEXT_JSON);
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String TEXT_XML = "text/xml";
    
    public static final MediaType MEDIATYPE_TEXT_XML =  MediaType.valueOf(TEXT_XML);
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String APPLICATION_ZIP = "application/zip";
    
    public static final String MEDIATYPE_FTL_EXTENSION = "ftl";
    public static final String MEDIATYPE_FTL_VALUE = "text/plain";
    public static final MediaType MEDIATYPE_FTL = new MediaType("text","plain");

    public static final String APPLICATION_XSLT = "application/xslt+xml";

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;

    protected final List<String> validImageFileExtensions;

    public CatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.validImageFileExtensions = Arrays.asList("svg", "png", "jpg");
    }

    /**
     * Uses messages as a template to update resource.
     * @param message Possibly incomplete ResourceInfo used to update resource
     * @param resource Original resource (to be saved in catalog after modification)
     */
    protected void calculateOptionalFields(ResourceInfo message, ResourceInfo resource, String calculate) {
        List<String> fieldsToCalculate;
        if (calculate == null || calculate.isEmpty()) {
            boolean changedProjection = message.getSRS() == null ||
                    !message.getSRS().equals(resource.getSRS());
            boolean changedProjectionPolicy = message.getProjectionPolicy() == null ||
                    !message.getProjectionPolicy().equals(resource.getProjectionPolicy());
            boolean changedNativeBounds = message.getNativeBoundingBox() == null ||
                    !message.getNativeBoundingBox().equals(resource.getNativeBoundingBox());
            boolean changedLatLonBounds = message.getLatLonBoundingBox() == null ||
                    !message.getLatLonBoundingBox().equals(resource.getLatLonBoundingBox());
            boolean changedNativeInterpretation = changedProjectionPolicy || changedProjection;
            fieldsToCalculate = new ArrayList<String>();
            if (changedNativeInterpretation && !changedNativeBounds) {
                fieldsToCalculate.add("nativebbox");
            }
            if ((changedNativeInterpretation || changedNativeBounds) && !changedLatLonBounds) {
                fieldsToCalculate.add("latlonbbox");
            }
        } else {
            fieldsToCalculate = Arrays.asList(calculate.toLowerCase().split(","));
        }

        if (fieldsToCalculate.contains("nativebbox")) {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            try {
                message.setNativeBoundingBox(builder.getNativeBounds(message));
            } catch (IOException e) {
                String errorMessage = "Error while calculating native bounds for layer: " + message;
                throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
        if (fieldsToCalculate.contains("latlonbbox")) {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            try {
                message.setLatLonBoundingBox(builder.getLatLonBounds(
                        message.getNativeBoundingBox(),
                        resolveCRS(message.getSRS())));
            } catch (IOException e) {
                String errorMessage =
                        "Error while calculating lat/lon bounds for featuretype: " + message;
                throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    private CoordinateReferenceSystem resolveCRS(String srs) {
        if ( srs == null ) {
            return null;
        }
        try {
            return CRS.decode(srs);
        } catch(Exception e) {
            throw new RuntimeException("This is unexpected, the layer seems to be mis-configured", e);
        }
    }
    
    /**
     * Determines if the current user is authenticated as full administrator.
     */
    protected boolean isAuthenticatedAsAdmin() {
        if (SecurityContextHolder.getContext() == null) {
            return false;
        }
        return GeoServerExtensions.bean(GeoServerSecurityManager.class).
                checkAuthenticationForAdminRole();
    }
    
    /**
     * Validates the current user can edit the resource (full admin required if workspaceName is null)
     * @param workspaceName
     */
    protected void checkFullAdminRequired(String workspaceName) {
        // global workspaces/styles can only be edited by a full admin
        if (workspaceName == null && !isAuthenticatedAsAdmin()) {
            throw new RestException("Cannot edit global resource , full admin credentials required", 
                    HttpStatus.METHOD_NOT_ALLOWED);
        }
    }
}
