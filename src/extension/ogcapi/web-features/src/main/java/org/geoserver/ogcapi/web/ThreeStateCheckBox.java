/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.ConformanceInfo;
import org.jspecify.annotations.Nullable;

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
class ThreeStateCheckBox extends CheckBox {

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

    public ThreeStateCheckBox(String id, IModel<Boolean> model) {
        super(id, model);
        this.setOutputMarkupId(true);
        this.state = State.valueOf(model.getObject());
        this.initialState = state;
        this.add(new IndeterminateBehavior());
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

    private static class IndeterminateBehavior extends Behavior {

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            String id = component.getMarkupId();
            if (component.getDefaultModelObject() == null) {
                StringBuilder js = new StringBuilder();
                js.append("(function() {"); // wrap in an IIFE to avoid polluting the global scope
                js.append("var cb = document.getElementById('").append(id).append("');");
                js.append("if (cb) {");
                js.append("  cb.checked = false;");
                js.append("  cb.indeterminate = true;");
                js.append(" console.log(\"Identifier:\", cb.id);");
                js.append("  cb.classList.add('indeterminate');"); // visual style
                js.append("  cb.addEventListener('change', function() {");
                js.append("    cb.indeterminate = false;");
                js.append(" console.log(\"Identifier:\", cb.id);");
                js.append(" console.log(\"Element classes before:\", cb.className);");
                js.append("    cb.classList.remove('indeterminate');");
                js.append(" console.log(\"Element classes after:\", cb.className);");
                js.append(" cb.offsetWidth");
                js.append("  }, { once: true });");
                js.append("}");
                js.append("})();");
                response.render(OnDomReadyHeaderItem.forScript(js));
            }
        }
    }
}
