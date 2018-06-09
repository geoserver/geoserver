/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class KeywordsEditorTest {

    WicketTester tester;
    ArrayList<KeywordInfo> keywords;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);
        keywords = new ArrayList<KeywordInfo>();
        keywords.add(new Keyword("one"));
        keywords.add(new Keyword("two"));
        keywords.add(new Keyword("three"));
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            public Component buildComponent(String id) {
                                return new KeywordsEditor(id, new Model(keywords));
                            }
                        }));
    }

    @Test
    public void testRemove() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        FormTester ft = tester.newFormTester("form");
        ft.selectMultiple("panel:keywords", new int[] {0, 2});
        tester.executeAjaxEvent("form:panel:removeKeywords", "click");

        assertEquals(1, keywords.size());
        assertEquals("two", keywords.get(0).getValue());
    }

    @Test
    public void testAdd() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:newKeyword", "four");
        ft.setValue("panel:lang", "en");
        ft.setValue("panel:vocab", "foobar");
        tester.executeAjaxEvent("form:panel:addKeyword", "click");

        assertEquals(4, keywords.size());
        assertEquals("four", keywords.get(3).getValue());
        assertEquals("en", keywords.get(3).getLanguage());
        assertEquals("foobar", keywords.get(3).getVocabulary());
    }
}
