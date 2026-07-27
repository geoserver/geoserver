/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * A panel with two arrows, up and down, supposed to reorder items in a container (a table)
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class UpDownPanel<T> extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(UpDownPanel.class);
    private static final int DEBOUNCE_MS = 500;

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

    private AbstractDefaultAjaxBehavior debouncedMoveBehavior;

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
        this.debouncedMoveBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();
                String direction = params.getParameterValue("direction").toOptionalString();
                int count = params.getParameterValue("count").toInt(0);
                if (count <= 0 || direction == null) {
                    return;
                }
                if ("up".equals(direction)) {
                    moveBy(-count, target, items);
                } else if ("down".equals(direction)) {
                    moveBy(count, target, items);
                }
            }
        };
        add(this.debouncedMoveBehavior);

        upLink = new ImageAjaxLink<Void>("up", "gs-icon-arrow-up") {
            @Serial
            private static final long serialVersionUID = 2377129539852597050L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                moveBy(-1, target, items);
            }

            @Override
            protected IAjaxCallListener getAjaxCallListener() {
                return buildDebounceListener("up");
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

        downLink = new ImageAjaxLink<Void>("down", "gs-icon-arrow-down") {
            @Serial
            private static final long serialVersionUID = -1770135905138092575L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                moveBy(1, target, items);
            }

            @Override
            protected IAjaxCallListener getAjaxCallListener() {
                return buildDebounceListener("down");
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

    private IAjaxCallListener buildDebounceListener(String direction) {
        return new AjaxCallListener() {
            @Override
            public CharSequence getPrecondition(Component component) {
                String callbackUrl = JavaScriptUtils.escapeQuotes(
                                debouncedMoveBehavior.getCallbackUrl().toString())
                        .toString();
                String key = JavaScriptUtils.escapeQuotes(getMarkupId()).toString();
                return """
                    var el = document.getElementById('%s');
                    if(!el){return false;}
                    var state = el._gsUpDownState || (el._gsUpDownState = {count:0, direction:null, timer:null});
                    if(state.direction !== '%s'){state.direction='%s'; state.count=0;}
                    state.count += 1;
                    if(state.timer){clearTimeout(state.timer);}
                    state.timer = setTimeout(function(){
                    var count=state.count; var dir=state.direction;
                    state.count=0; state.direction=null; state.timer=null;
                    if(!count || !dir){return;}
                    Wicket.Ajax.post({u:'%s', ep:{direction:dir, count:count}});
                    }, %d);
                    return false;
                    """
                        .formatted(key, direction, direction, callbackUrl, DEBOUNCE_MS);
            }
        };
    }

    private void moveBy(int delta, AjaxRequestTarget target, List<T> items) {
        int index = items.indexOf(entry);
        if (index < 0) {
            return;
        }
        int count = Math.abs(delta);
        if (count == 0) {
            return;
        }
        items.remove(index);
        int newIndex;
        if (delta < 0) {
            newIndex = Math.max(0, index - count);
        } else {
            newIndex = Math.min(items.size(), index + count);
        }
        items.add(newIndex, entry);
        target.add(container);
        target.add(this);
        target.add(downLink);
        target.add(upLink);
    }
}
