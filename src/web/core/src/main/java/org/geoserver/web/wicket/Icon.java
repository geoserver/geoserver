/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * A simple {@link Image} in a panel. For when you need to add an icon in a repeater without breaking yet another
 * fragment.
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class Icon extends Panel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    /** Constructs an Icon from a resource reference. */
    public Icon(String id, ResourceReference resourceReference) {
        this(id, new Model<>(resourceReference));
    }

    /**
     * Constructs an icon from a resource reference for the image and resource model for the "title" attribute to apply
     * to the rendered "&lt;img>" tag.
     */
    public Icon(String id, ResourceReference resourceReference, IModel<String> title) {
        this(id, new Model<>(resourceReference), title);
    }

    /** Constructs an Icon from a model. */
    public Icon(String id, IModel<?> model) {
        this(id, model, null);
    }

    /**
     * Constructs an Icon from a model for the resource reference and a resource model for the "title" attribute to
     * apply to the rendered "&lt;img>" tag.
     */
    public Icon(String id, IModel<?> model, IModel<String> title) {
        super(id);
        Object reference = model.getObject();
        WebComponent image;
        if (reference instanceof PackageResourceReference) {
            image = new CachingImage("img", model);
        } else if (reference instanceof ContextRelativeResourceReference) {
            ContextRelativeResource resource = ((ContextRelativeResourceReference) reference).getResource();
            String path = (String) resource.getCacheKey();
            path = path.substring(path.indexOf("//") + 2);

            image = new ContextImage("img", path);
        } else {
            image = new Image("img", model);
        }
        if (title != null) {
            image.add(new AttributeModifier("title", title));
        }
        add(image);
    }
}
