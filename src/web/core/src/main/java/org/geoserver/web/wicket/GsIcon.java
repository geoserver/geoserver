package org.geoserver.web.wicket;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Renders an icon as <i class="gs-icon gs-icon-{name}"></i>. Drop-in replacement for Wicket Image components used for
 * silk/geosilk icons.
 */
public class GsIcon extends WebComponent {

    public GsIcon(String id, String cssClass) {
        this(id, Model.of(cssClass));
    }

    public GsIcon(String id, IModel<String> cssClassModel) {
        super(id, cssClassModel);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.setName("i");
        tag.put("class", "gs-icon " + getDefaultModelObjectAsString());
        tag.setType(XmlTag.TagType.OPEN);
    }
}
