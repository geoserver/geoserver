/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panel for a parameter that can't be edited and thus its presented as a label text instead of an input field.
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class LabelParamPanel extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(LabelParamPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    public LabelParamPanel(final String id, final IModel<String> labelModel, IModel<String> paramLabelModel) {
        super(id, labelModel);
        Label label = new Label("paramName", paramLabelModel);
        TextField<String> textField = new TextField<>("paramValue", labelModel);

        add(label);
        add(textField);
    }
}
