/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * A panel which encapsulates an image next to a label.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class IconWithLabel extends Panel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        //if the panel-specific CSS file contains actual css then have the browser load the css 
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(getClass(), getClass().getSimpleName() + ".css")));
        }
    }
    protected Image image;

    protected Label label;

    /** Constructs the panel with a link containing an image and a label. */
    public IconWithLabel(final String id, final PackageResourceReference imageRef, final IModel<String> labelModel) {
        super(id);
        add(image = new Image("image", imageRef));
        add(label = new Label("label", labelModel));
    }

    /** Returns the image contained in this panel (allows playing with its attributes) */
    public Image getImage() {
        return image;
    }

    /** Returns the label wrapped by the {@link IconWithLabel} panel (allows playing with its attributes) */
    public Label getLabel() {
        return label;
    }
}
