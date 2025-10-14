/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.List;
import java.util.Optional;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.wicketstuff.select2.Select2Behavior;

/** Auto-complete version of a {@link org.apache.wicket.markup.html.form.DropDownChoice} */
public class Select2DropDownChoice<T> extends DropDownChoice<T> {

    private static final PackageResourceReference SELECT2_KEYBOARD_JS =
            new PackageResourceReference(Select2DropDownChoice.class, "js/select2/select2-keyboard.js");

    public Select2DropDownChoice(String id, IModel<T> model, IModel<List<T>> choices, IChoiceRenderer<T> renderer) {
        super(id, model, choices, renderer);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, IModel<T> model, List<T> choices, IChoiceRenderer<T> renderer) {
        super(id, model, choices, renderer);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, List<? extends T> choices) {
        super(id, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, List<? extends T> choices, IChoiceRenderer<? super T> renderer) {
        super(id, choices, renderer);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, IModel<T> model, List<? extends T> choices) {
        super(id, model, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, IModel<? extends List<? extends T>> choices) {
        super(id, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        super(id, model, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(
            String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        super(id, choices, renderer);
        initBehaviors();
    }

    private void initBehaviors() {
        add(new KeyboardBehavior());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        Optional<AjaxRequestTarget> target = getRequestCycle().find(AjaxRequestTarget.class);
        // Unbind select2 before ajax update, or the dropdown will be duplicated
        target.ifPresent(t -> t.prependJavaScript("$('#" + getMarkupId() + "').select2('destroy');"));
    }

    /** Mimics keyboard behavior of native drop down choices */
    private static class KeyboardBehavior extends Select2Behavior {

        public KeyboardBehavior() {}

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            super.renderHead(component, response);
            response.render(JavaScriptHeaderItem.forReference(SELECT2_KEYBOARD_JS));
            String enabler = "enableSelect2Keyboard('" + component.getMarkupId() + "');";
            if (isInModal(component)) {
                // Opening and closing the dropdown is a workaround for select2 event handlers
                // interfering with the scrollbar in a modal window. The WPS subprocess builder is
                // the only case where GeoServer uses a select2 dropdown in a modal window.
                enabler = "\n  "
                        + enabler
                        + "\n  $('#"
                        + component.getMarkupId()
                        + "').select2('open');\n  $('#"
                        + component.getMarkupId()
                        + "').select2('close');\n";
            }
            response.render(OnLoadHeaderItem.forScript(enabler));
        }

        /** Checks if any containers in the component's parent hierarchy is a modal window */
        private static boolean isInModal(Component component) {
            MarkupContainer parent = component.getParent();
            while (true) {
                if (parent instanceof GSModalWindow) {
                    return true;
                } else if (parent == null) {
                    return false;
                }
                parent = parent.getParent();
            }
        }
    }
}
