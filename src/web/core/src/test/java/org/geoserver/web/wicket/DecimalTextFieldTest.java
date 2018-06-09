/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.*;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DecimalTextFieldTest {

    WicketTester tester;
    Double theValue;

    static Locale originalLocale;

    @BeforeClass
    public static void setLocale() {
        // setting the local on the wicket session is not reliable...
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ITALIAN);
    }

    @AfterClass
    public static void resetLocale() {
        Locale.setDefault(originalLocale);
    }

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);
        tester.startPage(
                new InputTestPage() {

                    @Override
                    protected Component newTextInput(String id) {
                        return new DecimalTextField(
                                id,
                                new PropertyModel<Double>(DecimalTextFieldTest.this, "theValue"));
                    }
                });
    }

    @Test
    public void testEmpty() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "");
        ft.submit();

        assertNull(theValue);
    }

    @Test
    public void testNaN() throws Exception {
        theValue = Double.NaN;
        setUp();
        FormTester ft = tester.newFormTester("form");
        ft.submit();
        assertEquals((Double) Double.NaN, theValue);
    }

    @Test
    public void testLocale() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "12,15");
        ft.submit();

        assertEquals(12.15, theValue, 0d);
    }

    @Test
    public void testScientific() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "1E5");
        ft.submit();

        assertEquals(100000, theValue, 0d);
    }
}
