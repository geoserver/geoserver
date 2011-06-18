package org.geoserver.web.wicket;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;

import static org.geoserver.web.GeoServerWicketTestSupport.initResourceSettings;

public class KeywordsEditorTest extends TestCase {
    
    WicketTester tester;
    ArrayList<String> keywords;

    @Override
    protected void setUp() throws Exception {
        tester = new WicketTester();
        initResourceSettings(tester);
        keywords = new ArrayList<String>();
        keywords.add("one");
        keywords.add("two");
        keywords.add("three");
        tester.startPage(new FormTestPage(new ComponentBuilder() {
        
            public Component buildComponent(String id) {
                return new KeywordsEditor(id, new Model(keywords));
            }
        }));
    }
    
    public void testRemove() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        FormTester ft = tester.newFormTester("form");
        ft.selectMultiple("panel:keywords", new int[] {0, 2});
        tester.executeAjaxEvent("form:panel:removeKeywords", "onclick");
        
        assertEquals(1, keywords.size());
        assertEquals("two", keywords.get(0));
    }
    
    public void testAdd() throws Exception {
        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:newKeyword", "four");
        tester.executeAjaxEvent("form:panel:addKeyword", "onclick");
        
        assertEquals(4, keywords.size());
        assertEquals("four", keywords.get(3));
    }

}
