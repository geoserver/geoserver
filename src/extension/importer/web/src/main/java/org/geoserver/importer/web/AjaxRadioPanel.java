/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Ajax enabled radio group panel.
 *
 * <p>Taken from https://github.com/mnadeem/wicketAjaxRadio.
 *
 * @param <T>
 */
public abstract class AjaxRadioPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 1L;

    protected abstract void onRadioSelect(AjaxRequestTarget target, T newSelection);

    public AjaxRadioPanel(String id, List<T> items) {
        this(id, items, null);
    }

    public AjaxRadioPanel(String id, List<T> items, T currentSelection) {
        super(id);
        add(buildContents(items, currentSelection));
    }

    private Component buildContents(List<T> items, T currentSelection) {

        final RadioGroup<T> group = new RadioGroup<T>("radioGroup", new Model(currentSelection));
        group.add(
                new ListView<T>("radioButtons", items) {
                    @Override
                    protected void populateItem(ListItem<T> item) {
                        item.add(newRadioCell(group, item));
                        item.add(createLabel("label", item));
                    }
                });
        return group;
    }

    protected AjaxRadio<T> newRadioCell(final RadioGroup<T> group, ListItem<T> item) {
        return new AjaxRadio<T>("radio", item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onAjaxEvent(AjaxRequestTarget target) {
                onRadioSelect(target, group.getModelObject());
            }
        };
    }

    protected Component createLabel(String id, ListItem<T> item) {
        return new Label("label", item.getModel());
    }
}
