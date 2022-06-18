package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class StyleFormatLabelTest extends GeoServerWicketTestSupport implements Serializable {

    private static final long serialVersionUID = -5604201543567489561L;

    @Test
    public void testLoad() {
        tester.startPage(
                new FormTestPage(
                        (ComponentBuilder)
                                id -> {
                                    IModel<String> format = new Model<>("sld");
                                    return new StyleFormatLabel(id, format, null);
                                }));

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        // tester.assertComponent("content", StyleFormatLabel.class);
        // tester.startComponentInPage(StyleFormatLabel.class);
        // tester.getTagByWicketId("styleFormatLabel").getValue()
        // Assert.assertEquals(tester.getTagByWicketId("styleFormatLabel").getValue(), "SLD");
        Page page = tester.getLastRenderedPage();

        Label label =
                (Label) tester.getComponentFromLastRenderedPage("form:panel:styleFormatLabel");

        label.getId();
    }

    public void testWithNullVersion() {
        IModel<String> format = new Model<>("sld");
        StyleFormatLabel sfl = new StyleFormatLabel("id1", format, null);
        String formatLabel = (String) sfl.getMarkupAttributes().get("title");
        assertEquals("SLD", formatLabel);
    }
}
