/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
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

    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(ThreeStateCheckBox.class, "ThreeStateCheckBox.js");

    private String state;

    private final HiddenField<String> value;

    private final WebMarkupContainer display;

    public ThreeStateCheckBox(String id, IModel<Boolean> model) {
        super(id, model);
        this.state = toState(model.getObject());
        this.value = new HiddenField<>("value", new PropertyModel<>(this, "state"));
        this.value.setOutputMarkupId(true);
        this.display = new WebMarkupContainer("display");
        this.display.setOutputMarkupId(true);
        add(value, display);
    }

    /** Loads the cycling script and wires the display checkbox to its hidden value field. */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(JS));
        response.render(OnDomReadyHeaderItem.forScript(
                "gsTriStateInit('" + display.getMarkupId() + "','" + value.getMarkupId() + "')"));
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
}
