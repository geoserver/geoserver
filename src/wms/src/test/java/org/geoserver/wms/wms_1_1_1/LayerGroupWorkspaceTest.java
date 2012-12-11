/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class LayerGroupWorkspaceTest extends WMSTestSupport {

    LayerGroupInfo global, sf, cite;

    @Before
    public void prepare() throws Exception {
        Catalog cat = getCatalog();

        global = createLayerGroup(cat, "base", "base default",
            layer(cat, MockData.LAKES), layer(cat, MockData.FORESTS));
        cat.add(global);

        sf = createLayerGroup(cat, "base", "sf base", layer(cat, MockData.PRIMITIVEGEOFEATURE), 
            layer(cat, MockData.AGGREGATEGEOFEATURE));
        sf.setWorkspace(cat.getWorkspaceByName("sf"));
        cat.add(sf);

        cite = createLayerGroup(cat, "base", "cite base", layer(cat, MockData.BRIDGES), 
            layer(cat, MockData.BUILDINGS));
        cite.setWorkspace(cat.getWorkspaceByName("cite"));
        cat.add(cite);
    }
    
    @After
    public void rollback() throws Exception {
        Catalog cat = getCatalog();
        cat.remove(cite);
        cat.remove(sf);
        cat.remove(global);
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    LayerGroupInfo createLayerGroup(Catalog cat, String name, String title, LayerInfo... layers) throws Exception {
        LayerGroupInfo group = cat.getFactory().createLayerGroup();
        group.setName(name);
        group.setTitle("title for layer group " + title);
        group.setAbstract("abstract for layer group " + title);
        group.getLayers().addAll(Arrays.asList(layers));
        new CatalogBuilder(cat).calculateLayerGroupBounds(group);
        return group;
    }

    @Test 
    public void testAddLayerGroup() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo lg = createLayerGroup(cat, "base", "base", layer(cat, MockData.LOCKS));
        try {
            cat.add(lg);
            fail();
        }
        catch(Exception e) {}
    }

    @Test 
    public void testGlobalCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        assertXpathExists("//Layer/Name[text() = 'base']", dom);
        assertBounds(global, "base", dom);
        
        assertXpathExists("//Layer/Name[text() = 'sf:base']", dom);
        assertBounds(sf, "sf:base", dom);
        
        assertXpathExists("//Layer/Name[text() = 'cite:base']", dom);
        assertBounds(cite, "cite:base", dom);
    }

    @Test 
    public void testLayerGroupTitleInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        assertXpathExists("//Layer/Title[text() = 'title for layer group base default']", dom);
        assertXpathExists("//Layer/Title[text() = 'title for layer group sf base']", dom);
        assertXpathExists("//Layer/Title[text() = 'title for layer group cite base']", dom);
    }
    
    @Test 
    public void testLayerGroupAbstractInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        assertXpathExists("//Layer/Abstract[text() = 'abstract for layer group base default']", dom);
        assertXpathExists("//Layer/Abstract[text() = 'abstract for layer group sf base']", dom);
        assertXpathExists("//Layer/Abstract[text() = 'abstract for layer group cite base']", dom);
    }
    
    @Test 
    public void testWorkspaceCapabilities() throws Exception {
        Document dom = getAsDOM("sf/wms?request=getcapabilities&version=1.1.1");

        assertXpathExists("//Layer/Name[text() = 'base']", dom);
        assertXpathNotExists("//Layer/Name[text() = 'sf:base']", dom);
        assertBounds(sf, "base", dom);
    }

    @Test 
    public void testGlobalGetMap() throws Exception {
        Document dom = getAsDOM("wms/reflect?layers=base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'cite:Lakes,cite:Forests']", dom);

        dom = getAsDOM("wms/reflect?layers=sf:base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);

        dom = getAsDOM("wms/reflect?layers=cite:base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'cite:Bridges,cite:Buildings']", dom);
    }

    @Test
    public void testWorkspaceGetMap() throws Exception {
        Document dom = getAsDOM("sf/wms?request=reflect&layers=base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);

        dom = getAsDOM("cite/wms?request=reflect&layers=base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'cite:Bridges,cite:Buildings']", dom);

        dom = getAsDOM("sf/wms?request=reflect&layers=cite:base&format=kml");
        assertXpathExists("/kml:kml/kml:Document/kml:name[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);
    }

    void assertBounds(LayerGroupInfo lg, String name, Document dom) throws Exception {
        assertXpathEvaluatesTo(String.valueOf(Math.round(lg.getBounds().getMinX())), 
            "round(//Layer[Name/text() = '"+name+"']/BoundingBox/@minx)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(lg.getBounds().getMaxX())), 
                "round(//Layer[Name/text() = '"+name+"']/BoundingBox/@maxx)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(lg.getBounds().getMinY())), 
                "round(//Layer[Name/text() = '"+name+"']/BoundingBox/@miny)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(lg.getBounds().getMaxY())), 
                "round(//Layer[Name/text() = '"+name+"']/BoundingBox/@maxy)", dom);
    }
}
