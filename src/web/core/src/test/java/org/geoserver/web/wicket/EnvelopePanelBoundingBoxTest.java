/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;

public class EnvelopePanelBoundingBoxTest extends GeoServerWicketTestSupport {

    private static final String FORM = "form";

    @Test
    public void testInvalidBoundingBoxValuesEqual() throws FactoryException {
        final ReferencedEnvelope e =
                new ReferencedEnvelope3D(180, 180, 90, 90, 20, 20, CRS.decode("EPSG:7415"));
        tester.startPage(new FormTestPage(id -> providePanel(e, id)));

        tester.assertComponent(FORM, Form.class);

        FormTester ft = tester.newFormTester(FORM);

        ft.submit();

        assertEquals(3, tester.getFeedbackMessages(null).size());
    }

    @Test
    public void testInvalidBoundingBoxValuesLower() throws FactoryException {
        final ReferencedEnvelope e =
                new ReferencedEnvelope3D(180, 180, 90, 90, 20, 20, CRS.decode("EPSG:7415"));
        tester.startPage(new FormTestPage(id -> providePanel(e, id)));

        tester.assertComponent(FORM, Form.class);

        FormTester ft = tester.newFormTester(FORM);

        ft.setValue("panel:minX", "181");
        ft.setValue("panel:minY", "91");
        ft.setValue("panel:minZ", "21");

        ft.submit();

        assertEquals(3, tester.getFeedbackMessages(null).size());
    }

    @Test
    public void testValidBoundingBoxValues() throws FactoryException {
        final ReferencedEnvelope e =
                new ReferencedEnvelope3D(180, 181, 90, 91, 20, 21, CRS.decode("EPSG:7415"));
        tester.startPage(new FormTestPage(id -> providePanel(e, id)));

        tester.assertComponent(FORM, Form.class);

        FormTester ft = tester.newFormTester(FORM);

        ft.submit();

        assertEquals(0, tester.getFeedbackMessages(null).size());
    }

    private Component providePanel(ReferencedEnvelope e, String id) {
        EnvelopePanel panel = new EnvelopePanel(id, e);
        panel.setCRSFieldVisible(true);
        return panel;
    }
}
