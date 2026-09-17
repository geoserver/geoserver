/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.util.List;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.csw.CSWConfiguration;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests the includeUnadvertised vendor parameter of GetRecords and GetRecordById. */
public class GetRecordsUnadvertisedTest extends CSWInternalTestSupport {

    private static final String ALL_RECORDS =
            "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=full&maxRecords=100";

    private static final String CITE_RECORDS =
            "cite/csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=csw:Record&resultType=results&elementSetName=full&maxRecords=100";

    private static final String GET_RECORDS_POST =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <csw:GetRecords xmlns:csw="http://www.opengis.net/cat/csw/2.0.2"
                service="CSW" version="2.0.2" resultType="results" maxRecords="100"
                outputSchema="http://www.opengis.net/cat/csw/2.0.2">
              <csw:Query typeNames="csw:Record">
                <csw:ElementSetName>full</csw:ElementSetName>
              </csw:Query>
            </csw:GetRecords>
            """;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/csw/store/internal/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // un-advertise forests and lakes, both in the cite workspace, so we can test the
        // includeUnadvertised flag
        ResourceInfo forests = getCatalog().getResourceByName("Forests", ResourceInfo.class);
        forests.setAdvertised(false);
        getCatalog().save(forests);
        ResourceInfo lakes = getCatalog().getResourceByName("Lakes", ResourceInfo.class);
        lakes.setAdvertised(false);
        getCatalog().save(lakes);

        // citeAdmin administers the cite workspace, but has no rights on lakes
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        tam.putLimits("citeAdmin", cite, new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, true));
        tam.putLimits("citeAdmin", lakes, new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE));
        // reader has no rights on BasicPolygons, which stays advertised
        ResourceInfo basicPolygons = getCatalog().getResourceByName("BasicPolygons", ResourceInfo.class);
        tam.putLimits("reader", basicPolygons, new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE));
    }

    @Test
    public void testAdminWithFlag() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        Document d = getAsDOM(ALL_RECORDS + "&includeUnadvertised=true");
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("29", "count(//csw:SearchResults/*)", d);
        // a full administrator gets both unadvertised layers back
        assertXpathExists("//csw:Record[dc:title='Forests']", d);
        assertXpathExists("//csw:Record[dc:title='Lakes']", d);
    }

    @Test
    public void testAdminWithoutFlag() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        Document d = getAsDOM(ALL_RECORDS);
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("27", "count(//csw:SearchResults/*)", d);
        assertXpathNotExists("//csw:Record[dc:title='Forests']", d);
    }

    @Test
    public void testFlagWithoutAdmin() throws Exception {
        Document d = getAsDOM(ALL_RECORDS + "&includeUnadvertised=true");
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("27", "count(//csw:SearchResults/*)", d);
        assertXpathNotExists("//csw:Record[dc:title='Forests']", d);
    }

    @Test
    public void testWorkspaceAdminInVirtualService() throws Exception {
        login("citeAdmin", "citeAdmin", "ROLE_DUMMY");

        Document baseline = getAsDOM(CITE_RECORDS);
        assertXpathNotExists("//csw:Record[dc:title='Forests']", baseline);

        Document d = getAsDOM(CITE_RECORDS + "&includeUnadvertised=true");
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathExists("//csw:Record[dc:title='Forests']", d);
        // the workspace restriction still applies, layers of other workspaces stay out
        assertXpathNotExists("//csw:Record[dc:title='PrimitiveGeoFeature']", d);
        // and so does security, Lakes is off limits for this administrator
        assertXpathNotExists("//csw:Record[dc:title='Lakes']", d);
    }

    /**
     * The record count is filtered by security like the records themselves, a mismatch would let the client page past
     * the end of the results.
     */
    @Test
    public void testRecordsMatchedHonorsSecurity() throws Exception {
        login("reader", "reader", "ROLE_DUMMY");

        Document d = getAsDOM(ALL_RECORDS);
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("26", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("26", "count(//csw:SearchResults/*)", d);
        assertXpathNotExists("//csw:Record[dc:title='BasicPolygons']", d);
    }

    @Test
    public void testWorkspaceAdminOnGlobalService() throws Exception {
        login("citeAdmin", "citeAdmin", "ROLE_DUMMY");

        Document d = getAsDOM(ALL_RECORDS + "&includeUnadvertised=true");
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("27", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("27", "count(//csw:SearchResults/*)", d);
        assertXpathNotExists("//csw:Record[dc:title='Forests']", d);
    }

    @Test
    public void testGetRecordById() throws Exception {
        String forestId = getCatalog().getLayerByName("Forests").getResource().getId();
        String request = "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=csw:Record&id=" + forestId;

        Document d = getAsDOM(request);
        assertXpathNotExists("//csw:SummaryRecord[dc:title='Forests']", d);

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        d = getAsDOM(request);
        assertXpathNotExists("//csw:SummaryRecord[dc:title='Forests']", d);

        d = getAsDOM(request + "&includeUnadvertised=true");
        checkValidationErrors(d);
        assertXpathEvaluatesTo("abstract about Forests", "//csw:SummaryRecord[dc:title='Forests']/dct:abstract", d);
    }

    /** A POST request carries the vendor parameter on the query string. */
    @Test
    public void testPostWithFlag() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        Document d = postAsDOM("csw?includeUnadvertised=true", GET_RECORDS_POST);
        checkValidationErrors(d, new CSWConfiguration());

        assertXpathEvaluatesTo("29", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathExists("//csw:Record[dc:title='Forests']", d);
    }
}
