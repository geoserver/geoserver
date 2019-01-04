/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class EnvelopePanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testEditPlain() throws Exception {
        final ReferencedEnvelope e =
                new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new EnvelopePanel(id, e);
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        assertEquals("-180", ft.getTextComponentValue("panel:minX"));
        assertEquals("-90", ft.getTextComponentValue("panel:minY"));
        assertEquals("180", ft.getTextComponentValue("panel:maxX"));
        assertEquals("90", ft.getTextComponentValue("panel:maxY"));

        EnvelopePanel ep = (EnvelopePanel) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(e, ep.getModelObject());

        ft.setValue("panel:minX", "-2");
        ft.setValue("panel:minY", "-2");
        ft.setValue("panel:maxX", "2");
        ft.setValue("panel:maxY", "2");

        ft.submit();

        assertEquals(new Envelope(-2, 2, -2, 2), ep.getModelObject());
        assertEquals(
                DefaultGeographicCRS.WGS84,
                ((ReferencedEnvelope) ep.getModelObject()).getCoordinateReferenceSystem());
    }

    @Test
    public void testEditCRS() throws Exception {
        CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326", true);
        CoordinateReferenceSystem epsg4140 = CRS.decode("EPSG:4140", true);
        final ReferencedEnvelope e = new ReferencedEnvelope(-180, 180, -90, 90, epsg4326);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                EnvelopePanel panel = new EnvelopePanel(id, e);
                                panel.setCRSFieldVisible(true);
                                return panel;
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        // print(tester.getLastRenderedPage(), true, true);
        assertEquals("-180", ft.getTextComponentValue("panel:minX"));
        assertEquals("-90", ft.getTextComponentValue("panel:minY"));
        assertEquals("180", ft.getTextComponentValue("panel:maxX"));
        assertEquals("90", ft.getTextComponentValue("panel:maxY"));
        assertEquals("EPSG:4326", ft.getTextComponentValue("panel:crsContainer:crs:srs"));

        EnvelopePanel ep = (EnvelopePanel) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(e, ep.getModelObject());

        ft.setValue("panel:minX", "-2");
        ft.setValue("panel:minY", "-2");
        ft.setValue("panel:maxX", "2");
        ft.setValue("panel:maxY", "2");
        ft.setValue("panel:crsContainer:crs:srs", "EPSG:4140");

        ft.submit();

        assertEquals(new Envelope(-2, 2, -2, 2), ep.getModelObject());
        assertEquals(
                epsg4140,
                ((ReferencedEnvelope) ep.getModelObject()).getCoordinateReferenceSystem());
    }

    @Test
    public void testDecimalsPreserved() throws Exception {
        final ReferencedEnvelope e =
                new ReferencedEnvelope(-0.1E-10, 1.0E-9, -9E-11, 9E-11, DefaultGeographicCRS.WGS84);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new EnvelopePanel(id, e);
                            }
                        }));

        tester.assertComponent("form", Form.class);

        FormTester ft = tester.newFormTester("form");
        assertEquals("-0.00000000001", ft.getTextComponentValue("panel:minX"));
        assertEquals("-0.00000000009", ft.getTextComponentValue("panel:minY"));
        assertEquals("0.000000001", ft.getTextComponentValue("panel:maxX"));
        assertEquals("0.00000000009", ft.getTextComponentValue("panel:maxY"));
    }
}
