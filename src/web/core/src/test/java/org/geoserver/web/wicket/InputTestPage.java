/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

public abstract class InputTestPage extends WebPage {

    public InputTestPage() {
        Form form = new Form("form");
        add(form);

        form.add(newTextInput("input"));
    }

    protected abstract Component newTextInput(String id);
}
