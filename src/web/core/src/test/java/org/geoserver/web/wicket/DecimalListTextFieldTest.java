/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DecimalListTextFieldTest {

    WicketTester tester;
    List<Double> theList = new ArrayList<>();
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
                        return new DecimalListTextField(
                                id,
                                new PropertyModel<List>(DecimalListTextFieldTest.this, "theList"));
                    }
                });
    }

    @Test
    public void testEmpty() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "      ");
        ft.submit();

        assertEquals(0, theList.size());
    }

    @Test
    public void testLocale() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "1,3 12,15");
        ft.submit();

        assertEquals(2, theList.size());
        assertEquals(1.3, theList.get(0), 0d);
        assertEquals(12.15, theList.get(1), 0d);
    }

    @Test
    public void testScientific() throws Exception {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("input", "1E-3 1E5");
        ft.submit();

        assertEquals(2, theList.size());
        assertEquals(0.001, theList.get(0), 0d);
        assertEquals(100000, theList.get(1), 0d);
    }
}
