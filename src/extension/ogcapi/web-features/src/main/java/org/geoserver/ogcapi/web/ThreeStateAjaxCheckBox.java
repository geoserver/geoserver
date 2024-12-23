/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.ConformanceInfo;
import org.springframework.lang.Nullable;

/**
 * Three-state {@link AjaxCheckBox} preserving the {@code null} initial model value.
 *
 * <p>
 *
 * <ul>
 *   <li>If the initial state is undefined (null), checking and unchecking resolves to undefined.
 *   <li>If the initial state is false, checking and unchecking resolves back to false.
 * </ul>
 *
 * <p>This prevents the "undefined" states to be persisted as {@code false} when serializing the
 * {@link ConformanceInfo}.
 *
 * <p><strong>Beware</strong> {@link #getModel()} can hence return {@code null}.
 */
@SuppressWarnings("serial")
abstract class ThreeStateAjaxCheckBox extends AjaxCheckBox {

    public enum State {
        TRUE(true),
        FALSE(false),
        UNDEFINED(null);

        @Nullable
        private Boolean value;

        private State(Boolean nullableState) {
            this.value = nullableState;
        }

        @Nullable
        public Boolean value() {
            return value;
        }

        public static State valueOf(Boolean value) {
            return null == value ? UNDEFINED : value.booleanValue() ? TRUE : FALSE;
        }
    }

    private State state;

    private final State initialState;

    public ThreeStateAjaxCheckBox(String id, IModel<Boolean> model) {
        super(id, model);
        this.state = State.valueOf(model.getObject());
        this.initialState = state;
        super.internalOnModelChanged();
    }

    /**
     * @return {@code null} when the initial state is {@code null} and the final state is "unchecked", {@code true} or
     *     {@code false} otherwise according to the model value.
     */
    @Override
    @Nullable
    public Boolean getModelObject() {
        return super.getModelObject();
    }

    /**
     * Prevents the null (undefined) state from being replaced by false during form submission by directly handling the
     * model’s value
     */
    @Override
    public void updateModel() {
        Boolean convertedInput = super.getConvertedInput();

        if (State.UNDEFINED == initialState && Boolean.FALSE.equals(convertedInput)) {
            state = State.UNDEFINED;
        } else {
            state = State.valueOf(convertedInput);
        }

        setModelObject(state.value());
    }

    /** Ensure the “checked” attribute is removed when the state is undefined */
    @Override
    protected void onComponentTag(org.apache.wicket.markup.ComponentTag tag) {
        super.onComponentTag(tag);

        if (state == State.TRUE) {
            tag.put("checked", "checked");
        } else { // holds on for both FALSE and UNDEFINED
            tag.remove("checked");
        }
    }
}
