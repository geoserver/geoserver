/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfoImpl;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfoImpl;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;

public class OSEOLimitPanelTest extends OSEOWebTestSupport {

    public static final String ROLE1 = "R1";
    public static final String ROLE2 = "R2";
    public static final String PLATFORM_IS_S2 = "\"eo:platform\" = 's2a'";
    public static final String CLOUDCOVER_LT_10 = "\"opt:cloudCover\" < 10";

    @Before
    public void setup() throws IOException {
        ensureRolesAvailable(List.of(ROLE1, ROLE2));
    }

    /** Test panel creation and contents, collection limit */
    @Test
    public void testFillCollectionPanel() {
        EOCollectionAccessLimitInfo limit = new EOCollectionAccessLimitInfoImpl("foo > bar", List.of(ROLE1, ROLE2));
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        // not a product limit does not have a collection reference
        assertNull(tester.getComponentFromLastRenderedPage("panel:form:collectionContainer"));
        // cql filter and roles are present
        tester.assertModelValue("panel:form:cqlFilter", "foo > bar");
        tester.assertModelValue(
                "panel:form:roles:palette", List.of(new GeoServerRole(ROLE1), new GeoServerRole(ROLE2)));
    }

    @Test
    public void testNewCollectionPanel() {
        EOCollectionAccessLimitInfo limit = new EOCollectionAccessLimitInfoImpl();
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        FormTester form = tester.newFormTester("panel:form");
        form.setValue("cqlFilter", PLATFORM_IS_S2);
        form.setValue("roles:palette:recorder", ROLE1 + "," + ROLE2);

        form.submit();
        tester.assertNoErrorMessage();

        assertEquals(PLATFORM_IS_S2, limit.getCQLFilter());
        assertEquals(List.of(ROLE1, ROLE2), limit.getRoles());
    }

    /** Test panel creation and contents, product limit */
    @Test
    public void testFillProductPanel() {
        EOProductAccessLimitInfo limit =
                new EOProductAccessLimitInfoImpl(collectionNames.get(2), PLATFORM_IS_S2, List.of(ROLE1, ROLE2));
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        tester.assertModelValue("panel:form:collectionContainer:collection", collectionNames.get(2));
        tester.assertModelValue("panel:form:cqlFilter", PLATFORM_IS_S2);
        tester.assertModelValue(
                "panel:form:roles:palette", List.of(new GeoServerRole(ROLE1), new GeoServerRole(ROLE2)));
    }

    @Test
    public void testNewProductionPanel() {
        EOProductAccessLimitInfo limit = new EOProductAccessLimitInfoImpl();
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        FormTester form = tester.newFormTester("panel:form");
        form.select("collectionContainer:collection", 1);
        form.setValue("cqlFilter", CLOUDCOVER_LT_10);
        form.setValue("roles:palette:recorder", ROLE1 + "," + ROLE2);
        form.submit();
        tester.assertNoErrorMessage();

        assertEquals(collectionNames.get(1), limit.getCollection());
        assertEquals(CLOUDCOVER_LT_10, limit.getCQLFilter());
        assertEquals(List.of(ROLE1, ROLE2), limit.getRoles());
    }

    @Test
    public void testMandatoryFields() {
        EOProductAccessLimitInfo limit = new EOProductAccessLimitInfoImpl();
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        FormTester form = tester.newFormTester("panel:form");
        form.submit();
        tester.assertErrorMessages(
                "Field 'collection' is required.", "Field 'cqlFilter' is required.", "Field 'roles' is required.");
    }

    @Test
    public void testValidateFilterSyntax() {
        EOCollectionAccessLimitInfo limit = new EOCollectionAccessLimitInfoImpl();
        tester.startComponentInPage(new OSEOLimitPanel("panel", new Model<>(limit)));

        FormTester form = tester.newFormTester("panel:form");
        form.setValue("cqlFilter", "a & b"); // invalid filter
        form.setValue("roles:palette:recorder", ROLE1);
        form.submit();

        tester.assertErrorMessages(
                "Invalid CQL filter: [a & b]. Lexical error at line 1, column 4.  Encountered: ' ' (32), after prefix"
                        + " \"&\" Parsing : a & b.");

        // syntax is correct now, but attribute name is not part of the schema
        form = tester.newFormTester("panel:form");
        form.setValue("cqlFilter", "abc = 's2a'");
        form.submit();
        tester.assertErrorMessages(
                "Invalid CQL filter: [abc = 's2a']. Error on CQL filter: the attribute with name abc does not exist");

        // third time is the charm
        form = tester.newFormTester("panel:form");
        form.setValue("cqlFilter", PLATFORM_IS_S2);
        form.setValue("roles:palette:recorder", ROLE1);
        form.submit();
        tester.assertNoErrorMessage();
    }
}
