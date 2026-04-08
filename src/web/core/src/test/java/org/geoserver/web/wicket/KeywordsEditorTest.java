/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.util.ListModel;
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
    List<KeywordInfo> keywords;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);
        keywords = new ArrayList<>();
        keywords.add(new Keyword("one"));
        keywords.add(new Keyword("two"));
        keywords.add(new Keyword("three"));
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new KeywordsEditor(id, new ListModel<>(keywords))));
    }

    @Test
    public void testRemove() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        tester.executeAjaxEvent("form:panel:container:table:keywords:0:removeKeyword", "click");

        assertEquals(2, keywords.size());
        assertEquals("two", keywords.get(0).getValue());
    }

    @Test
    public void testAdd() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        tester.executeAjaxEvent("form:panel:container:addKeyword", "click");

        FormTester ft = tester.newFormTester("form");
        ft.getForm()
                .get("panel:container:table:keywords:3:keywordBorder:keywordBorder_body:keyword")
                .setDefaultModelObject("four");
        ft.getForm()
                .get("panel:container:table:keywords:3:keywordBorder:keywordBorder_body:language")
                .setDefaultModelObject("en");
        ft.getForm()
                .get("panel:container:table:keywords:3:vocabularyBorder:vocabularyBorder_body:vocabulary")
                .setDefaultModelObject("foobar");

        assertEquals(4, keywords.size());
        assertEquals("four", keywords.get(3).getValue());
        assertEquals("en", keywords.get(3).getLanguage());
        assertEquals("foobar", keywords.get(3).getVocabulary());

        ft = tester.newFormTester("form");
        ft.setClearFeedbackMessagesBeforeSubmit(true);
        ft.submit();
        tester.assertNoErrorMessage();
    }
}
