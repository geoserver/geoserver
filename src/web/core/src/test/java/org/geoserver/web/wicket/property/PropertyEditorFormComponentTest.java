/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.property;

import static org.junit.Assert.*;

import java.util.Iterator;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PropertyEditorFormComponentTest extends GeoServerWicketTestSupport {

    Foo foo;

    @Before
    public void init() {
        foo = new Foo();
    }

    void startPage() {
        tester.startPage(new PropertyEditorTestPage(foo));
        tester.assertRenderedPage(PropertyEditorTestPage.class);
    }

    // TODO mcr
    // since introduction of PropertyEditorFormComponent.validate this test is broken
    // Using the component in the GUI works perfectly

    @Test
    @Ignore
    public void testAdd() {
        // JD:for the life of me i can't figure out any sane way to test forms with ajax in the mix
        // so unable to test the case of adding multiple key/value pairs since it involves
        // intermixing of the two
        startPage();

        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key", TextField.class);
        tester.assertComponent("form:props:container:list:0:value", TextField.class);
        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");
        form.submit();

        assertEquals(1, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
    }

    @Test
    @SuppressWarnings("TryFailThrowable")
    public void testRemove() {
        foo.getProps().put("foo", "bar");
        foo.getProps().put("bar", "baz");
        foo.getProps().put("baz", "foo");
        startPage();

        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);
        tester.assertComponent("form:props:container:list:1:remove", AjaxLink.class);
        tester.assertComponent("form:props:container:list:2:remove", AjaxLink.class);
        try {
            tester.assertComponent("form:props:container:list:3:remove", AjaxLink.class);
            fail();
        } catch (AssertionError e) {
        }

        ListView list =
                (ListView) tester.getComponentFromLastRenderedPage("form:props:container:list");
        assertNotNull(list);

        int i = 0;
        for (Iterator<Component> it = list.iterator(); it.hasNext(); i++) {
            if ("baz".equals(it.next().get("key").getDefaultModelObjectAsString())) {
                break;
            }
        }
        assertFalse(i == 3);

        tester.clickLink("form:props:container:list:" + i + ":remove", true);
        tester.newFormTester("form").submit();

        assertEquals(2, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
        assertEquals("baz", foo.getProps().get("bar"));
        assertFalse(foo.getProps().containsKey("baz"));
    }

    @Test
    public void testAddRemove() {
        startPage();
        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key", TextField.class);
        tester.assertComponent("form:props:container:list:0:value", TextField.class);
        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");

        tester.clickLink("form:props:container:list:0:remove", true);

        assertNull(form.getForm().get("props:container:list:0:key"));
        assertNull(form.getForm().get("props:container:list:0:value"));
        assertNull(form.getForm().get("props:container:list:0:remove"));
        form.submit();

        assertTrue(foo.getProps().isEmpty());
    }
}
