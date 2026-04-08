package org.geoserver.acl.plugin.it.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;

import java.io.IOException;
import java.util.List;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManager;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.decorators.SecuredFeatureLocking;
import org.geoserver.security.decorators.SecuredFeatureStore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests WFS transactions integration with ACL rules
 *
 * <p>Test data for reference:
 *
 * <pre>
 * iau:MarsPoi:
 * mars.1
 *   geom: POINT (-36.897 -27.2282)
 *   name: Blunck
 *   diameter: 66.485
 * mars.2
 *   geom: POINT (-36.4134 -30.3621)
 *   name: Martynov
 *   diameter: 61.0
 * mars.3
 *   geom: POINT (-2.75999999999999 -86.876)
 *   name: Australe Mensa
 *   diameter: 172.0
 * mars.4
 *   geom: POINT (-65 -9.885)
 *   name: Ophir
 *   diameter: 0.0
 * </pre>
 */
public class TransactionIntegrationTest extends AbstractAclWFSIntegrationTest {

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        addUser("iau", "pwd", null, List.of("ROLE_IAU"));
    }

    @Before
    public void setUpAcl() throws IOException {
        support.addRule(10, DENY, null, null, null, null, null, null);
    }

    @Test
    public void testInsertAsAdmin() throws Exception {
        loginAsAdmin();
        Document dom = insert(0, 0, "origin", 1);
        assertTransactionSummary(dom, 1, 0, 0);
    }

    @Test
    public void testInsertAsAnonymous() throws Exception {

        Document response = insert(0, 0, "origin", 1);

        assertLayerNotAvailable(response);
    }

    @Test
    public void testInsert() throws Exception {
        login("iau", "pwd", "ROLE_IAU");

        Document response;
        response = insert(-180, -90, "bottom_left", 1);
        assertLayerNotAvailable(response);

        support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");

        response = insert(-180, -90, "bottom_left", 1);
        assertTransactionSummary(response, 1, 0, 0);
    }

    @Test
    public void testInsertWithCqlWriteRule() throws Exception {
        Rule allowRule = support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");
        support.setCqlWriteFilter(allowRule, "diameter > 1");

        login("iau", "pwd", "ROLE_IAU");

        int diameter = 0;
        Document response = insert(-32, -1, "cqlfiler_insert_test", diameter);
        assertExceptionReport(
                response,
                "Insert error: At least one of the features inserted does not satisfy your write restrictions");

        diameter = 2;
        response = insert(-32, -1, "cqlwrite_test", diameter);
        assertTransactionSummary(response, 1, 0, 0);
    }

    private Document insert(double x, double y, String name, int diameter) throws Exception {
        String xml =
                """
          <Transaction service="WFS" version="1.1.0"
            xmlns="http://www.opengis.net/wfs"
            xmlns:gml="http://www.opengis.net/gml"
            xmlns:iau="http://geoserver.org/iau">

            <Insert>
              <iau:MarsPoi>
               <iau:geom>
                 <gml:Point><gml:pos>%f %f</gml:pos></gml:Point>
               </iau:geom>
               <iau:name>%s</iau:name>
               <iau:diameter>%d</iau:diameter>
              </iau:MarsPoi>
            </Insert>
          </Transaction>
          """
                        .formatted(x, y, name, diameter);
        return postAsDOM("wfs", xml);
    }

    @Test
    public void testUpdateAsAnonymous() throws Exception {
        Document response = update("name", "Blunck", "name", "Blunck updated");
        assertLayerNotAvailable(response);
    }

    @Test
    public void testUpdateAsAdmin() throws Exception {
        loginAsAdmin();
        Document response = update("name", "Blunck", "name", "Blunck updated");
        int expectedUpdates = 1;
        assertTransactionSummary(response, 0, expectedUpdates, 0);

        response = update("name", "Blunck updated", "diameter", "62");
        expectedUpdates = 1;
        assertTransactionSummary(response, 0, expectedUpdates, 0);
    }

    @Test
    public void testUpdateWithCqlWriteRule() throws Exception {
        Rule allowRule = support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");
        support.setCqlWriteFilter(allowRule, "diameter = 0");

        login("iau", "pwd", "ROLE_IAU");

        // Martynov has diameter=61, should not allow to update its name
        Document response = update("name", "Martynov", "name", "Martynov updated");
        int expectedUpdates = 0;
        assertTransactionSummary(response, 0, expectedUpdates, 0);

        // Ophir has diameter=0.0, should allow to update its name
        response = update("name", "Ophir", "name", "Ophir updated");
        expectedUpdates = 1;
        assertTransactionSummary(response, 0, expectedUpdates, 0);

        // revert name change
        response = update("name", "Ophir updated", "name", "Ophir");
        expectedUpdates = 1;
        assertTransactionSummary(response, 0, expectedUpdates, 0);
    }

    /**
     * Tests wfs:Update tries to set a property to a value that doesn't match the
     * {@link LayerDetails#getCqlFilterWrite() write filter}. This is currently un-enforceable.
     * {@link AclResourceAccessManager#buildVectorAccessLimits()} correctly creates the {@link VectorAccessLimits}, but
     * {@link SecuredFeatureLocking#modifyFeatures(org.geotools.api.feature.type.Name[], Object[],
     * org.geotools.api.filter.Filter)} and
     * {@link SecuredFeatureStore#modifyFeatures(org.geotools.api.feature.type.Name[], Object[],
     * org.geotools.api.filter.Filter)} do not validate the names/values arrays comply with the
     * {@link VectorAccessLimits#getWriteFilter()}
     */
    @Test
    @Ignore("Exposes https://github.com/geoserver/geoserver-acl/issues/85")
    public void testUpdateWithCqlWriteRule_newValueInvalid() throws Exception {
        Rule allowRule = support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");
        support.setCqlWriteFilter(allowRule, "diameter = 0");

        login("iau", "pwd", "ROLE_IAU");
        // should not allow to change the diameter to a new value that does not comply with the CQL write filter
        // 'diameter = 0'
        Document response = update("name", "Ophir", "diameter", "2");
        print(response);
        int expectedUpdates = 0;
        assertTransactionSummary(response, 0, expectedUpdates, 0);
    }

    private Document update(String filterProperty, String filterValue, String updateProperty, String updateValue)
            throws Exception {
        String xml =
                """
                <wfs:Transaction service="WFS" version="1.1.0"
                   xmlns:iau="http://geoserver.org/iau"
                   xmlns:ogc="http://www.opengis.net/ogc"
                   xmlns:gml="http://www.opengis.net/gml"
                   xmlns:wfs="http://www.opengis.net/wfs">

                  <wfs:Update typeName="iau:MarsPoi">
                    <wfs:Property>
                      <wfs:Name>%s</wfs:Name>
                      <wfs:Value>%s</wfs:Value>
                    </wfs:Property>
                    <ogc:Filter>
                      <ogc:PropertyIsEqualTo>
                        <ogc:PropertyName>%s</ogc:PropertyName>
                        <ogc:Literal>%s</ogc:Literal>
                      </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                  </wfs:Update>
                </wfs:Transaction>
			    """
                        .formatted(updateProperty, updateValue, filterProperty, filterValue);

        Document response = postAsDOM("wfs", xml);
        return response;
    }

    private void assertLayerNotAvailable(Document exceptionReport) throws XpathException {
        String expectedValue = "Feature type 'MarsPoi' is not available";
        assertExceptionReport(exceptionReport, expectedValue);
    }

    protected void assertTransactionSummary(Document dom, int expectedInserts, int expectedUpdates, int expectedDeletes)
            throws Exception {
        // print(dom);
        assertXpathEvaluatesTo(
                String.valueOf(expectedInserts),
                "//wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalInserted",
                dom);
        assertXpathEvaluatesTo(
                String.valueOf(expectedUpdates),
                "//wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalUpdated",
                dom);
        assertXpathEvaluatesTo(
                String.valueOf(expectedDeletes),
                "//wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalDeleted",
                dom);
    }
}
