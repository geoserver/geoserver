/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A simple external link with a label inside. This is a utility component, avoid some boilerplate
 * code in case the link is really just a link with a label inside
 */
public class SimpleExternalLink extends Panel {

    private static final long serialVersionUID = -5857914940426458362L;
    ExternalLink link;
    Label label;

    public ExternalLink getLink() {
        return link;
    }

    public Label getLabel() {
        return label;
    }

    public SimpleExternalLink(String id) {
        this(id, null);
    }

    public SimpleExternalLink(String id, IModel<String> model) {
        this(id, model, model);
    }

    public SimpleExternalLink(String id, IModel<String> linkModel, IModel<?> labelModel) {
        super(id, linkModel);

        add(link = new ExternalLink("link", linkModel));
        link.add(label = new Label("label", labelModel));
    }
}
