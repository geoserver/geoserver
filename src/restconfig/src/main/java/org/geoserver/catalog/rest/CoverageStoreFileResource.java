/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.SingleGridCoverage2DReader;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.factory.GeoTools;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
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

    /**
     * Post is allowed only if the reader in question is structured and can do harvesting 
     */
    @Override
    public boolean allowPost() {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        
        // check the coverage store exists
        CoverageStoreInfo info = catalog.getCoverageStoreByName(workspace, coveragestore);
        if(info == null) {
            return false;
        }

        try {
            GridCoverageReader reader = info.getGridCoverageReader(null, null);
            if(reader instanceof StructuredGridCoverage2DReader) {
                StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
                return !sr.isReadOnly();
            } else {
                return false;
            }
        } catch(IOException e) {
            throw new RestletException("Failed to access the existing reader to " +
            		"check if it can harvest new files", Status.SERVER_ERROR_INTERNAL);
        }
    }
    
    @Override
    public void handlePost() {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String format = getAttribute("format");
        Request request = getRequest();
        String method = getUploadMethod(request);

        
        // theoretically allowPost was just called, so all these should not need a check
        try {
            CoverageStoreInfo info = catalog.getCoverageStoreByName(workspace, coveragestore);
            GridCoverageReader reader = info.getGridCoverageReader(null, null);
            StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
            // This method returns a List of the harvested files.
            final List<File> uploadedFiles = doFileUpload(method, workspace, coveragestore, format);
            // File Harvesting
            sr.harvest(null, uploadedFiles, GeoTools.getDefaultHints());
        } catch(IOException e) {
            throw new RestletException("File harvest failed", Status.SERVER_ERROR_INTERNAL, e);
        }
    }
    
    @Override
    public void handlePut() {
        Request request = getRequest();
        Response response = getResponse();
        
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String format = getAttribute("format");
        String method = getUploadMethod(request);
        
        // doFileUpload returns a List of File but in the case of a Put operation the list contains only a value
        final File uploadedFile = doFileUpload(method, workspace, coveragestore, format).get(0);
        
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
        URL uploadedFileURL = DataUtilities.fileToURL(uploadedFile);
        if (isInlineUpload(method)) {
            //TODO: create a method to figure out the relative url instead of making assumption
            // about the structure
            
            String defaultRoot = File.separator + "data" + File.separator + workspace + File.separator + coveragestore;
            
            StringBuilder fileBuilder = new StringBuilder(uploadedFile.getAbsolutePath());
            
            String url;
            if(uploadedFile.isDirectory() && uploadedFile.getName().equals(coveragestore)) {

                int def = fileBuilder.indexOf(defaultRoot);
                
                if(def >= 0){
                    url = "file:data/" + workspace + "/" + coveragestore;
                }else{
                    url = fileBuilder.toString();
                }
            } else {

                int def = fileBuilder.indexOf(defaultRoot);
                
                if(def >= 0){
                    
                    String itemPath = fileBuilder.substring(def + defaultRoot.length());
                    
                    url = "file:data/" + workspace + "/" + coveragestore + "/" + itemPath;
                }else{
                    url = fileBuilder.toString();
                }
            }
            if (url.contains("+")) {
                url = url.replace("+", "%2B");
            }
            if (url.contains(" ")) {
                url = url.replace(" ", "%20");
            }
            info.setURL(url);
        }
        else {
            info.setURL( uploadedFileURL.toExternalForm());
        }
        
        //add or update the datastore info
        if ( add ) {
        	if (!catalog.validate(info, true).isValid()) {
        		throw new RuntimeException("Validation failed");
        	}
            catalog.add( info );
        }
        else {
        	if (!catalog.validate(info, false).isValid()) {
        		throw new RuntimeException("Validation failed");
        	}
            catalog.save( info );
        }
        
        builder.setStore(info);
        
        //check configure parameter, if set to none to not try to configure coverage
        String configure = form.getFirstValue( "configure" );
        if ( "none".equalsIgnoreCase( configure ) ) {
            getResponse().setStatus( Status.SUCCESS_CREATED );
            return;
        }
        
        GridCoverage2DReader reader = null;
        try {
            reader = 
                (GridCoverage2DReader) ((AbstractGridFormat) coverageFormat).getReader(DataUtilities.fileToURL(uploadedFile));
            if ( reader == null ) {
                throw new RestletException( "Could not aquire reader for coverage.", Status.SERVER_ERROR_INTERNAL);
            }
            
            // coverage read params
            final Map customParameters = new HashMap();
            String useJAIImageReadParam = form.getFirstValue("USE_JAI_IMAGEREAD");
            if (useJAIImageReadParam != null) {
            	customParameters.put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), Boolean.valueOf(useJAIImageReadParam));
            }
            
            //check if the name of the coverage was specified
            String coverageName = form.getFirstValue("coverageName");
            String[] names = reader.getGridCoverageNames();
            
            if(names.length > 1 && coverageName != null) {
                throw new RestletException("The reader found more than one coverage, " +
                		"coverageName cannot be used in this case (it would generate " +
                		"the same name for all coverages found", Status.CLIENT_ERROR_BAD_REQUEST);
            }
            
            // configure all available coverages, preserving backwards compatibility for the
            // case of single coverage reader
            if(names.length > 1) {
                for (String name : names) {
                    SingleGridCoverage2DReader singleReader = new SingleGridCoverage2DReader(reader, name);
                    configureCoverageInfo(builder, info, add, name, name, singleReader,
                            customParameters);
                }
            } else {
                configureCoverageInfo(builder, info, add, names[0], coverageName, reader,
                        customParameters);
            }
            
            
            //poach the coverage store data format
            DataFormat df = new CoverageStoreResource(getContext(),request,response,catalog).createXMLFormat(request, response);
            response.setEntity(df.toRepresentation(info));
            response.setStatus(Status.SUCCESS_CREATED);
        }
        catch( Exception e ) {
            if(e instanceof RestletException) {
                throw (RestletException) e;
            }
            throw new RestletException( "Error auto-configuring coverage", Status.SERVER_ERROR_INTERNAL, e );
        } finally {
            if(reader != null) {
                try {
                    reader.dispose();
                } catch(IOException e)  {
                    // it's ok, we tried
                }            
            }
        }
    }

    private void configureCoverageInfo(CatalogBuilder builder, CoverageStoreInfo storeInfo,
            boolean add, String nativeName, String coverageName, GridCoverage2DReader reader,
            final Map customParameters) throws Exception, IOException {
        CoverageInfo cinfo = builder.buildCoverage( reader, customParameters );
        
        if (coverageName != null) {
            cinfo.setName(coverageName);
        }
        if (nativeName != null) {
            cinfo.setNativeCoverageName(nativeName);
        }
        
        if ( !add ) {
            //update the existing
            String name = coverageName != null ? coverageName: nativeName;
            CoverageInfo existing = catalog.getCoverageByCoverageStore(storeInfo, name);
            if ( existing == null ) {
                //grab the first if there is only one
                List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore( storeInfo);
                // single coverage reader?
                if ( coverages.size() == 1 && coverages.get(0).getNativeName() == null) {
                    existing = coverages.get(0);
                }
                // check if we have it or not
                if ( coverages.size() == 0 ) {
                    // no coverages yet configured, change add flag and continue on
                    add = true;
                } else {
                    for (CoverageInfo ci : coverages) {
                        if(ci.getNativeName().equals(name)) {
                            existing = ci;
                        }
                    }
                    if(existing == null) {
                        add = true;
                    }
                }
            }
            
            if ( existing != null ) {
                builder.updateCoverage(existing,cinfo);
                catalog.validate(existing, false).throwIfInvalid();
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
            catalog.validate(cinfo, true).throwIfInvalid();
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
                if (!catalog.validate(layerInfo, true).isValid()) {
                    valid = false;
                }
            } catch (Exception e) {
                valid = false;
            }

            layerInfo.setEnabled(valid);
            catalog.add(layerInfo);
        } else {
            catalog.save( cinfo );
        }
    }

    protected File findPrimaryFile(File directory, String format) {
        // first check if the format accepts a whole directory
        if ( ((AbstractGridFormat)coverageFormat).accepts(directory) ) {
            return directory;
        }
        for ( File f : directory.listFiles() ) {
            if(f.isDirectory()){
                File result = findPrimaryFile(f,format);
                if(result!=null){
                    return result;
                }
            }else{
                if ( ((AbstractGridFormat)coverageFormat).accepts(f) ) {
                    return f;
                }
            }
        }
        
        return null;
    }
}
