/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.wfs.WFSInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CoverageResource extends AbstractCatalogResource {

    public CoverageResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
        
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(CoverageInfo.class, request, response, this);
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");
        String coverage = getAttribute( "coverage" );

        if ( coveragestore == null ) {
            LOGGER.fine( "GET coverage " + workspace + "," + coverage );
            //grab the corresponding namespace for this workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix( workspace );
            if ( ns != null ) {
                return catalog.getCoverageByName(ns,coverage);
            }

            throw new RestletException( "", Status.CLIENT_ERROR_NOT_FOUND );
        }

        LOGGER.fine( "GET coverage " + coveragestore + "," + coverage );
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        return catalog.getCoverageByCoverageStore( cs, coverage );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("coverage") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace");
        String coveragestore = getAttribute( "coveragestore");

        CoverageInfo coverage = (CoverageInfo) object;
        if ( coverage.getStore() == null ) {
            //get from requests
            CoverageStoreInfo ds = catalog.getCoverageStoreByName( workspace, coveragestore );
            coverage.setStore( ds );
        }
        final boolean isNew = isNewCoverage(coverage);
        String nativeCoverageName = coverage.getNativeCoverageName();
        if (nativeCoverageName == null) {
            nativeCoverageName = coverage.getNativeName();
        }
        CatalogBuilder builder = new CatalogBuilder(catalog);
        CoverageStoreInfo store = coverage.getStore();
        builder.setStore(store);

        // We handle 2 different cases here
        if (!isNew) {
            // Configuring a partially defined coverage
            builder.initCoverage(coverage, nativeCoverageName);
        } else {
            // Configuring a brand new coverage (only name has been specified)
            String specifiedName = coverage.getName();
            coverage = builder.buildCoverageByName(nativeCoverageName, specifiedName);
        }

        NamespaceInfo ns = coverage.getNamespace();
        if ( ns != null && !ns.getPrefix().equals( workspace ) ) {
            //TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning( "Namespace: " + ns.getPrefix() + " does not match workspace: " + workspace + ", overriding." );
            ns = null;
        }
        
        if ( ns == null){
            //infer from workspace
            ns = catalog.getNamespaceByPrefix( workspace );
            coverage.setNamespace( ns );
        }
        
        coverage.setEnabled(true);
        catalog.validate(coverage, true).throwIfInvalid();
        catalog.add( coverage );
        
        //create a layer for the coverage
        catalog.add(builder.buildLayer(coverage));
        
        LOGGER.info( "POST coverage " + coveragestore + "," + coverage.getName() );
        return coverage.getName();
    }

    /**
     * This method returns {@code true} in case we have POSTed a Coverage object with the name only, as an instance
     * when configuring a new coverage which has just been harvested. 
     * 
     * @param coverage
     *
     */
    private boolean isNewCoverage(CoverageInfo coverage) {
        return coverage.getName() != null && (coverage.isAdvertised()) && (!coverage.isEnabled())
                && (coverage.getAlias() == null) && (coverage.getCRS() == null)
                && (coverage.getDefaultInterpolationMethod() == null)
                && (coverage.getDescription() == null) && (coverage.getDimensions() == null)
                && (coverage.getGrid() == null) && (coverage.getInterpolationMethods() == null)
                && (coverage.getKeywords() == null) && (coverage.getLatLonBoundingBox() == null)
                && (coverage.getMetadata() == null) && (coverage.getNativeBoundingBox() == null)
                && (coverage.getNativeCRS() == null) && (coverage.getNativeFormat() == null)
                && (coverage.getProjectionPolicy() == null) && (coverage.getSRS() == null)
                && (coverage.getResponseSRS() == null) && (coverage.getRequestSRS() == null);
    }

    @Override
    public boolean allowPut() {
        return getAttribute("coverage") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        CoverageInfo c = (CoverageInfo) object;
        
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String coverage = getAttribute("coverage");
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        CoverageInfo original = catalog.getCoverageByCoverageStore( cs,  coverage );
        calculateOptionalFields(c, original);
        
        new CatalogBuilder(catalog).updateCoverage(original,c);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save( original );
        
        clear(original);
        
        LOGGER.info( "PUT coverage " + coveragestore + "," + coverage );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("coverage") != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        String coverage = getAttribute("coverage");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        CoverageStoreInfo ds = catalog.getCoverageStoreByName(workspace, coveragestore);
        CoverageInfo c = catalog.getCoverageByCoverageStore( ds,  coverage );
        List<LayerInfo> layers = catalog.getLayers(c);
        
        if (recurse) {
            //by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info( "DELETE layer " + l.getName());
            }
        }
        else {
            if (!layers.isEmpty()) {
                throw new RestletException( "coverage referenced by layer(s)", Status.CLIENT_ERROR_FORBIDDEN);
            }
        }
        
        catalog.remove( c );
        clear(c);
        
        LOGGER.info( "DELETE coverage " + coveragestore + "," + coverage );
    }
    
    void clear(CoverageInfo info) {
        catalog.getResourcePool().clear(info.getStore());
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected Class<CoverageInfo> getObjectClass() {
                return CoverageInfo.class;
            }
            @Override
            protected CatalogInfo getCatalogObject() {
                String workspace = getAttribute("workspace");
                String coveragestore = getAttribute("coveragestore");
                String coverage = getAttribute("coverage");
                
                if (workspace == null || coveragestore == null || coverage == null) {
                    return null;
                }
                CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
                if (cs == null) {
                    return null;
                }
                return catalog.getCoverageByCoverageStore( cs,  coverage );
            }
            @Override
            protected void postEncodeReference(Object obj, String ref, String prefix, 
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if ( obj instanceof NamespaceInfo ) {
                    NamespaceInfo ns = (NamespaceInfo) obj;
                    encodeLink("/namespaces/" + encode(ns.getPrefix()), writer);
                }
                if ( obj instanceof CoverageStoreInfo ) {
                    CoverageStoreInfo cs = (CoverageStoreInfo) obj;
                    encodeLink("/workspaces/" + encode(cs.getWorkspace().getName()) +
                        "/coveragestores/" + encode(cs.getName()), writer );
                    
                }
            }
        });
    }

}
