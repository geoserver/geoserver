/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;

/**
 * A panel with two arrows, up and down, supposed to reorder items in a container (a table)
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class UpDownPanel<T> extends Panel {

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

    @Serial
    private static final long serialVersionUID = -5964561496724645286L;

    T entry;
    private ImageAjaxLink<?> upLink;

    private ImageAjaxLink<?> downLink;

    private Component container;

    public UpDownPanel(
            String id,
            final T entry,
            final List<T> items,
            Component container,
            final StringResourceModel upTitle,
            final StringResourceModel downTitle) {
        super(id);
        this.entry = entry;
        this.setOutputMarkupId(true);
        this.container = container;

        upLink =
                new ImageAjaxLink<Void>(
                        "up", new ContextRelativeResourceReference("/img/icons/silk/arrow_up.png", false)) {
                    @Serial
                    private static final long serialVersionUID = 2377129539852597050L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = items.indexOf(UpDownPanel.this.entry);
                        items.remove(index);
                        items.add(Math.max(0, index - 1), UpDownPanel.this.entry);
                        target.add(UpDownPanel.this.container);
                        target.add(this);
                        target.add(downLink);
                        target.add(upLink);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("title", upTitle.getString());
                        if (items.indexOf(entry) == 0) {
                            tag.put("class", "visibility-hidden");
                        } else {
                            tag.put("class", "visibility-visible");
                        }
                    }
                };
        upLink.getImage().add(new AttributeModifier("alt", new ParamResourceModel("up", upLink)));
        upLink.setOutputMarkupId(true);
        add(upLink);

        downLink =
                new ImageAjaxLink<Void>(
                        "down", new ContextRelativeResourceReference("/img/icons/silk/arrow_down.png", false)) {
                    @Serial
                    private static final long serialVersionUID = -1770135905138092575L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        int index = items.indexOf(UpDownPanel.this.entry);
                        items.remove(index);
                        items.add(Math.min(items.size(), index + 1), UpDownPanel.this.entry);
                        target.add(UpDownPanel.this.container);
                        target.add(this);
                        target.add(downLink);
                        target.add(upLink);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        tag.put("title", downTitle.getString());
                        if (items.indexOf(entry) == items.size() - 1) {
                            tag.put("class", "visibility-hidden");
                        } else {
                            tag.put("class", "visibility-visible");
                        }
                    }
                };
        downLink.getImage().add(new AttributeModifier("alt", new ParamResourceModel("down", downLink)));
        downLink.setOutputMarkupId(true);
        add(downLink);
    }
}
