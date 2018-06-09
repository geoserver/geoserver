/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
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
            new PackageResourceReference(
                    Select2DropDownChoice.class, "js/select2/select2-keyboard.js");

    public Select2DropDownChoice(
            String id, IModel<T> model, IModel<List<T>> choices, IChoiceRenderer<T> renderer) {
        super(id, model, choices, renderer);
        initBehaviors();
    }

    public Select2DropDownChoice(
            String id, IModel<T> model, List<T> choices, IChoiceRenderer<T> renderer) {
        super(id, model, choices, renderer);
        initBehaviors();
    }

    public Select2DropDownChoice(String id, List<? extends T> choices) {
        super(id, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(
            String id, List<? extends T> choices, IChoiceRenderer<? super T> renderer) {
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

    public Select2DropDownChoice(
            String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        super(id, model, choices);
        initBehaviors();
    }

    public Select2DropDownChoice(
            String id,
            IModel<? extends List<? extends T>> choices,
            IChoiceRenderer<? super T> renderer) {
        super(id, choices, renderer);
        initBehaviors();
    }

    private void initBehaviors() {
        add(new Select2Behavior());
        add(new KeyboardBehavior());
    }

    /** Mimics keyboard behavior of native drop down choices */
    private static class KeyboardBehavior extends Behavior {

        public KeyboardBehavior() {}

        public void renderHead(Component component, IHeaderResponse response) {
            super.renderHead(component, response);
            response.render(JavaScriptHeaderItem.forReference(SELECT2_KEYBOARD_JS));
            String enabler = "enableSelect2Keyboard('" + component.getMarkupId() + "');";
            response.render(OnLoadHeaderItem.forScript(enabler));
        }
    }
}
