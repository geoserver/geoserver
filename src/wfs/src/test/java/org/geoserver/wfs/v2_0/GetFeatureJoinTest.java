/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

import com.vividsolutions.jts.io.WKTReader;

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
}
