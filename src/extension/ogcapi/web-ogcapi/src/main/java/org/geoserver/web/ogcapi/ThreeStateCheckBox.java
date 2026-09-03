/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.ogcapi.ConformanceInfo;
import org.jspecify.annotations.Nullable;

/**
 * Three-state checkbox editing a {@link Boolean} that may be {@code null}.
 *
 * <p>The visible checkbox cycles through the three states on click: indeterminate ({@code null}), checked
 * ({@code true}), unchecked ({@code false}). A hidden field carries the authoritative value so {@code false} and
 * {@code null} stay distinct on submit; a plain checkbox could not tell them apart. Keeping {@code null} avoids
 * persisting the "follow the default" state as an explicit {@code false} in the {@link ConformanceInfo}.
 */
class ThreeStateCheckBox extends FormComponentPanel<Boolean> {

    private String state;

    private final HiddenField<String> value;

    public ThreeStateCheckBox(String id, IModel<Boolean> model) {
        super(id, model);
        this.state = toState(model.getObject());
        this.value = new HiddenField<>("value", new PropertyModel<>(this, "state"));
        this.value.setOutputMarkupId(true);
        WebMarkupContainer display = new WebMarkupContainer("display");
        display.setOutputMarkupId(true);
        add(value, display);
        add(new TriStateBehavior(display.getMarkupId(), value.getMarkupId()));
    }

    @Override
    public void convertInput() {
        value.processInput();
        setConvertedInput(fromState(value.getConvertedInput()));
    }

    /** Wicket property model accessor for the hidden field; not called directly. */
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Nullable
    private static Boolean fromState(@Nullable String state) {
        if ("true".equals(state)) return Boolean.TRUE;
        if ("false".equals(state)) return Boolean.FALSE;
        return null;
    }

    private static String toState(@Nullable Boolean value) {
        return value == null ? "" : value.toString();
    }

    /** Loads the cycling script and wires the display checkbox to its hidden value field on render. */
    private static class TriStateBehavior extends Behavior {
        private final String displayId;
        private final String valueId;

        TriStateBehavior(String displayId, String valueId) {
            this.displayId = displayId;
            this.valueId = valueId;
        }

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            response.render(JavaScriptHeaderItem.forReference(
                    new PackageResourceReference(ThreeStateCheckBox.class, "ThreeStateCheckBox.js")));
            response.render(OnDomReadyHeaderItem.forScript("gsTriStateInit('" + displayId + "','" + valueId + "')"));
        }
    }
}
