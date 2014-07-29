/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class CatalogResourceBase extends ReflectiveResource {

    /**
     * logger
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver.catalog.rest");
    /**
     * the catalog
     */
    protected Catalog catalog;

    protected GeoServer geoServer;

    /**
     * the class of the resource
     */
    protected Class clazz;
    /**
     * xstream persister factory
     */
    protected XStreamPersisterFactory xpf;
    
    public CatalogResourceBase(Context context,Request request, Response response, Class clazz,
            Catalog catalog) {
        super( context, request, response );
        this.clazz = clazz;
        this.catalog = catalog;
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request,Response response) {
        return new CatalogFreemarkerHTMLFormat( clazz, request, response, this );
    }
    
    protected void encodeAlternateAtomLink( String link, HierarchicalStreamWriter writer ) {
        encodeAlternateAtomLink( link, writer, getFormatGet() );
    }
    
    protected void encodeAlternateAtomLink( String link, HierarchicalStreamWriter writer, DataFormat format ) {
        writer.startNode( "atom:link");
        writer.addAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
        writer.addAttribute( "rel", "alternate" );
        writer.addAttribute( "href", href(link,format) );
        
        if ( format != null ) {
            writer.addAttribute( "type", format.getMediaType().toString() );
        }
        
        writer.endNode();
    }
    
    protected void encodeLink( String link, HierarchicalStreamWriter writer ) {
        encodeLink( link, writer, getFormatGet() );
    }
    
    protected void encodeLink( String link, HierarchicalStreamWriter writer, DataFormat format ) {
        if ( getFormatGet() instanceof ReflectiveXMLFormat  ) {
            //encode as an atom link
            encodeAlternateAtomLink(link, writer, format);
        }
        else {
            //encode as a child element
            writer.startNode( "href" );
            writer.setValue( href( link, format) );
            writer.endNode();
        }
    }
    
    protected void encodeCollectionLink( String link, HierarchicalStreamWriter writer ) {
        encodeCollectionLink( link, writer, getFormatGet() );
    }
    
    protected void encodeCollectionLink( String link, HierarchicalStreamWriter writer, DataFormat format) {
        if ( format instanceof ReflectiveXMLFormat ) {
            //encode as atom link
            encodeAlternateAtomLink(link, writer, format);
        }
        else {
            //encode as a value
            writer.setValue( href( link, format ) );
        }
    }
    
    String href( String link, DataFormat format ) {
        PageInfo pg = getPageInfo();
        
        //try to figure out extension
        String ext = null;
        if ( format != null ) {
            ext = MediaTypes.getExtensionForMediaType( format.getMediaType() );
        }
        
        if ( ext == null ) {
            ext = pg.getExtension();
        }
        
        if(ext != null && ext.length() > 0)
            link = link+ "." + ext;
        
        // encode as relative or absolute depending on the link type
        if ( link.startsWith( "/") ) {
            // absolute, encode from "root"
            return pg.rootURI(link);
        } else {
            //encode as relative
            return pg.pageURI(link);
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
     * Uses messages as a template to update resource.
     * @param message Possibly incomplete ResourceInfo used to update resource
     * @param resource Original resource (to be saved in catalog after modification)
     */
    protected void calculateOptionalFields(ResourceInfo message, ResourceInfo resource) {
        Form form = getRequest().getResourceRef().getQueryAsForm();
        String calculate = form.getFirstValue("recalculate", true);
        List<String> fieldsToCalculate;
        if (calculate == null) {
            boolean changedProjection = message.getSRS() != null;
            boolean changedProjectionPolicy = message.getProjectionPolicy() != null;
            boolean changedNativeBounds = message.getNativeBoundingBox() != null;
            boolean changedLatLonBounds = message.getLatLonBoundingBox() != null;
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
                resource.setNativeBoundingBox(builder.getNativeBounds(resource));
            } catch (IOException e) {
                String errorMessage = "Error while calculating native bounds for layer: " + resource;
                throw new RestletException(errorMessage, Status.SERVER_ERROR_INTERNAL, e);
            }
        }
        if (fieldsToCalculate.contains("latlonbbox")) {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            try {
                resource.setLatLonBoundingBox(builder.getLatLonBounds(
                        resource.getNativeBoundingBox(),
                        resolveCRS(resource.getSRS())));
            } catch (IOException e) {
                String errorMessage =
                        "Error while calculating lat/lon bounds for featuretype: " + resource;
                throw new RestletException(errorMessage, Status.SERVER_ERROR_INTERNAL, e);
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
}
