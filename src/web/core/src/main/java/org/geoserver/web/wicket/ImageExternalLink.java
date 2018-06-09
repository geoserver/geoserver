/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * A panel which encapsulates an {@link ExternalLink} containing a image and an optional label.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class ImageExternalLink extends Panel {

    protected Label label;

    protected Image image;

    protected ExternalLink link;

    /** Constructs the panel with a link containing an image and a label. */
    public ImageExternalLink(
            final String id,
            final String href,
            final PackageResourceReference imageRef,
            final IModel<String> label) {
        super(id);
        add(this.link = new ExternalLink("link", href));
        link.add(this.image = new Image("image", imageRef));
        link.add(this.label = new Label("label", label));
    }

    /** Returns the image contained in this link (allows playing with its attributes) */
    public Image getImage() {
        return image;
    }

    /**
     * Returns the link wrapped by the {@link ImageExternalLink} panel (allows playing with its
     * attributes and enable/disable the link)
     */
    public ExternalLink getLink() {
        return link;
    }

    public Label getLabel() {
        return label;
    }
}
