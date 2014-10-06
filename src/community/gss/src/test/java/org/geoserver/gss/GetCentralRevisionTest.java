/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.geoserver.gss.GSSCore.*;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.VersioningDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetCentralRevisionTest extends GSSTestSupport {
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        VersioningDataStore synch = (VersioningDataStore) getCatalog().getDataStoreByName("synch").getDataStore(null);
        FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) synch.getFeatureSource(SYNCH_HISTORY);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(fs.getSchema());
        // three synchs occurred on this layer
        fs.addFeatures(DataUtilities.collection(fb.buildFeature(null, new Object[] {"roads", 150, 160})));
        fs.addFeatures(DataUtilities.collection(fb.buildFeature(null, new Object[] {"roads", 182, 210})));
        fs.addFeatures(DataUtilities.collection(fb.buildFeature(null, new Object[] {"roads", 193, 340})));
        // and none on restricted
    }
    
    /**
     * Tests a layer that is not there
     * @throws Exception
     */
    public void testCentralRevisionUnknownLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "service=gss&request=GetCentralRevision&typeName=sf:missing");
        validate(response);
        Document doc = dom(response);
        // print(doc);
        checkOws10Exception(doc, "InvalidParameterValue");
    }
    
    /**
     * Tests a layer that is there, but not in the synch tables
     * @throws Exception
     */
    public void testCentralRevisionLocalLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "service=gss&request=GetCentralRevision&typeName=sf:archsites");
        validate(response);
        Document doc = dom(response);
        // print(doc);
        checkOws10Exception(doc, "InvalidParameterValue");
    }
    

    public void testCentralRevisionRestricted() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "service=gss&request=GetCentralRevision&typeName=sf:restricted");
        validate(response);
        Document doc = dom(response);
        // print(doc);
        
        // no synchronization, give me everything back
        assertXpathEvaluatesTo("-1", "//gss:CentralRevisions/gss:LayerRevision/@centralRevision", doc);
    }
    
    public void testCentralRevisionRoads() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "service=gss&request=GetCentralRevision&typeName=sf:roads");
        validate(response);
        Document doc = dom(response);
        // print(doc);
        
        assertXpathEvaluatesTo("340", "//gss:CentralRevisions/gss:LayerRevision/@centralRevision", doc);
    }
    
}
