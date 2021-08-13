/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.web.InternationalStringPanel;

/**
 * A reusable component for title and abstract. It adds both the normal string fields and the
 * internationalized ones.
 */
public class TitleAndAbstractPanel extends Panel {

    /**
     * @param id the wicket:id
     * @param model the model
     * @param titleLabelProperty the name of the property holding the title label value
     * @param abstractLabelProperty the name of the property holding the abstract label value
     * @param labelProvider the WebMarkupContainer being the context to retrieve the labels.
     */
    public TitleAndAbstractPanel(
            String id,
            IModel<?> model,
            String titleLabelProperty,
            String abstractLabelProperty,
            MarkupContainer labelProvider) {
        this(
                id,
                model,
                null,
                null,
                null,
                null,
                titleLabelProperty,
                abstractLabelProperty,
                labelProvider);
    }

    /**
     * @param id the wicket:id
     * @param model the model
     * @param titleProperty the property of the tile field
     * @param internationalTitleProperty the property of the internationalTitleField
     * @param abstractProperty the property of the abstract field
     * @param internationalAbstractProperty the property of the internationalAbstractField
     * @param titleLabelProperty the name of the property holding the title label value
     * @param abstractLabelProperty the name of the property holding the abstract label value
     * @param labelProvider the WebMarkupContainer being the context to retrieve the labels.
     */
    public TitleAndAbstractPanel(
            String id,
            IModel<?> model,
            String titleProperty,
            String internationalTitleProperty,
            String abstractProperty,
            String internationalAbstractProperty,
            String titleLabelProperty,
            String abstractLabelProperty,
            MarkupContainer labelProvider) {
        super(id, model);
        initUI(
                model,
                titleProperty,
                internationalTitleProperty,
                abstractProperty,
                internationalAbstractProperty,
                titleLabelProperty,
                abstractLabelProperty,
                labelProvider);
    }

    private void initUI(
            IModel<?> model,
            String titleProperty,
            String internationalTitleProperty,
            String abstractProperty,
            String internationalAbstractProperty,
            String titleLabelProperty,
            String abstractLabelProperty,
            MarkupContainer labelProvider) {
        WebMarkupContainer titleLabelContainer = new WebMarkupContainer("titleLabel");

        titleProperty = titleProperty != null ? titleProperty : "title";
        abstractProperty = abstractProperty != null ? abstractProperty : "abstract";
        internationalTitleProperty =
                internationalTitleProperty != null
                        ? internationalTitleProperty
                        : "internationalTitle";
        internationalAbstractProperty =
                internationalAbstractProperty != null
                        ? internationalAbstractProperty
                        : "internationalAbstract";
        titleLabelContainer.add(
                new Label(
                        "titleLabel", new StringResourceModel(titleLabelProperty, labelProvider)));
        add(titleLabelContainer);
        TextField<String> title =
                new TextField<>("title", new PropertyModel<>(model, titleProperty));
        add(title);
        InternationalStringPanel<TextField<String>> internationalStringPanelTitle =
                new InternationalStringPanel<TextField<String>>(
                        "internationalTitle",
                        new PropertyModel<>(model, internationalTitleProperty),
                        title,
                        titleLabelContainer) {
                    @Override
                    protected TextField<String> getTextComponent(String id, IModel<String> model) {
                        return new TextField<>(id, model);
                    }
                };
        add(internationalStringPanelTitle);

        WebMarkupContainer abstractLabelContainer = new WebMarkupContainer("abstractLabel");
        abstractLabelContainer.add(
                new Label(
                        "abstractLabel",
                        new StringResourceModel(abstractLabelProperty, labelProvider)));
        add(abstractLabelContainer);
        TextArea<String> area =
                new TextArea<>("abstract", new PropertyModel<>(model, abstractProperty));
        add(area);
        InternationalStringPanel<TextArea<String>> internationalStringPanelAbstract =
                new InternationalStringPanel<TextArea<String>>(
                        "internationalAbstract",
                        new PropertyModel<>(model, internationalAbstractProperty),
                        area,
                        abstractLabelContainer) {
                    @Override
                    protected TextArea<String> getTextComponent(String id, IModel<String> model) {
                        return new TextArea<>(id, model);
                    }
                };
        add(internationalStringPanelAbstract);
    }
}
