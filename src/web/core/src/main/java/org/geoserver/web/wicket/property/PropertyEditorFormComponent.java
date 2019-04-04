/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.ValidationError;
import org.springframework.util.StringUtils;

/**
 * Form component panel for editing {@link Properties} property.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PropertyEditorFormComponent extends FormComponentPanel<Properties> {

    private static final long serialVersionUID = -1960584178014140068L;
    ListView<Tuple> listView;
    List<Tuple> invalidTuples = null;

    public PropertyEditorFormComponent(String id) {
        super(id);
        init();
    }

    public PropertyEditorFormComponent(String id, IModel<Properties> model) {
        super(id, model);
        init();
    }

    void init() {
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        listView =
                new ListView<Tuple>("list") {
                    private static final long serialVersionUID = -7250612551499360015L;

                    @Override
                    protected void populateItem(ListItem<Tuple> item) {
                        item.setModel(new CompoundPropertyModel<Tuple>(item.getModelObject()));
                        item.add(
                                new TextField<String>("key")
                                        .add(
                                                new AjaxFormComponentUpdatingBehavior("blur") {
                                                    private static final long serialVersionUID =
                                                            5416373713193788662L;

                                                    @Override
                                                    protected void onUpdate(
                                                            AjaxRequestTarget target) {}
                                                }));
                        item.add(
                                new TextField<String>("value")
                                        .add(
                                                new AjaxFormComponentUpdatingBehavior("blur") {
                                                    private static final long serialVersionUID =
                                                            -8679502120189597358L;

                                                    @Override
                                                    protected void onUpdate(
                                                            AjaxRequestTarget target) {}
                                                }));
                        item.add(
                                new AjaxLink<Tuple>("remove", item.getModel()) {
                                    private static final long serialVersionUID =
                                            3201264868229144613L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        List<Tuple> l = listView.getModelObject();
                                        l.remove(getModelObject());
                                        target.add(container);
                                    }
                                });
                    }
                };
        // listView.setReuseItems(true);
        container.add(listView);

        add(
                new AjaxLink<Void>("add") {
                    private static final long serialVersionUID = 4741595573705562351L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        listView.getModelObject().add(new Tuple());
                        target.add(container);
                    }
                });
    }

    List<Tuple> tuples() {

        if (invalidTuples != null) return invalidTuples;

        Properties props = getModelObject();
        if (props == null) {
            props = new Properties();
        }

        List<Tuple> tuples = new ArrayList<Tuple>();
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            tuples.add(new Tuple((String) e.getKey(), (String) e.getValue()));
        }

        Collections.sort(tuples);
        return tuples;
    }

    @Override
    protected void onBeforeRender() {
        listView.setModel(new ListModel<Tuple>(tuples()));
        super.onBeforeRender();
    }

    @Override
    public void convertInput() {
        for (Iterator<?> it = listView.iterator(); it.hasNext(); ) {
            ListItem<?> item = (ListItem<?>) it.next();
            ((FormComponent<?>) item.get("key")).updateModel();
            ((FormComponent<?>) item.get("value")).updateModel();
        }

        Properties props = getModelObject();
        if (props == null) {
            props = new Properties();
        }

        props.clear();
        for (Tuple t : listView.getModelObject()) {
            props.put(t.getKey(), t.getValue());
        }

        setConvertedInput(props);
    }

    @Override
    public void validate() {
        invalidTuples = null;
        for (Tuple t : listView.getModelObject()) {
            if (StringUtils.hasLength(t.getKey()) == false) {
                invalidTuples = listView.getModelObject();
                error(new ValidationError("KeyRequired").addKey("KeyRequired"));
                return;
            }
            if (StringUtils.hasLength(t.getValue()) == false) {
                invalidTuples = listView.getModelObject();
                error(new ValidationError("ValueRequired").addKey("ValueRequired"));
                return;
            }
        }

        super.validate();
    }

    static class Tuple implements Serializable, Comparable<Tuple> {
        private static final long serialVersionUID = 1L;

        private String key;
        private String value;

        public Tuple() {}

        public Tuple(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key != null ? key.compareTo(o.key) : o.key == null ? 0 : -1;
        }
    }
}
