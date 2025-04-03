/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static java.lang.Boolean.TRUE;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.StyleAttributeModifier;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
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
    private static final long serialVersionUID = 1L;

    private final Component targetComponent;

    public ToggleDisplayCheckboxBehavior(Component targetComponent) {
        this.targetComponent = targetComponent;
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        targetComponent.add(new StyleAttributeModifier() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Map<String, String> update(Map<String, String> pOldStyles) {
                CheckBox cb = (CheckBox) component;
                Boolean lChecked = TRUE.equals(cb.getModelObject());
                pOldStyles.put("display", lChecked ? "block" : "none");
                return pOldStyles;
            }
        });
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);

        String onchangeScript = String.format(
                "document.getElementById('%s').style.display = this.checked ? 'block' : 'none' ;",
                targetComponent.getMarkupId(true));

        tag.put("onchange", onchangeScript);
    }
}
