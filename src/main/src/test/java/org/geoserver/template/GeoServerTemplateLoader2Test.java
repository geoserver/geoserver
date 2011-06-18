/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import java.io.IOException;
import java.io.InputStream;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.DataUtilities;
import org.opengis.feature.simple.SimpleFeatureType;

public class GeoServerTemplateLoader2Test extends GeoServerTestSupport {

    GeoServerDataDirectory dd;
    Catalog cat;
    GeoServerTemplateLoader tl;
    
    @Override
    protected boolean useLegacyDataDirectory() {
        return false;
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        dd = getDataDirectory();
        cat = getCatalog();
        tl = new GeoServerTemplateLoader(getClass(),getResourceLoader());
    }
    
    public void testRelativeToFeatureType() throws IOException {
       
        Object source = tl.findTemplateSource( "dummy.ftl");
        assertNull(source);
        
        FeatureTypeInfo ft = cat.getFeatureTypeByName( "PrimitiveGeoFeature");
        dd.copyToResourceDir( ft, template(), "dummy.ftl");
        tl.setFeatureType( ft );
        
        source = tl.findTemplateSource( "dummy.ftl");
        assertNotNull(source);
    }
    
    public void testRelativeToStore() throws IOException {
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        FeatureTypeInfo ft = cat.getFeatureTypeByName( "PrimitiveGeoFeature");
        tl.setFeatureType( ft );
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        dd.copyToStoreDir( ft.getStore(), template(), "dummy.ftl");
        assertNotNull(tl.findTemplateSource( "dummy.ftl"));
    }
    
    public void testRelativeToWorkspace() throws IOException {
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        FeatureTypeInfo ft = cat.getFeatureTypeByName( "PrimitiveGeoFeature");
        tl.setFeatureType( ft );
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        dd.copyToWorkspaceDir( ft.getStore().getWorkspace(), template(), "dummy.ftl");
        assertNotNull(tl.findTemplateSource( "dummy.ftl"));
    }
    
    public void testGlobal() throws IOException {
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        FeatureTypeInfo ft = cat.getFeatureTypeByName( "PrimitiveGeoFeature");
        tl.setFeatureType( ft );
        assertNull(tl.findTemplateSource( "dummy.ftl"));
        
        dd.copyToWorkspacesDir( template(), "dummy.ftl" );
        assertNotNull(tl.findTemplateSource( "dummy.ftl"));
    }
    
    public void testRemoteType() throws Exception {
        SimpleFeatureType ft = DataUtilities.createType("remoteType", "the_geom:MultiPolygon,FID:String,ADDRESS:String");
        tl.setFeatureType(ft);
        tl.findTemplateSource("header.ftl");
    }
    
    InputStream template() throws IOException {
        return getClass().getResourceAsStream( "dummy.ftl.disabled");
    }
}
