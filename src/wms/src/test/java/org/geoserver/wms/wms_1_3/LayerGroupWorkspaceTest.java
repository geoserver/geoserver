/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static junit.framework.Assert.fail;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

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
import org.opengis.referencing.crs.GeographicCRS;
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
    protected void registerNamespaces(java.util.Map<String,String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
    };
    
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
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.3.0");
        
        assertXpathExists("//wms:Layer/wms:Name[text() = 'base']", dom);
        assertBounds(global, "base", dom);
        
        assertXpathExists("//wms:Layer/wms:Name[text() = 'sf:base']", dom);
        assertBounds(sf, "sf:base", dom);
        
        assertXpathExists("//wms:Layer/wms:Name[text() = 'cite:base']", dom);
        assertBounds(cite, "cite:base", dom);
    }

    @Test 
    public void testLayerGroupTitleInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.3.0");
        assertXpathExists("//wms:Layer/wms:Title[text() = 'title for layer group base default']", dom);
        assertXpathExists("//wms:Layer/wms:Title[text() = 'title for layer group sf base']", dom);
        assertXpathExists("//wms:Layer/wms:Title[text() = 'title for layer group cite base']", dom);
    }
    
    @Test 
    public void testLayerGroupAbstractInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.3.0", false);
        assertXpathExists("//wms:Layer/wms:Abstract[text() = 'abstract for layer group base default']", dom);
        assertXpathExists("//wms:Layer/wms:Abstract[text() = 'abstract for layer group sf base']", dom);
        assertXpathExists("//wms:Layer/wms:Abstract[text() = 'abstract for layer group cite base']", dom);
    }    
    
    @Test 
    public 
    void testWorkspaceCapabilities() throws Exception {
        Document dom = getAsDOM("sf/wms?request=getcapabilities&version=1.3.0");

        assertXpathExists("//wms:Layer/wms:Name[text() = 'base']", dom);
        assertXpathNotExists("//wms:Layer/wms:Name[text() = 'sf:base']", dom);
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
        double minx = lg.getBounds().getMinX();
        double miny = lg.getBounds().getMinY();
        double maxx = lg.getBounds().getMaxX();
        double maxy = lg.getBounds().getMaxY();

        if (lg.getBounds().getCoordinateReferenceSystem() instanceof GeographicCRS) {
            //flip
            double tmp = minx;
            minx = miny;
            miny = tmp;

            tmp = maxx;
            maxx = maxy;
            maxy = tmp;
        }
        assertXpathEvaluatesTo(String.valueOf(Math.round(minx)), 
            "round(//wms:Layer[wms:Name/text() = '"+name+"']/wms:BoundingBox/@minx)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(maxx)), 
                "round(//wms:Layer[wms:Name/text() = '"+name+"']/wms:BoundingBox/@maxx)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(miny)), 
                "round(//wms:Layer[wms:Name/text() = '"+name+"']/wms:BoundingBox/@miny)", dom);
        assertXpathEvaluatesTo(String.valueOf(Math.round(maxy)), 
                "round(//wms:Layer[wms:Name/text() = '"+name+"']/wms:BoundingBox/@maxy)", dom);
    }
}
