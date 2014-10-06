/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import org.geotools.data.Query;
import org.geotools.data.VersioningFeatureStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Id;
import org.w3c.dom.Document;

import static java.util.Collections.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetDiffTest extends GSSTestSupport {

    /**
     * Tests that we asked for a out of order GetDiff: there has not been any PostDiff before it
     */
    public void testGetDiffOutOfOrder() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(true), loadTextResource("GetDiffInitial.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        checkOws10Exception(dom, "InvalidParameterValue", "fromVersion");
    }
    
    /**
     * Checks we properly report back the layer is not known to the service
     * @throws Exception
     */
    public void testUnknownLayer() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("GetDiffUnknown.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        checkOws10Exception(dom, "InvalidParameterValue", "typeName");
    }
    
    /**
     * Checks we properly report back that revision is not known
     * @throws Exception
     */
    public void testFutureRevision() throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("GetDiffFuture.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        checkOws10Exception(dom, "InvalidParameterValue", "fromVersion");
    }
    
    public void testNoLocalChanges() throws Exception {
        // run a postdiff that will merge some central changes. There are no local ones
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        
        // now run a GetDiff that should get a positive, empty response
        response = postAsServletResponse(root(true), loadTextResource("GetDiffInitial.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        
        // we should have got back a GetDiffResponse with an empty transation in it
        assertXpathEvaluatesTo("-1", "/gss:GetDiffResponse/@fromVersion", dom);
        assertXpathEvaluatesTo("4", "/gss:GetDiffResponse/@toVersion", dom);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/@typeName", dom);
        assertXpathExists("/gss:GetDiffResponse/gss:Changes", dom);
        assertXpathEvaluatesTo("0", "count(/gss:GetDiffResponse/gss:Changes/*)", dom);
    }
    
    public void testCleanMerge() throws Exception {
        // grab the datastore 
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // make the same changes as in the post diff
        Id updateFilter = ff.id(singleton(ff.featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), -48, updateFilter);
        // remove the third feature
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5")));
        restricted.removeFeatures(removeFilter);
        assertEquals(3, restricted.getCount(Query.ALL));
        

        // get the response and do the basic checks
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);

        // check there are no conflicts
        assertEquals(0, gss.getActiveConflicts("restricted").size());
        
        // run GetDiff
        response = postAsServletResponse(root(true), loadTextResource("GetDiffInitial.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        
        // we should have got back an empty transaction
        // ... check the main element
        assertXpathEvaluatesTo("-1", "/gss:GetDiffResponse/@fromVersion", dom);
        assertXpathEvaluatesTo("6", "/gss:GetDiffResponse/@toVersion", dom);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/@typeName", dom);
        // check the transaction is empty
        assertXpathEvaluatesTo("0", "count(/gss:GetDiffResponse/gss:Changes/*)", dom);
    }
    
    public void testNoConflictingChanges() throws Exception {
        // grab the datastore so that we can make some changes that will not generate conflicts
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // modify the fourth feature, change its cat from 400 to 450
        Id updateFilter = ff.id(singleton(ff.featureId("restricted.1b99be2b-2480-4742-ad52-95c294efda3b")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), 450, updateFilter);
        // remove the third feature
        Id removeFilter = ff.id(singleton(ff.featureId("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a")));
        restricted.removeFeatures(removeFilter);
        assertEquals(3, restricted.getCount(Query.ALL));
        
        // execute the postDiff
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);

        // check there are no conflicts
        assertEquals(0, gss.getActiveConflicts("restricted").size());
        
        // run GetDiff
        response = postAsServletResponse(root(true), loadTextResource("GetDiffInitial.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        
        // check the document contents are the expected ones
        // ... check the main element
        assertXpathEvaluatesTo("-1", "/gss:GetDiffResponse/@fromVersion", dom);
        assertXpathEvaluatesTo("6", "/gss:GetDiffResponse/@toVersion", dom);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/@typeName", dom);
        // check the transaction has two elements
        assertXpathEvaluatesTo("2", "count(/gss:GetDiffResponse/gss:Changes/*)", dom);
        // check the update one
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/gss:Changes/wfs:Update/@typeName", dom);
        assertXpathEvaluatesTo("1", "count(/gss:GetDiffResponse/gss:Changes/wfs:Update/ogc:Filter/ogc:FeatureId)", dom);
        assertXpathEvaluatesTo("restricted.1b99be2b-2480-4742-ad52-95c294efda3b", "/gss:GetDiffResponse/gss:Changes/wfs:Update/ogc:Filter/ogc:FeatureId/@fid", dom);
        assertXpathEvaluatesTo("1", "count(/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property)", dom);
        assertXpathEvaluatesTo("cat", "/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property/wfs:Name", dom);
        assertXpathEvaluatesTo("450", "/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property/wfs:Value", dom);
        // check the delete one
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/gss:Changes/wfs:Delete/@typeName", dom);
        assertXpathEvaluatesTo("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a", "/gss:GetDiffResponse/gss:Changes/wfs:Delete/ogc:Filter/ogc:FeatureId/@fid", dom);
        assertXpathEvaluatesTo("restricted.c15e76ab-e44b-423e-8f85-f6d9927b878a", "/gss:GetDiffResponse/gss:Changes/wfs:Delete/ogc:Filter/ogc:FeatureId/@fid", dom);
    }
    
    /**
     * This time we mix some conflicting and non conflicting changes
     */
    public void testConflicts() throws Exception {
        VersioningFeatureStore restricted = (VersioningFeatureStore) synchStore.getFeatureSource("restricted");
        SimpleFeatureType schema = restricted.getSchema();
        // modify the fourth feature, change its cat from 400 to 450
        Id updateFilter = ff.id(singleton(ff.featureId("restricted.1b99be2b-2480-4742-ad52-95c294efda3b")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), 450, updateFilter);
        // a update that will generate a conflict
        updateFilter = ff.id(singleton(ff.featureId("restricted.d91fe390-bdc7-4b22-9316-2cd6c8737ef5")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), 347, updateFilter);
        // an update that will generate a clean merge
        updateFilter = ff.id(singleton(ff.featureId("restricted.be7cafea-d0b7-4257-9b9c-1ed3de3f7ef4")));
        restricted.modifyFeatures(schema.getDescriptor("cat"), -48, updateFilter);

        // execute the postDiff
        MockHttpServletResponse response = postAsServletResponse(root(true),
                loadTextResource("PostDiffInitial.xml"));
        checkPostDiffSuccessResponse(response);
        
        // check there is one conflict and one clean merge
        assertEquals(1, gss.getActiveConflicts("restricted").size());
        assertEquals(1, gss.getCleanMerges("restricted", 7).size());
        
        // run GetDiff
        response = postAsServletResponse(root(true), loadTextResource("GetDiffInitial.xml"));
        validate(response);
        Document dom = dom(response);
        // print(dom);
        
        // check the document contents are the expected ones
        // ... check the main element
        assertXpathEvaluatesTo("-1", "/gss:GetDiffResponse/@fromVersion", dom);
        assertXpathEvaluatesTo("7", "/gss:GetDiffResponse/@toVersion", dom);
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/@typeName", dom);
        // ... check we get only one change, the non conflicting one
        assertXpathEvaluatesTo("1", "count(/gss:GetDiffResponse/gss:Changes)", dom);
        // check the update one
        assertXpathEvaluatesTo("sf:restricted", "/gss:GetDiffResponse/gss:Changes/wfs:Update/@typeName", dom);
        assertXpathEvaluatesTo("1", "count(/gss:GetDiffResponse/gss:Changes/wfs:Update/ogc:Filter/ogc:FeatureId)", dom);
        assertXpathEvaluatesTo("restricted.1b99be2b-2480-4742-ad52-95c294efda3b", "/gss:GetDiffResponse/gss:Changes/wfs:Update/ogc:Filter/ogc:FeatureId/@fid", dom);
        assertXpathEvaluatesTo("1", "count(/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property)", dom);
        assertXpathEvaluatesTo("cat", "/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property/wfs:Name", dom);
        assertXpathEvaluatesTo("450", "/gss:GetDiffResponse/gss:Changes/wfs:Update/wfs:Property/wfs:Value", dom);

    }
    
    
}
