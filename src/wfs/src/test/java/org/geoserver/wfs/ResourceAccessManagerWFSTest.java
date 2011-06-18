package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.Document;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ResourceAccessManagerWFSTest extends WFSTestSupport {

    static final String INSERT_RESTRICTED_STREET = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\n"
            + "  xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:cite=\"http://www.opengis.net/cite\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd http://www.openplans.org/topp http://localhost:8080/geoserver/wfs/DescribeFeatureType?typename=topp:tasmania_roads\">\n"
            + "  <wfs:Insert>\n"
            + "    <cite:Buildings fid=\"Buildings.123\">\n"
            + "      <cite:the_geom>\n"
            + "        <gml:MultiPolygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
            + "          <gml:polygonMember>\n"
            + "            <gml:Polygon>\n"
            + "              <gml:outerBoundaryIs>\n"
            + "                <gml:LinearRing>\n"
            + "                  <gml:coordinates cs=\",\" decimal=\".\"\n"
            + "                    ts=\" \" xmlns:gml=\"http://www.opengis.net/gml\">0.0020,0.0008 0.0020,0.0010\n"
            + "                    0.0024,0.0010 0.0024,0.0008 0.0020,0.0008</gml:coordinates>\n"
            + "                </gml:LinearRing>\n"
            + "              </gml:outerBoundaryIs>\n"
            + "            </gml:Polygon>\n"
            + "          </gml:polygonMember>\n"
            + "        </gml:MultiPolygon>\n"
            + "      </cite:the_geom>\n"
            + "      <cite:FID>151</cite:FID>\n"
            + "      <cite:ADDRESS>123 Restricted Street</cite:ADDRESS>\n"
            + "    </cite:Buildings>\n" + "  </wfs:Insert>\n" + "</wfs:Transaction>";
    
    static final String UPDATE_ADDRESS = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\"\n" + 
    		"  xmlns:cite=\"http://www.opengis.net/cite\"\n" + 
    		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
    		"  xmlns:wfs=\"http://www.opengis.net/wfs\">\n" + 
    		"  <wfs:Update typeName=\"cite:Buildings\">\n" + 
    		"    <wfs:Property>\n" + 
    		"      <wfs:Name>ADDRESS</wfs:Name>\n" + 
    		"      <wfs:Value>123 ABC Street</wfs:Value>\n" + 
    		"    </wfs:Property>\n" + 
    		"  </wfs:Update>\n" + 
    		"</wfs:Transaction>";
    
    static final String DELETE_ADDRESS = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\"\n" + 
    "  xmlns:cite=\"http://www.opengis.net/cite\"\n" + 
    "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
    "  xmlns:wfs=\"http://www.opengis.net/wfs\"" +
    "  xmlns:gml=\"http://www.opengis.net/gml\">\n" + 
    "  <wfs:Delete typeName=\"cite:Buildings\">" +
    "  <ogc:Filter>\n" + 
    "    <ogc:BBOX>\n" + 
    "        <ogc:PropertyName>the_geom</ogc:PropertyName>\n" + 
    "        <gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n" + 
    "           <gml:lowerCorner>-180 -90</gml:lowerCorner>\n" + 
    "           <gml:upperCorner>180 90</gml:upperCorner>\n" + 
    "        </gml:Envelope>\n" + 
    "      </ogc:BBOX>\n" +
    "  </ogc:Filter>\n" +
    "  </wfs:Delete>\n" + 
    "</wfs:Transaction>";

    /**
     * Add the test resource access manager in the spring context
     */
    protected String[] getSpringContextLocations() {
        String[] base = super.getSpringContextLocations();
        String[] extended = new String[base.length + 1];
        System.arraycopy(base, 0, extended, 0, base.length);
        extended[base.length] = "classpath:/org/geoserver/wfs/ResourceAccessManagerContext.xml";
        return extended;
    }
    
    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    /**
     * Add the users
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_DUMMY");
        props.put("cite_readfilter", "cite,ROLE_DUMMY");
        props.put("cite_readatts", "cite,ROLE_DUMMY");
        props.put("cite_insertfilter", "cite,ROLE_DUMMY");
        props.put("cite_writefilter", "cite,ROLE_DUMMY");
        props.put("cite_writeatts", "cite,ROLE_DUMMY");
        props.store(new FileOutputStream(users), "");
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        // populate the access manager
        TestResourceAccessManager tam = (TestResourceAccessManager) applicationContext
                .getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings = catalog.getFeatureTypeByName(getLayerId(MockData.BUILDINGS));

        // limits for mr readfilter
        Filter fid113 = ff.equal(ff.property("FID"), ff.literal("113"), false);
        tam.putLimits("cite_readfilter", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                fid113, null, null));

        // limits for mr readatts (both limited read attributes and features)
        List<PropertyName> readAtts = Arrays.asList(ff.property("the_geom"), ff.property("FID"));
        tam.putLimits("cite_readatts", buildings, new VectorAccessLimits(CatalogMode.HIDE,
                readAtts, fid113, null, null));

        // disallow writing on Restricted Street
        Filter restrictedStreet = ff.not(ff.like(ff.property("ADDRESS"), "*Restricted Street*", "*", "?",
                "\\"));
        tam.putLimits("cite_insertfilter", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                null, null, restrictedStreet));
        
        // allows writing only on 113
        tam.putLimits("cite_writefilter", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                null, null, fid113));
        
        // disallow writing on the ADDRESS attribute
        List<PropertyName> writeAtts = Arrays.asList(ff.property("the_geom"), ff.property("FID"));
        tam.putLimits("cite_writeatts", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                null, writeAtts, null));
    }

    public void testNoLimits() throws Exception {
        // no limits, should see all of the attributes and rows
        authenticate("cite", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("2", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("2", "count(//cite:ADDRESS)", doc);
    }

    public void testReadFilter() throws Exception {
        // should only see one feature and all attributes
        authenticate("cite_readfilter", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("113", "//cite:FID", doc);
        assertXpathEvaluatesTo("1", "count(//cite:ADDRESS)", doc);
    }

    public void testFilterAttribute() throws Exception {
        // should only see one feature
        authenticate("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
        assertXpathEvaluatesTo("113", "//cite:FID", doc);
        assertXpathEvaluatesTo("0", "count(//cite:ADDRESS)", doc);
    }

    public void testInsertNoLimits() throws Exception {
        authenticate("cite", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//ogc:FeatureId)", dom);
        assertXpathEvaluatesTo("new0", "//ogc:FeatureId/@fid", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:SUCCESS)", dom);
    }
    
    public void testInsertRestricted() throws Exception {
        authenticate("cite_insertfilter", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:FAILED)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//wfs:Message", dom);
        assertTrue(message.matches(".*write restrictions.*"));
    }
    
    public void testInsertAttributeRestricted() throws Exception {
        authenticate("cite_writeatts", "cite");
        Document dom = postAsDOM("wfs", INSERT_RESTRICTED_STREET);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:Status/wfs:FAILED)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//wfs:Message", dom);
        assertTrue(message.matches(".*write protected.*ADDRESS.*"));
    }
    
    public void testUpdateNoLimits() throws Exception {
        authenticate("cite", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        //print(dom);
        assertXpathEvaluatesTo("2", "//wfs:totalUpdated", dom);
    }
    
    public void testUpdateLimitWrite() throws Exception {
        authenticate("cite_writefilter", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        //print(dom)
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);
        
        // double check
        authenticate("cite", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        //print(doc);
        
        // check this one has been updated
        assertXpathEvaluatesTo("123 ABC Street", "//cite:Buildings[cite:FID = '113']/cite:ADDRESS", doc);
        // but the other did not
        assertXpathEvaluatesTo("215 Main Street", "//cite:Buildings[cite:FID = '114']/cite:ADDRESS", doc);
    }
    
    public void testUpdateAttributeRestricted() throws Exception {
        authenticate("cite_writeatts", "cite");
        Document dom = postAsDOM("wfs", UPDATE_ADDRESS);
        // print(dom);

        // not sure why CITE tests make us throw an exception for this one instead of using 
        // in transaction reporting...
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate("//ows:ExceptionText", dom);
        assertTrue(message.matches(".*write protected.*ADDRESS.*"));
    }
    
    public void testDeleteLimitWrite() throws Exception {
        authenticate("cite_writefilter", "cite");
        Document dom = postAsDOM("wfs", DELETE_ADDRESS);
        // print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalDeleted", dom);
        
        // double check
        authenticate("cite", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                + getLayerId(MockData.BUILDINGS));
        // print(doc);
        
        // check this one has been deleted
        assertXpathEvaluatesTo("0", "count(//cite:Buildings[cite:FID = '113'])", doc);
        // but the other did not
        assertXpathEvaluatesTo("1", "count(//cite:Buildings[cite:FID = '114'])", doc);
    }
    
    

}
