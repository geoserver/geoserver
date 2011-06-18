/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.opengis.coverage.grid.Format;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class CoverageStoreFileResource extends StoreFileResource {

    Format coverageFormat;
    
    public CoverageStoreFileResource(Request request, Response response,
            Format coverageFormat, Catalog catalog) {
        super(request, response, catalog);
        this.coverageFormat = coverageFormat;
    }
    
    @Override
    public void handlePut() {
        Request request = getRequest();
        Response response = getResponse();
        
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String format = getAttribute("format");
        String method = getUploadMethod(request);
        
        final File uploadedFile = doFileUpload(method, workspace, coveragestore, format);
        
        // /////////////////////////////////////////////////////////////////////
        //
        // Add overviews to the Coverage
        //
        // /////////////////////////////////////////////////////////////////////
        Form form = request.getResourceRef().getQueryAsForm();
        if ("yes".equalsIgnoreCase(form.getFirstValue("overviews")) ) {
            /* TODO: Add overviews here */;
        }
            
        //create a builder to help build catalog objects
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace( catalog.getWorkspaceByName( workspace ) );
        
        //create the coverage store
        CoverageStoreInfo info = catalog.getCoverageStoreByName(workspace, coveragestore);
        boolean add = false;
        if ( info == null ) {
            //create a new coverage store
            LOGGER.info("Auto-configuring coverage store: " + coveragestore);
            
            info = builder.buildCoverageStore(coveragestore);
            add = true;
        }
        else {
            //use the existing
            LOGGER.info("Using existing coverage store: " + coveragestore);
        }
        
        info.setType(coverageFormat.getName());
        if (isInlineUpload(method)) {
            //TODO: create a method to figure out the relative url instead of making assumption
            // about the structure
            info.setURL("file:data/" + workspace + "/" + coveragestore + "/" + uploadedFile.getName() );
        }
        else {
            try {
                info.setURL( uploadedFile.toURL().toExternalForm());
            } catch (MalformedURLException e) {
                throw new RestletException( "url error", Status.SERVER_ERROR_INTERNAL, e );
            }
        }
        
        //add or update the datastore info
        if ( add ) {
            catalog.add( info );
        }
        else {
            catalog.save( info );
        }
        
        builder.setStore(info);
        
        //check configure parameter, if set to none to not try to configure coverage
        String configure = form.getFirstValue( "configure" );
        if ( "none".equalsIgnoreCase( configure ) ) {
            getResponse().setStatus( Status.SUCCESS_CREATED );
            return;
        }
        
        String coverage = uploadedFile.getName();
        if ( coverage.indexOf( '.') != -1 ) { 
            coverage = coverage.substring( 0, coverage.indexOf( '.') );
        }
        
        try {
            AbstractGridCoverage2DReader reader = 
                (AbstractGridCoverage2DReader) ((AbstractGridFormat) coverageFormat).getReader(uploadedFile.toURL());
            if ( reader == null ) {
                throw new RestletException( "Could not aquire reader for coverage.", Status.SERVER_ERROR_INTERNAL );
            }
            
            // coverage read params
            final Map customParameters = new HashMap();
            String useJAIImageReadParam = form.getFirstValue("USE_JAI_IMAGEREAD");
            if (useJAIImageReadParam != null) {
            	customParameters.put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), Boolean.valueOf(useJAIImageReadParam));
            }
            
            CoverageInfo cinfo = builder.buildCoverage( reader, customParameters );
            
            //check if the name of the coverage was specified
            String coverageName = form.getFirstValue("coverageName");
            if ( coverageName != null ) {
                cinfo.setName( coverageName );
            }
            
            if ( !add ) {
                //update the existing
                CoverageInfo existing = catalog.getCoverageByCoverageStore(info, 
                    coverageName != null ? coverageName : coverage );
                if ( existing == null ) {
                    //grab the first if there is only one
                    List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore( info);
                    if ( coverages.size() == 1 ) {
                        existing = coverages.get(0);
                    }
                    if ( coverages.size() == 0 ) {
                        //no coverages yet configured, change add flag and continue on
                        add = true;
                    }
                    else {
                        // multiple coverages, and one to configure not specified
                        throw new RestletException( "Unable to determine coverage to configure.", Status.SERVER_ERROR_INTERNAL);
                    }
                }
                
                if ( existing != null ) {
                    builder.updateCoverage(existing,cinfo);
                    catalog.save( existing );
                    cinfo = existing;
                }
            }
            
            //do some post configuration, if srs is not known or unset, transform to 4326
            if ("UNKNOWN".equals(cinfo.getSRS())) {
                //CoordinateReferenceSystem sourceCRS = cinfo.getBoundingBox().getCoordinateReferenceSystem();
                //CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
                //ReferencedEnvelope re = cinfo.getBoundingBox().transform(targetCRS, true);
                cinfo.setSRS( "EPSG:4326" );
                //cinfo.setCRS( targetCRS );
                //cinfo.setBoundingBox( re );
            }

            //add/save
            if ( add ) {
                catalog.add( cinfo );
                
                LayerInfo layerInfo = builder.buildLayer( cinfo );
                //JD: commenting this out, these sorts of edits should be handled
                // with a second PUT request on the created coverage
                /*
                String styleName = form.getFirstValue("style");
                if ( styleName != null ) {
                    StyleInfo style = catalog.getStyleByName( styleName );
                    if ( style != null ) {
                        layerInfo.setDefaultStyle( style );
                        if ( !layerInfo.getStyles().contains( style ) ) {
                            layerInfo.getStyles().add( style );
                        }
                    }
                    else {
                        LOGGER.warning( "Client specified style '" + styleName + "'but no such style exists.");
                    }
                }

                String path = form.getFirstValue( "path");
                if ( path != null ) {
                    layerInfo.setPath( path );
                }
                */

                boolean valid = true;
                try {
                    if (!catalog.validate(layerInfo, true).isEmpty()) {
                        valid = false;
                    }
                } catch (Exception e) {
                    valid = false;
                }

                layerInfo.setEnabled(valid);
                catalog.add(layerInfo);
            }
            else {
                catalog.save( cinfo );
                
                //TODO: update the layers pointing at this coverage
            }
            
            //poach the coverage store data format
            DataFormat df = new CoverageStoreResource(getContext(),request,response,catalog).createXMLFormat(request, response);
            response.setEntity(df.toRepresentation(info));
            response.setStatus(Status.SUCCESS_CREATED);
        }
        catch( Exception e ) {
            throw new RestletException( "Error auto-configuring coverage", Status.SERVER_ERROR_INTERNAL, e );
        }
    }

    protected File findPrimaryFile(File directory, String format) {
        for ( File f : directory.listFiles() ) {
            if ( ((AbstractGridFormat)coverageFormat).accepts(f) ) {
                return f;
            }
        }
        
        return null;
    }
}
