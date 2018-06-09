/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.layer.StyleParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class StyleParameterFilterSubform
        extends AbstractParameterFilterSubform<StyleParameterFilter> {

    /** Model Set<String> as a List<String> and optionally add a dummy element at the beginning. */
    static class SetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private final IModel<Set<String>> realModel;

        private final List<String> fakeObject;

        protected final String extra;

        public SetAsListModel(IModel<Set<String>> realModel, String extra) {
            super();
            this.realModel = realModel;
            this.extra = extra;

            Set<String> realObj = realModel.getObject();

            int size;
            if (realObj == null) {
                size = 0;
            } else {
                size = realObj.size();
            }
            if (extra != null) {
                size++;
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();

            fakeObject.clear();

            if (extra != null) fakeObject.add(extra);
            if (realObj != null) fakeObject.addAll(realObj);

            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if (object == null) {
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(extra);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }

    static class LabelledEmptyStringModel implements IModel<String> {

        private static final long serialVersionUID = 7591957769540603345L;

        private final IModel<String> realModel;

        final String label;

        public LabelledEmptyStringModel(IModel<String> realModel, String label) {
            super();
            this.realModel = realModel;
            this.label = label;
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public String getObject() {
            String s = realModel.getObject();
            if (s == null || s.isEmpty()) {
                return label;
            } else {
                return s;
            }
        }

        @Override
        public void setObject(String object) {
            if (label.equals(object)) {
                realModel.setObject("");
            } else {
                realModel.setObject(object);
            }
        }
    }
    /**
     * Model Set<String> as a List<String> and add an option to represent the set being {@literal
     * null}
     */
    static class NullableSetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private final IModel<Set<String>> realModel;

        private final List<String> fakeObject;

        protected final String nullify;

        public NullableSetAsListModel(IModel<Set<String>> realModel, String nullify) {
            super();
            this.realModel = realModel;
            this.nullify = nullify;

            Set<String> realObj = realModel.getObject();

            int size;
            if (realObj == null) {
                size = 1;
            } else {
                size = realObj.size();
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();

            fakeObject.clear();

            if (realObj != null) {
                fakeObject.addAll(realObj);
            } else {
                fakeObject.add(nullify);
            }

            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if (object == null || object.contains(nullify)) {
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(nullify);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public StyleParameterFilterSubform(String id, IModel<StyleParameterFilter> model) {
        super(id, model);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        final Component defaultValue;

        final String allStyles = getLocalizer().getString("allStyles", this);
        final String layerDefault = getLocalizer().getString("layerDefault", this);

        final IModel<List<String>> availableStylesModelDefault =
                new SetAsListModel(
                        new PropertyModel<Set<String>>(getModel(), "layerStyles"), layerDefault);
        final IModel<List<String>> availableStylesModelAllowed =
                new SetAsListModel(
                        new PropertyModel<Set<String>>(getModel(), "layerStyles"), allStyles);
        final IModel<List<String>> selectedStylesModel =
                new NullableSetAsListModel(
                        new PropertyModel<Set<String>>(getModel(), "styles"), allStyles);
        final IModel<String> selectedDefaultModel =
                new LabelledEmptyStringModel(
                        new PropertyModel<String>(getModel(), "realDefault"), layerDefault);

        defaultValue =
                new DropDownChoice<String>(
                        "defaultValue", selectedDefaultModel, availableStylesModelDefault);
        add(defaultValue);

        final CheckBoxMultipleChoice<String> styles =
                new CheckBoxMultipleChoice<String>(
                        "styles", selectedStylesModel, availableStylesModelAllowed);
        styles.setPrefix("<li>");
        styles.setSuffix("</li>");
        add(styles);
    }
}
