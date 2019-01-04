/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A simple bookmarkable link with a label inside. This is a utility component, avoid some
 * boilerplate code in case the link is really just pointing to a bookmarkable page without side
 * effects
 *
 * @author Andrea Aime - OpenGeo
 */
public class SimpleBookmarkableLink extends Panel {
    private static final long serialVersionUID = -7688902365198291065L;
    BookmarkablePageLink<?> link;
    Label label;

    public <C extends Page> SimpleBookmarkableLink(
            String id, Class<C> pageClass, IModel<?> labelModel, String... pageParams) {
        this(id, pageClass, labelModel, toPageParameters(pageParams));
    }

    private static PageParameters toPageParameters(String[] pageParams) {
        if (pageParams.length % 2 == 1)
            throw new IllegalArgumentException(
                    "The page parameters array should contain an even number of elements");

        PageParameters result = new PageParameters();
        for (int i = 0; i < pageParams.length; i += 2) {
            String name = pageParams[i];
            String value = pageParams[i + 1];
            // starting with wicket 6 the value cannot be null, in that case the
            // param should not be provided
            if (value != null) {
                result.add(name, value);
            }
        }

        return result;
    }

    public <C extends Page> SimpleBookmarkableLink(
            String id, Class<C> pageClass, IModel<?> labelModel, PageParameters params) {
        super(id, labelModel);

        add(link = new BookmarkablePageLink<Object>("link", pageClass, params));
        link.add(label = new Label("label", labelModel));
    }

    public BookmarkablePageLink<?> getLink() {
        return link;
    }

    public Label getLabel() {
        return label;
    }
}
