/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.decorators.SecuredLayerGroupInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * 
 * @author Niels Charlier
 *
 */
public class SecuredLayerGroupTest extends GeoServerSystemTestSupport {
    
    @Test
    public void testCreateNewLayerGroup() throws Exception {
        //create mocks
        final LayerGroupInfo lg = createNiceMock(LayerGroupInfo.class);        
        final CatalogFactory factory = createNiceMock(CatalogFactory.class);  
        final Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getFactory()).andReturn(factory);        
        replay(catalog);
        expect(factory.createLayerGroup()).andReturn(lg);     
        replay(factory);
        
        //tests
        final Catalog secureCatalog = new SecureCatalogImpl(catalog);
        final LayerGroupInfo layerGroup = secureCatalog.getFactory().createLayerGroup();
        
        assertTrue(layerGroup instanceof SecuredLayerGroupInfo);
        assertTrue(((SecuredLayerGroupInfo) layerGroup).unwrap(LayerGroupInfo.class) == lg);
    }
    
    @Test
    public void testGetLayerGroup() throws Exception {
        //create mocks
        final LayerGroupInfo lg = createNiceMock(LayerGroupInfo.class);
        expect(lg.getWorkspace()).andReturn(null);
        final ArrayList<PublishedInfo> layers = new ArrayList<PublishedInfo>();
        expect(lg.getLayers()).andReturn(layers);
        replay(lg);
        final Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getLayerGroup("lg")).andReturn(lg);        
        replay(catalog);
        
        //tests
        final Catalog secureCatalog = new SecureCatalogImpl(catalog);
        final LayerGroupInfo layerGroup = secureCatalog.getLayerGroup("lg");
        
        assertTrue(layerGroup instanceof SecuredLayerGroupInfo);
        assertTrue(((SecuredLayerGroupInfo) layerGroup).unwrap(LayerGroupInfo.class) == lg);
    }
    
    @Test
    public void testLayerGroupSynchronised() throws Exception {
        //create mocks
        final LayerInfo layer1 = createNiceMock(LayerInfo.class);
        final LayerInfo layer2 = createNiceMock(LayerInfo.class);
        
        final LayerGroupInfo lg = createNiceMock(LayerGroupInfo.class);       
        final ArrayList<PublishedInfo> layers = new ArrayList<PublishedInfo>();
        expect(lg.getLayers()).andReturn(layers).times(3);
        lg.setRootLayer(layer1);
        expectLastCall();
        replay(lg);
        
                
        //tests
        final ArrayList<PublishedInfo> securedLayers = new ArrayList<PublishedInfo>();
        final SecuredLayerGroupInfo securedLg = new SecuredLayerGroupInfo(
                lg, null, securedLayers);
        

        securedLg.getLayers().add(new SecuredLayerInfo(layer1, null));
        securedLg.getLayers().add(new SecuredLayerInfo(layer2, null));
        
        assertEquals(2, securedLg.getLayers().size());
        assertEquals(2, layers.size());
        assertTrue(layers.get(0) == layer1);
        assertTrue(layers.get(1) == layer2);
        
        securedLg.getLayers().remove(1);
        assertEquals(1, securedLg.getLayers().size());
        assertEquals(1, layers.size());
        
        securedLg.setRootLayer(new SecuredLayerInfo(layer2, null));
        //expect is test enough
        
    }

}
