/* (c) 2023  Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.MultiPolygon;
import org.geolatte.geom.Point;
import org.geolatte.geom.codec.Wkt;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class GeometryWktTextAreaTest {

    private WicketTester tester;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
    }

    @Test
    public void testGenericGeometry() {
        GeometryWktTextArea<Geometry> textArea = GeometryWktTextArea.of("wkt");
        tester.startComponentInPage(new FormPage("panel", textArea));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "SRID=4326;MULTIPOLYGON(((-180 -90,-180 90,180 90,180 -90,-180 -90)))");
        form.submit();
        Object value = form.getForm().get("wkt").getDefaultModelObject();
        assertThat(value, instanceOf(MultiPolygon.class));
    }

    @Test
    public void testMultiPolygon() {
        IModel<MultiPolygon> model = Model.of();
        GeometryWktTextArea<MultiPolygon> textArea = new GeometryWktTextArea<>("wkt", MultiPolygon.class, model);
        tester.startComponentInPage(new FormPage("panel", textArea));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "SRID=4326;MULTIPOLYGON(((-180 -90,-180 90,180 90,180 -90,-180 -90)))");
        form.submit();
        Object value = form.getForm().get("wkt").getDefaultModelObject();
        assertThat(value, instanceOf(MultiPolygon.class));

        form = tester.newFormTester("panel:form");
        form.setValue("wkt", "SRID=4326;POLYGON ((-180 -90,-180 90,180 90,180 -90,-180 -90))");
        // WicketRuntimException for GeoServer 2.26.x or lower, ConversionException since 2.27.x
        RuntimeException error = assertThrows(RuntimeException.class, form::submit);
        assertTrue(error instanceof ConversionException || error instanceof WicketRuntimeException);
    }

    @Test
    public void testPolygon() {
        tester.startComponentInPage(new FormPage("panel", GeometryWktTextArea.of("wkt")));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "SRID=4326;MULTIPOLYGON(((-180 -90,-180 90,180 90,180 -90,-180 -90)))");
        form.submit();
        Object value = form.getForm().get("wkt").getDefaultModelObject();
        assertThat(value, instanceOf(MultiPolygon.class));
    }

    @Test
    public void testBlankStringToNull() {
        tester.startComponentInPage(new FormPage("panel", GeometryWktTextArea.of("wkt")));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "  ");
        form.submit();
        Object value = form.getForm().get("wkt").getDefaultModelObject();
        assertThat(value, nullValue());
    }

    @Test
    public void testInvalidWKT() {
        tester.startComponentInPage(new FormPage("panel", GeometryWktTextArea.of("wkt")));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "MULTIPOLYGON(( NAH ))");
        RuntimeException error = assertThrows(RuntimeException.class, form::submit);
        // WicketRuntimException for GeoServer 2.26.x or lower, ConversionException since 2.27.x
        assertTrue(error instanceof ConversionException || error instanceof WicketRuntimeException);
    }

    @Test
    public void testRequired() {
        Point initialValue = (Point) Wkt.fromWkt("POINT(1 1)");
        IModel<Point> model = Model.of(initialValue);
        GeometryWktTextArea<Point> textArea = new GeometryWktTextArea<>("wkt", Point.class, model);
        textArea.setRequired(true);

        tester.startComponentInPage(new FormPage("panel", textArea));
        FormTester form = tester.newFormTester("panel:form");

        form.setValue("wkt", "");
        form.submit();
        assertSame(initialValue, textArea.getModelObject());
        tester.assertErrorMessages("'wkt' is required.");
    }

    @SuppressWarnings("serial")
    static class FormPage extends Panel {

        private Form<?> form;

        public FormPage(String id, GeometryWktTextArea<?> textArea) {
            super(id);
            add(form = new Form<>("form"));
            form.add(textArea);
        }
    }
}
