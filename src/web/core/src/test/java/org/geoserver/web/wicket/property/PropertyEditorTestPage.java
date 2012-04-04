package org.geoserver.web.wicket.property;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;

public class PropertyEditorTestPage extends WebPage {

    public PropertyEditorTestPage(Foo foo) {
        Form form = new Form("form", new CompoundPropertyModel(foo));
        form.add(new PropertyEditorFormComponent("props"));
        form.add(new SubmitLink("save"));
        add(form);
    }
}
