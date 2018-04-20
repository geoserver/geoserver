/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

import org.springframework.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.io.WKTReader;

import au.com.bytecode.opencsv.CSVReader;

public class GetFeatureJoinTest extends WFS20TestSupport {
    
    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        
        //setup an H2 datastore for the purpose of doing joins
        //run all the tests against a store that can do native paging (h2) and one that 
        // can't (property)
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);
        
        Map params = ds.getConnectionParameters(); 
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath()+"/foo");
        cat.add(ds);
        
        FeatureSource fs1 = getFeatureSource(SystemTestData.FORESTS);
        FeatureSource fs2 = getFeatureSource(SystemTestData.LAKES);
        FeatureSource fs3 = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);
        
        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        
        tb.init((SimpleFeatureType) fs1.getSchema());
        //tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());
        
        tb.init((SimpleFeatureType) fs2.getSchema());
        //tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());
        
        tb.init((SimpleFeatureType) fs3.getSchema());
        tb.remove("surfaceProperty");
        tb.remove("curveProperty");
        tb.remove("uriProperty");
        store.createSchema(tb.buildFeatureType());
        
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        
        FeatureStore fs = (FeatureStore) store.getFeatureSource("Forests");
        fs.addFeatures(fs1.getFeatures());
        addFeature(fs,"MULTIPOLYGON (((0.008151604330777 -0.0023208963631571, 0.0086527358638763 -0.0012374917185382, 0.0097553137885805 -0.0004505798694767, 0.0156132468328575 0.001226912691216, 0.0164282119026783 0.0012863836826631, 0.0171241513076058 0.0011195104764988, 0.0181763809803841 0.0003258121477801, 0.018663180519973 -0.0007914339515293, 0.0187 -0.0054, 0.0185427596344991 -0.0062643098258021, 0.0178950534559435 -0.0072336706251426, 0.0166538015456463 -0.0078538015456464, 0.0160336706251426 -0.0090950534559435, 0.0150643098258021 -0.0097427596344991, 0.0142 -0.0099, 0.0086 -0.0099, 0.0077356901741979 -0.0097427596344991, 0.0067663293748574 -0.0090950534559435, 0.0062572403655009 -0.0082643098258021, 0.0061 -0.0074, 0.0061055767515099 -0.0046945371967831, 0.0062818025956546 -0.0038730531083409, 0.0066527358638763 -0.0032374917185382, 0.0072813143786463 -0.0026800146279973, 0.008151604330777 -0.0023208963631571)))", 
            "110", "Foo Forest");
        addFeature(fs, "MULTIPOLYGON (((-0.0023852705061082 -0.005664537521815, -0.0026781637249217 -0.0063716443030016, -0.0033852705061082 -0.006664537521815, -0.0040923772872948 -0.0063716443030016, -0.0043852705061082 -0.005664537521815, -0.0040923772872947 -0.0049574307406285, -0.0033852705061082 -0.004664537521815, -0.0026781637249217 -0.0049574307406285, -0.0023852705061082 -0.005664537521815)))",
            "111", "Bar Forest");
        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);
        
        fs = (FeatureStore) store.getFeatureSource("Lakes");
        fs.addFeatures(fs2.getFeatures());
        addFeature(fs, "POLYGON ((0.0049784771992108 -0.0035817570010558, 0.0046394552911414 -0.0030781256232061, 0.0046513167019495 -0.0024837722339832, 0.0051238379318686 -0.0011179833712748, 0.0057730295670053 -0.0006191988155468, 0.0065631962428717 -0.0022312008226987, 0.0065546368796182 -0.0027977724434409, 0.0060815583363558 -0.0033764140395305, 0.0049784771992108 -0.0035817570010558))",
            "102", "Red Lake"); 
        addFeature(fs, "POLYGON ((0.0057191452206184 -0.0077928768384869, 0.0051345315543621 -0.0076850644756826, 0.0046394552911414 -0.0070781256232061, 0.0046513167019495 -0.0064837722339832, 0.0051238379318686 -0.0051179833712748, 0.0054994549090862 -0.0047342895334108, 0.0070636636030018 -0.0041582580884052, 0.0078667798947931 -0.0042156264760765, 0.0082944271909999 -0.0046527864045, 0.0089944271909999 -0.0060527864045, 0.0090938616646936 -0.0066106299753791, 0.0089805097233498 -0.0069740280868118, 0.0084059445811345 -0.007452049322921, 0.0057191452206184 -0.0077928768384869))",
            "103", "Green Lake");
        addFeature(fs, "POLYGON ((0.0007938800267961 -0.0056175636045986, 0.0011573084862925 -0.0051229419555271, 0.0017412204815544 -0.0049337922722299, 0.0023617041415903 -0.0050976945961703, 0.0029728059060882 -0.0055503031602247, 0.0034289873678372 -0.0063805324543033, 0.0035801692478343 -0.0074485059825999, 0.0034823709081135 -0.008013559804892, 0.0032473247836666 -0.008318888359415, 0.0029142821960289 -0.0085126790755088, 0.0023413406005588 -0.0085369332611115, 0.0011766812981572 -0.0078593563537122, 0.0006397573417165 -0.0067622385244755, 0.0007938800267961 -0.0056175636045986))",
            "110", "Black Lake");
        ft = cb.buildFeatureType(fs);
        cat.add(ft);
        
        fs = (FeatureStore) store.getFeatureSource("PrimitiveGeoFeature");
        fs.addFeatures(fs3.getFeatures());
        ft = cb.buildFeatureType(fs);
        cat.add(ft);
        
        tb = new SimpleFeatureTypeBuilder();
        tb.setName("TimeFeature");
        tb.add("name", String.class);
        tb.add("dateTime", Date.class);
        
        SimpleFeatureType timeFeatureType = tb.buildFeatureType(); 
        store.createSchema(timeFeatureType);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(timeFeatureType);
        fb.add("one");
        fb.add(dateFormat.parseObject("2006-04-04 22:00:00"));
        features.add(fb.buildFeature(null));
        
        fb.add("two");
        fb.add(dateFormat.parseObject("2006-05-05 20:00:00"));
        features.add(fb.buildFeature(null));
        
        fb.add("three");
        fb.add(dateFormat.parseObject("2006-06-28 18:00:00"));
        features.add(fb.buildFeature(null));
        
        fs = (FeatureStore) store.getFeatureSource("TimeFeature");
        fs.addFeatures(features);
        ft = cb.buildFeatureType(fs);
        cat.add(ft);

        // add three joinable types with same code, but different type names
        SimpleFeatureType ft1 = DataUtilities.createType(SystemTestData.CITE_URI, "t1",
                "g1:Point:srid=4326,code1:int,name1:String");
        store.createSchema(ft1);
        fs = (FeatureStore) store.getFeatureSource("t1");
        addFeature(fs, "POINT(1 1)", Integer.valueOf(1), "First");
        ft = cb.buildFeatureType(fs);
        cat.add(ft);

        SimpleFeatureType ft2 = DataUtilities.createType(SystemTestData.CITE_URI, "t2",
                "g2:Point:srid=4326,code2:int,name2:String");
        store.createSchema(ft2);
        fs = (FeatureStore) store.getFeatureSource("t2");
        addFeature(fs, "POINT(2 2)", Integer.valueOf(1), "Second");
        ft = cb.buildFeatureType(fs);
        cat.add(ft);

        SimpleFeatureType ft3 = DataUtilities.createType(SystemTestData.CITE_URI, "t3",
                "g3:Point:srid=4326,code3:int,name3:String");
        store.createSchema(ft3);
        fs = (FeatureStore) store.getFeatureSource("t3");
        addFeature(fs, "POINT(3 3)", Integer.valueOf(1), "Third");
        ft = cb.buildFeatureType(fs);
        cat.add(ft);
    }

    void addFeature(FeatureStore store, String wkt, Object... atts) throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder((SimpleFeatureType) store.getSchema());
        b.add(new WKTReader().read(wkt));
        for (Object att : atts) {
            b.add(att);
        }
        
        DefaultFeatureCollection features = new DefaultFeatureCollection(null,null); 
        features.add(b.buildFeature(null));
        store.addFeatures(features);
    }
    
    @Test
    public void testSpatialJoinPOST() throws Exception {
        String xml = 
         "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
           " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
            "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>" +
             "<fes:Filter> " + 
               "<fes:Intersects> " + 
                "<fes:ValueReference>a/the_geom</fes:ValueReference> " + 
                "<fes:ValueReference>b/the_geom</fes:ValueReference>" + 
               "</fes:Intersects> " + 
             "</fes:Filter> " + 
            "</wfs:Query>" + 
          "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Forests/gs:NAME[text() = 'Green Forest']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Lakes/gs:NAME[text() = 'Blue Lake']", dom);

        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Lakes/gs:NAME[text() = 'Green Lake']", dom);
    }

    @Test
    public void testSpatialJoinNoAliasesCustomPrefixes() throws Exception {
        String xml =
                "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
                        " xmlns:gs='" + SystemTestData.DEFAULT_URI + "'" +
                        " xmlns:ns123='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" +
                        "<wfs:Query typeNames='ns123:Forests ns123:Lakes'>" +
                        "<fes:Filter> " +
                        "<fes:Intersects> " +
                        "<fes:ValueReference>ns123:Forests/the_geom</fes:ValueReference> " +
                        "<fes:ValueReference>ns123:Lakes/the_geom</fes:ValueReference>" +
                        "</fes:Intersects> " +
                        "</fes:Filter> " +
                        "</wfs:Query>" +
                        "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Forests/gs:NAME[text() = 'Green Forest']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Lakes/gs:NAME[text() = 'Blue Lake']", dom);

        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Lakes/gs:NAME[text() = 'Green Lake']", dom);
    }
    
    @Test
    public void testSpatialJoinGET() throws Exception {
        Document dom = 
            getAsDOM("wfs?service=WFS&version=2.0.0&request=getFeature&typenames=gs:Forests,gs:Lakes&aliases=a,b&filter=%3CFilter%3E%3CIntersects%3E%3CValueReference%3Ea%2Fthe_geom%3C%2FValueReference%3E%3CValueReference%3Eb%2Fthe_geom%3C%2FValueReference%3E%3C%2FIntersects%3E%3C%2FFilter%3E");
        
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Forests/gs:NAME[text() = 'Green Forest']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[position() = 1]/wfs:member/gs:Lakes/gs:NAME[text() = 'Blue Lake']", dom);

        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
        XMLAssert.assertXpathExists("wfs:FeatureCollection/wfs:member[position()=2]/wfs:Tuple//gs:Lakes/gs:NAME[text() = 'Green Lake']", dom);
    }
    
    @Test
    public void testSpatialJoinPOSTWithPrimaryFilter() throws Exception {
        String xml = 
         "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
           " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
            "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>" +
             "<fes:Filter> " +
               "<fes:And>" +
                 "<fes:Intersects> " + 
                   "<fes:ValueReference>a/the_geom</fes:ValueReference> " + 
                   "<fes:ValueReference>b/the_geom</fes:ValueReference>" + 
                 "</fes:Intersects> " +
                 "<PropertyIsEqualTo>" +
                   "<ValueReference>a/FID</ValueReference>" +
                   "<Literal>110</Literal>" + 
                 "</PropertyIsEqualTo>" + 
               "</fes:And>" + 
             "</fes:Filter> " + 
            "</wfs:Query>" + 
          "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Green Lake']", dom);
    }
    
    @Test
    public void testSpatialJoinPOSTWithSecondaryFilter() throws Exception {
        String xml = 
         "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
           " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
            "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>" +
             "<fes:Filter> " +
               "<fes:And>" +
                 "<fes:Intersects> " + 
                   "<fes:ValueReference>a/the_geom</fes:ValueReference> " + 
                   "<fes:ValueReference>b/the_geom</fes:ValueReference>" + 
                 "</fes:Intersects> " +
                 "<PropertyIsEqualTo>" +
                   "<ValueReference>b/FID</ValueReference>" +
                   "<Literal>101</Literal>" + 
                 "</PropertyIsEqualTo>" + 
               "</fes:And>" + 
             "</fes:Filter> " + 
            "</wfs:Query>" + 
          "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Green Forest']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Blue Lake']", dom);
    }
    
    @Test
    public void testSpatialJoinWithBothFilters() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>" +
                "<fes:Filter> " +
                  "<fes:And>" +
                    "<fes:Disjoint> " + 
                      "<fes:ValueReference>a/the_geom</fes:ValueReference> " + 
                      "<fes:ValueReference>b/the_geom</fes:ValueReference>" + 
                    "</fes:Disjoint> " +
                    "<fes:And>" + 
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>a/NAME</ValueReference>" +
                          "<Literal>Bar Forest</Literal>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsGreaterThan>" +
                          "<ValueReference>b/FID</ValueReference>" +
                          "<Literal>102</Literal>" + 
                        "</PropertyIsGreaterThan>" +
                    "</fes:And>" + 
                  "</fes:And>" + 
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

           Document dom = postAsDOM("wfs", xml);

           XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);
           XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Bar Forest'])", dom);
           
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Green Lake']", dom);
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testStandardJoin() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>" +
                "<fes:Filter> " +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>a/FID</ValueReference>" +
                      "<ValueReference>b/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

           Document dom = postAsDOM("wfs", xml);
           XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
           
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testJoinAliasConflictProperty() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a NAME'>" +
                "<fes:Filter> " +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>a/FID</ValueReference>" +
                      "<ValueReference>NAME/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

           Document dom = postAsDOM("wfs", xml);
        // print(dom);
           XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
           
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }

    @Test
    public void testStandardJoinThreeWays() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes gs:Lakes' aliases='a b c'>" +
                "<fes:Filter> " +
                    "<And>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>a/FID</ValueReference>" +
                          "<ValueReference>b/FID</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>b/FID</ValueReference>" +
                          "<ValueReference>c/FID</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                    "</And>" + 
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[1]/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[2]/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[3]/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testStandardJoinThreeWaysLocalFilters() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:t1 gs:t2 gs:t3' aliases='a b c'>" +
                "<fes:Filter> " +
                    "<And>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>a/name1</ValueReference>" +
                          "<Literal>First</Literal>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>a/code1</ValueReference>" +
                          "<ValueReference>b/code2</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>b/name2</ValueReference>" +
                          "<Literal>Second</Literal>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>c/name3</ValueReference>" +
                          "<Literal>Third</Literal>" + 
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>b/code2</ValueReference>" +
                          "<ValueReference>c/code3</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                    "</And>" + 
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);

        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[1]/gs:t2[gs:name2 = 'Second']", dom);
        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[2]/gs:t1[gs:name1 = 'First']", dom);
        XMLAssert.assertXpathExists(
                "//wfs:Tuple/wfs:member[3]/gs:t3[gs:name3 = 'Third']", dom);
    }

    @Test
    public void testStandardJoin2() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='c d'>" +
                "<fes:Filter> " +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>c/FID</ValueReference>" +
                      "<ValueReference>d/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

       Document dom = postAsDOM("wfs", xml);
       XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
       
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testStandardJoinMainTypeRenamed() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo forestsInfo = catalog.getFeatureTypeByName("gs:Forests");
        String oldName = forestsInfo.getName(); 
        forestsInfo.setName("ForestsRenamed");
        try {
            catalog.save(forestsInfo);
            String xml = 
                "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
                  " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
                   "<wfs:Query typeNames='gs:ForestsRenamed gs:Lakes' aliases='c d'>" +
                    "<fes:Filter> " +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>c/FID</ValueReference>" +
                          "<ValueReference>d/FID</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                    "</fes:Filter> " + 
                   "</wfs:Query>" + 
                 "</wfs:GetFeature>";
    
           Document dom = postAsDOM("wfs", xml);
           // print(dom);
           XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
           
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:ForestsRenamed/gs:NAME[text() = 'Foo Forest']", dom);
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
        } finally {
            forestsInfo.setName(oldName);
            catalog.save(forestsInfo);
        }
    }
    
    /**
     * See [GEOS-8032] WFS 2.0 feature joins fails when the joined feature type is renamed
     * @throws Exception
     */
    @Test
    @Ignore
    public void testStandardJoinSecondaryTypeRenamed() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo forestsInfo = catalog.getFeatureTypeByName("gs:Lakes");
        String oldName = forestsInfo.getName(); 
        forestsInfo.setName("LakesRenamed");
        try {
            catalog.save(forestsInfo);
            String xml = 
                "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
                  " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
                   "<wfs:Query typeNames='gs:Forests gs:LakesRenamed' aliases='c d'>" +
                    "<fes:Filter> " +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>c/FID</ValueReference>" +
                          "<ValueReference>d/FID</ValueReference>" + 
                        "</PropertyIsEqualTo>" +
                    "</fes:Filter> " + 
                   "</wfs:Query>" + 
                 "</wfs:GetFeature>";
    
           Document dom = postAsDOM("wfs", xml);
           print(dom);
           XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
           
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Renamed/gs:NAME[text() = 'Foo Forest']", dom);
           XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:LakesRenamed/gs:NAME[text() = 'Black Lake']", dom);
        } finally {
            forestsInfo.setName(oldName);
            catalog.save(forestsInfo);
        }
    }
    
    @Test
    public void testStandardJoinNoAliases() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes'>" +
                "<fes:Filter> " +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>gs:Forests/FID</ValueReference>" +
                      "<ValueReference>gs:Lakes/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

       Document dom = postAsDOM("wfs", xml);
       XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
       
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testSelfJoin() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Forests' aliases='a b'>" +
                "<fes:Filter> " +
                    "<Disjoint>" +
                      "<ValueReference>a/the_geom</ValueReference>" +
                      "<ValueReference>b/the_geom</ValueReference>" + 
                    "</Disjoint>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

           Document dom = postAsDOM("wfs", xml);
           
           XMLAssert.assertXpathEvaluatesTo("6", "count(//wfs:Tuple)", dom);
    }

    @Test
    public void testSelfJoinLocalNamespaces() throws Exception {
        String xml =
                "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
                        " xmlns:ns42='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" +
                        "<wfs:Query typeNames='ns42:Forests ns42:Forests' aliases='a b'>" +
                        "<fes:Filter> " +
                        "<Disjoint>" +
                        "<ValueReference>a/ns42:the_geom</ValueReference>" +
                        "<ValueReference>b/ns42:the_geom</ValueReference>" +
                        "</Disjoint>" +
                        "</fes:Filter> " +
                        "</wfs:Query>" +
                        "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        XMLAssert.assertXpathEvaluatesTo("6", "count(//wfs:Tuple)", dom);
    }
    
    @Test
    public void testTemporalJoin() throws Exception {
        String xml = 
             "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
                  " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
                   "<wfs:Query typeNames='gs:PrimitiveGeoFeature gs:TimeFeature' aliases='a b'>" +
                    "<fes:Filter> " +
                      "<And>" +
                        "<After>" +
                          "<ValueReference>a/dateTimeProperty</ValueReference>" +
                          "<ValueReference>b/dateTime</ValueReference>" + 
                        "</After>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>a/name</ValueReference>" +
                          "<Literal>name-f008</Literal>" + 
                        "</PropertyIsEqualTo>" +
                      "</And>" +
                    "</fes:Filter> " + 
                   "</wfs:Query>" + 
                 "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);
    }
    
    @Test
    public void testStandardJoinLocalFilterNot() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='c d'>" +
                "<fes:Filter> " +
                  "<And>" +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>c/FID</ValueReference>" +
                      "<ValueReference>d/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                    "<Not>" +
                      "<PropertyIsEqualTo>" +
                         "<ValueReference>d/NAME</ValueReference>" +
                         "<Literal>foo</Literal>" +
                      "</PropertyIsEqualTo>" + 
                    "</Not>"+
                  "</And>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

       Document dom = postAsDOM("wfs", xml);
       XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
       
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testStandardJoinLocalFilterOr() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='c d'>" +
                "<fes:Filter> " +
                  "<And>" +
                    "<PropertyIsEqualTo>" +
                      "<ValueReference>c/FID</ValueReference>" +
                      "<ValueReference>d/FID</ValueReference>" + 
                    "</PropertyIsEqualTo>" +
                    "<Or>" +
                      "<PropertyIsEqualTo>" +
                         "<ValueReference>d/NAME</ValueReference>" +
                         "<Literal>foo</Literal>" +
                      "</PropertyIsEqualTo>" +
                      "<PropertyIsEqualTo>" +
                        "<ValueReference>d/NAME</ValueReference>" +
                        "<Literal>Black Lake</Literal>" +
                     "</PropertyIsEqualTo>" +
                    "</Or>"+
                  "</And>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

       Document dom = postAsDOM("wfs", xml);
       XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:Tuple)", dom);
       
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Forests/gs:NAME[text() = 'Foo Forest']", dom);
       XMLAssert.assertXpathExists("//wfs:Tuple/wfs:member/gs:Lakes/gs:NAME[text() = 'Black Lake']", dom);
    }
    
    @Test
    public void testOredJoinCondition() throws Exception {
        String xml = 
            "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='" + FES.NAMESPACE + "'" +
              " xmlns:gs='" + SystemTestData.DEFAULT_URI + "' version='2.0.0'>" + 
               "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='c d'>" +
                "<fes:Filter> " +
                    "<Or>" +
                      "<PropertyIsEqualTo>" +
                        "<ValueReference>c/FID</ValueReference>" +
                        "<ValueReference>d/FID</ValueReference>" + 
                      "</PropertyIsEqualTo>" +
                      "<And>" + 
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>c/NAME</ValueReference>" +
                          "<Literal>Bar Forest</Literal>" +
                        "</PropertyIsEqualTo>" +
                        "<PropertyIsEqualTo>" +
                          "<ValueReference>d/NAME</ValueReference>" +
                          "<Literal>Green Lake</Literal>" +
                        "</PropertyIsEqualTo>" +
                     "</And>" +
                   "</Or>" +
                "</fes:Filter> " + 
               "</wfs:Query>" + 
             "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:Tuple)", dom);
       
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member/gs:Forests/gs:NAME = 'Foo Forest' "
                + "and wfs:member/gs:Lakes/gs:NAME = 'Black Lake']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member/gs:Forests/gs:NAME = 'Bar Forest' "
                + "and wfs:member/gs:Lakes/gs:NAME = 'Green Lake']", dom);
    }
    
    @Test
    public void testStandardJoinCSV() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>"
                + "<fes:Filter> " + "<PropertyIsEqualTo>"
                + "<ValueReference>a/FID</ValueReference>"
                + "<ValueReference>b/FID</ValueReference>" + "</PropertyIsEqualTo>"
                + "</fes:Filter> " + "</wfs:Query>" + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");
    
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=Forests.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs1 = getFeatureSource(MockData.FORESTS);
        FeatureSource fs2 = getFeatureSource(MockData.LAKES);
    
        for (String[] line : lines) {
            // check each line has the expected number of elements (num of att1 +
            // num of att2+1 for the id)
            assertEquals(
                    fs1.getSchema().getDescriptors().size()
                            + fs2.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    @Test
    public void testJoinAliasConflictingPropertyCSV() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a NAME'>"
                + "<fes:Filter> " + "<PropertyIsEqualTo>"
                + "<ValueReference>a/FID</ValueReference>"
                + "<ValueReference>NAME/FID</ValueReference>"
                + "</PropertyIsEqualTo>" + "</fes:Filter> " + "</wfs:Query>"
                + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");
    
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=Forests.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs1 = getFeatureSource(MockData.FORESTS);
        FeatureSource fs2 = getFeatureSource(MockData.LAKES);
    
        for (String[] line : lines) {
            // check each line has the expected number of elements (num of att1 +
            // num of att2+1 for the id)
            assertEquals(
                    fs1.getSchema().getDescriptors().size()
                            + fs2.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    @Test
    public void testStandardJoinNoAliasesCSV() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:Forests gs:Lakes'>" + "<fes:Filter> "
                + "<PropertyIsEqualTo>"
                + "<ValueReference>Forests/FID</ValueReference>"
                + "<ValueReference>Lakes/FID</ValueReference>"
                + "</PropertyIsEqualTo>" + "</fes:Filter> " + "</wfs:Query>"
                + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");
    
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=Forests.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs1 = getFeatureSource(MockData.FORESTS);
        FeatureSource fs2 = getFeatureSource(MockData.LAKES);
    
        for (String[] line : lines) {
            assertEquals(
                    fs1.getSchema().getDescriptors().size()
                            + fs2.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    @Test
    public void testSelfJoinCSV() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:Forests Forests' aliases='a b'>"
                + "<fes:Filter> " + "<Disjoint>"
                + "<ValueReference>a/the_geom</ValueReference>"
                + "<ValueReference>b/the_geom</ValueReference>" + "</Disjoint>"
                + "</fes:Filter> " + "</wfs:Query>" + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");

        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=Forests.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs = getFeatureSource(MockData.FORESTS);
    
        for (String[] line : lines) {
            assertEquals(2 * fs.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    @Test
    public void testSpatialJoinPOST_CSV() throws Exception {
        String xml = "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:Forests gs:Lakes' aliases='a b'>"
                + "<fes:Filter> " + "<fes:Intersects> "
                + "<fes:ValueReference>a/the_geom</fes:ValueReference> "
                + "<fes:ValueReference>b/the_geom</fes:ValueReference>"
                + "</fes:Intersects> " + "</fes:Filter> " + "</wfs:Query>"
                + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");
    
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=Forests.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs1 = getFeatureSource(MockData.FORESTS);
        FeatureSource fs2 = getFeatureSource(MockData.LAKES);
    
        for (String[] line : lines) {
            assertEquals(
                    fs1.getSchema().getDescriptors().size()
                            + fs2.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    @Test
    public void testStandardJoinThreeWaysLocalFiltersCSV() throws Exception {
        String xml = "<wfs:GetFeature xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:fes='"
                + FES.NAMESPACE + "'" + " xmlns:gs='" + SystemTestData.DEFAULT_URI
                + "' outputFormat='csv' version='2.0.0'>"
                + "<wfs:Query typeNames='gs:t1 gs:t2 gs:t3' aliases='a b c'>"
                + "<fes:Filter> " + "<And>" + "<PropertyIsEqualTo>"
                + "<ValueReference>a/name1</ValueReference>"
                + "<Literal>First</Literal>" + "</PropertyIsEqualTo>"
                + "<PropertyIsEqualTo>" + "<ValueReference>a/code1</ValueReference>"
                + "<ValueReference>b/code2</ValueReference>"
                + "</PropertyIsEqualTo>" + "<PropertyIsEqualTo>"
                + "<ValueReference>b/name2</ValueReference>"
                + "<Literal>Second</Literal>" + "</PropertyIsEqualTo>"
                + "<PropertyIsEqualTo>" + "<ValueReference>c/name3</ValueReference>"
                + "<Literal>Third</Literal>" + "</PropertyIsEqualTo>"
                + "<PropertyIsEqualTo>" + "<ValueReference>b/code2</ValueReference>"
                + "<ValueReference>c/code3</ValueReference>"
                + "</PropertyIsEqualTo>" + "</And>" + "</fes:Filter> "
                + "</wfs:Query>" + "</wfs:GetFeature>";
    
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/xml", "UTF-8");
    
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
    
        // check the charset encoding
        assertEquals("UTF-8", resp.getCharacterEncoding());
    
        // check the content disposition
        assertEquals("attachment; filename=t1.csv",
                resp.getHeader("Content-Disposition"));
    
        // read the response back with a parser that can handle escaping, newlines
        // and what not
        List<String[]> lines = readLines(resp.getContentAsString());
    
        FeatureSource fs1 = getFeatureSource(new QName("t1"));
        FeatureSource fs2 = getFeatureSource(new QName("t2"));
        FeatureSource fs3 = getFeatureSource(new QName("t3"));
    
        for (String[] line : lines) {
            assertEquals(
                    fs1.getSchema().getDescriptors().size()
                            + fs2.getSchema().getDescriptors().size()
                            + fs3.getSchema().getDescriptors().size() + 1,
                    line.length);
        }
    }
    
    /**
     * Convenience to read the csv content and
     * 
     * @param csvContent
     *
     * @throws IOException
     */
    private List<String[]> readLines(String csvContent) throws IOException {
        // System.out.println(csvContent);
        CSVReader reader = new CSVReader(new StringReader(csvContent));
    
        List<String[]> result = new ArrayList<String[]>();
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            result.add(nextLine);
        }
        return result;
    }
    
    @Test
    public void testSelfJoinNoAliases() throws Exception {
        String xml = "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" count=\"10\" " +
                "service=\"WFS\" startIndex=\"0\" version=\"2.0.0\"> <wfs:Query " +
                "xmlns:ns76=\"" + SystemTestData.DEFAULT_URI + "\" " +
                "typeNames=\"ns76:PrimitiveGeoFeature ns76:PrimitiveGeoFeature\"> <Filter " +
                "xmlns=\"http://www.opengis.net/fes/2.0\"> <PropertyIsEqualTo> " +
                "<ValueReference>ns76:PrimitiveGeoFeature/ns76:booleanProperty</ValueReference> " +
                "<ValueReference>ns76:PrimitiveGeoFeature/ns76:booleanProperty</ValueReference> " +
                "</PropertyIsEqualTo> </Filter> </wfs:Query> </wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        
        // many tuples match:
        // f001 with itself, f003, f008
        // f003 with itself, f001, f008
        // f008 with itself, f001, f003
        // f002 with itself
        XMLAssert.assertXpathEvaluatesTo("10", "count(//wfs:Tuple)", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f001' and wfs:member[2]//gml:description = 'description-f001']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f001' and wfs:member[2]//gml:description = 'description-f003']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f001' and wfs:member[2]//gml:description = 'description-f008']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f003' and wfs:member[2]//gml:description = 'description-f001']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f003' and wfs:member[2]//gml:description = 'description-f003']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f003' and wfs:member[2]//gml:description = 'description-f008']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f008' and wfs:member[2]//gml:description = 'description-f001']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f008' and wfs:member[2]//gml:description = 'description-f003']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f008' and wfs:member[2]//gml:description = 'description-f008']", dom);
        XMLAssert.assertXpathExists("//wfs:Tuple[wfs:member[1]//gml:description = 'description-f002' and wfs:member[2]//gml:description = 'description-f002']", dom);
    }
    
}
