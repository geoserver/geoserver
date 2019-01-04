/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;

/**
 * Implements auto complete behavior to manage the roles choice as a list of string separated by ";"
 */
public class RolesAutoCompleteBehavior extends AutoCompleteBehavior<String> {

    private static final long serialVersionUID = -6743826046815447371L;
    private StringBuilder selectedRoles;
    private List<String> availableRoles;

    public RolesAutoCompleteBehavior(
            IAutoCompleteRenderer<String> renderer,
            AutoCompleteSettings settings,
            StringBuilder selectedRoles,
            List<String> availableRoles) {
        super(renderer, settings);
        this.selectedRoles = selectedRoles;
        this.availableRoles = availableRoles;
    }

    @Override
    protected Iterator<String> getChoices(String input) {
        int lastCommaIndex = input.lastIndexOf(';');
        String realInput = "";
        if (lastCommaIndex == -1) {
            selectedRoles.setLength(0);
            realInput = input;
        } else {
            selectedRoles.setLength(0);
            selectedRoles.append(input.substring(0, lastCommaIndex) + ";");
            realInput = input.substring(lastCommaIndex + 1);
        }

        List<String> completions = new ArrayList<String>();
        for (int i = 0; i < availableRoles.size(); i++) {
            String role = availableRoles.get(i);
            if (realInput.isEmpty()
                    || role.startsWith(realInput.toUpperCase())
                    || role.startsWith(realInput.toLowerCase())) {
                List<String> sr = Arrays.asList(selectedRoles.toString().split(";"));
                if (!sr.contains(role)) {
                    completions.add(role + ";");
                }
            }
        }
        Collections.sort(completions);
        return completions.iterator();
    }
}
