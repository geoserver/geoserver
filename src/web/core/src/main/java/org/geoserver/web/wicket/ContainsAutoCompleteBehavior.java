/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;

/**
 * Suggests any of the fixed choices that contain the typed text (case insensitive). Attach to a text field to offer a
 * set of template values, e.g. example JDBC connection URLs.
 */
public class ContainsAutoCompleteBehavior extends AutoCompleteBehavior<String> {
    @Serial
    private static final long serialVersionUID = 993566054116148859L;

    private final List<String> choices;

    public ContainsAutoCompleteBehavior(List<String> choices) {
        super(new AbstractAutoCompleteTextRenderer<>() {
            @Serial
            private static final long serialVersionUID = 3192368880726583011L;

            @Override
            protected String getTextValue(String object) {
                return object;
            }
        });
        settings.setPreselect(true).setShowListOnEmptyInput(true).setShowCompleteListOnFocusGain(true);
        this.choices = new ArrayList<>(choices);
    }

    public ContainsAutoCompleteBehavior(String... choices) {
        this(Arrays.asList(choices));
    }

    @Override
    protected Iterator<String> getChoices(String input) {
        String ucInput = input.toUpperCase();
        List<String> result = new ArrayList<>();
        for (String choice : choices) {
            if (choice.toUpperCase().contains(ucInput)) result.add(choice);
        }
        return result.iterator();
    }
}
