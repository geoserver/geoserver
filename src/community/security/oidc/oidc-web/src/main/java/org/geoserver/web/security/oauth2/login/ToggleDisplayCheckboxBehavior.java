/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static java.lang.Boolean.TRUE;

import java.io.Serial;
import java.util.Set;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;

/**
 * Behavior for {@link CheckBox} components allows to toggle visibility of another {@link #targetComponent}. The
 * visibility is based on the CSS display=none property so that the regular form processing in unaffected.
 *
 * <p>Circumvents problems with Ajax based solutions which tend to loose intermediate user input in target components on
 * re-render/hide.
 *
 * @author awaterme
 */
public class ToggleDisplayCheckboxBehavior extends Behavior {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Component targetComponent;

    public ToggleDisplayCheckboxBehavior(Component targetComponent) {
        this.targetComponent = targetComponent;
    }

    @Override
    public void bind(Component component) {
        super.bind(component);

        // this is used during ajax calls (also to set it up)
        targetComponent.add(new ClassAttributeModifier() {
            @Override
            protected Set<String> update(Set<String> classes) {
                CheckBox cb = (CheckBox) component;
                Boolean lChecked = TRUE.equals(cb.getModelObject());
                if (lChecked) {
                    classes.remove("display-none");
                    classes.add("display-block");
                } else {
                    classes.add("display-none");
                    classes.remove("display-block");
                }
                return classes;
            }
        });
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        String script = "\n";

        // this will attach a change listener to the checkbox
        // the state of the checkbox will then display:block/display:none the panel
        // this is used while the user is working with the page
        script += "$('#" + targetComponent.getMarkupId(true)
                + "').parent().find(\"ul li input\").on('change',function() { \n"
                + "   var element1 = $('#"
                + targetComponent.getMarkupId(true) + "');\n"
                + "    var element = element1.parent().find(\"ul li input\")[0];\n"
                + "    if (element.checked) {\n"
                + "        element1.addClass('display-block');\n"
                + "        element1.removeClass('display-none');\n"
                + "    } else {\n"
                + "        element1.addClass('display-none');\n"
                + "        element1.removeClass('display-block');\n"
                + "   }\n"
                + "} \n);\n\n";

        script += "$('#" + targetComponent.getMarkupId(true)
                + "').parent().find(\"ul li input\").trigger(\"change\"); // ensure in correct state";

        script += "\n";
        response.render(OnDomReadyHeaderItem.forScript(script));
    }
}
