/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/** Verifies the hidden value field maps to the three model states, including the null to false transition. */
public class ThreeStateCheckBoxTest extends GeoServerWicketTestSupport {

    @Test
    public void testNullToFalse() {
        assertMapping(null, "false", Boolean.FALSE);
    }

    @Test
    public void testNullToTrue() {
        assertMapping(null, "true", Boolean.TRUE);
    }

    @Test
    public void testTrueToNull() {
        assertMapping(Boolean.TRUE, "", null);
    }

    @Test
    public void testFalseToTrue() {
        assertMapping(Boolean.FALSE, "true", Boolean.TRUE);
    }

    private void assertMapping(Boolean initial, String submitted, Boolean expected) {
        IModel<Boolean> model = new Model<>(initial);
        tester.startPage(new FormTestPage(id -> new ThreeStateCheckBox(id, model)));

        FormTester form = tester.newFormTester("form");
        form.setValue("panel:value", submitted);
        form.submit();

        assertEquals(expected, model.getObject());
    }
}
