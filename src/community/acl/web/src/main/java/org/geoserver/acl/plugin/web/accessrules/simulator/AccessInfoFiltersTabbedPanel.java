/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.simulator;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.geolatte.geom.Geometry;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerAttribute.AccessType;
import org.geoserver.acl.plugin.web.components.GeometryWktTextArea;
import org.geoserver.acl.plugin.web.support.SerializableFunction;
import org.geoserver.acl.plugin.web.support.SerializablePredicate;

@SuppressWarnings("serial")
public class AccessInfoFiltersTabbedPanel extends Panel {

    private WebMarkupContainer tabsPanel;

    private RadioGroup<String> filtertabset;
    private Radio<String> wktareaTab;
    private Radio<String> cqlreadTab;
    private Radio<String> cqlwriteTab;
    private Radio<String> clipareaTab;
    private Radio<String> stylesTab;
    private Radio<String> attributesTab;

    public AccessInfoFiltersTabbedPanel(String id, IModel<AccessInfo> model) {
        super(id, new CompoundPropertyModel<>(model));
        add(tabsPanel = tabsPanel());

        tabsPanel.add(intersectArea());
        tabsPanel.add(clipArea());

        tabsPanel.add(cqlFilterRead());
        tabsPanel.add(cqlFilterWrite());

        tabsPanel.add(defaultStyle());
        tabsPanel.add(allowedStyles());

        tabsPanel.add(attributesRO());
        tabsPanel.add(attributesRW());
        tabsPanel.add(attributesHidden());
        initVisibility();
    }

    public @Override void onModelChanged() {
        initVisibility();
    }

    private void initVisibility() {
        AccessInfo access = model().getObject();
        wktareaTab.setEnabled(notEmpty(access.getIntersectArea()));
        clipareaTab.setEnabled(notEmpty(access.getClipArea()));
        cqlreadTab.setEnabled(notEmpty(access.getCqlFilterRead()));
        cqlwriteTab.setEnabled(notEmpty(access.getCqlFilterWrite()));
        stylesTab.setEnabled(notEmpty(access.getDefaultStyle()) || notEmpty(access.getAllowedStyles()));
        attributesTab.setEnabled(notEmpty(access.getAttributes()));

        selectFirstEnabled(wktareaTab, clipareaTab, cqlreadTab, cqlwriteTab, stylesTab, attributesTab);
    }

    @SafeVarargs
    private void selectFirstEnabled(Radio<String>... tabs) {
        boolean found = false;
        for (Radio<String> tab : tabs) {
            if (!found && tab.isEnabled()) {
                tab.add(AttributeModifier.replace("checked", "true"));
                found = true;
            } else {
                tab.add(AttributeModifier.remove("checked"));
            }
        }
    }

    private boolean notEmpty(Object value) {
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return value != null;
    }

    private WebMarkupContainer tabsPanel() {
        filtertabset = new RadioGroup<>("filtertabset", Model.of());

        wktareaTab = new Radio<>("wktareaTab", Model.of("intersectArea"), filtertabset);
        clipareaTab = new Radio<>("clipAreaTab", Model.of("cliparea"), filtertabset);
        cqlreadTab = new Radio<>("cqlreadTab", Model.of("cqlr"), filtertabset);
        cqlwriteTab = new Radio<>("cqlwriteTab", Model.of("cqlw"), filtertabset);
        stylesTab = new Radio<>("stylesTab", Model.of("styles"), filtertabset);
        attributesTab = new Radio<>("attributesTab", Model.of("attributes"), filtertabset);
        filtertabset.add(wktareaTab, clipareaTab, cqlreadTab, cqlwriteTab, stylesTab, attributesTab);

        WebMarkupContainer container = new WebMarkupContainer("tabs");
        container.setOutputMarkupPlaceholderTag(true);
        container.add(filtertabset);
        return container;
    }

    @SuppressWarnings("rawtypes")
    private GeometryWktTextArea<Geometry> intersectArea() {
        GeometryWktTextArea<Geometry> area =
                new GeometryWktTextArea<>("intersectArea", Geometry.class, model().bind("intersectArea"));
        area.setEnabled(false);
        return area;
    }

    @SuppressWarnings("rawtypes")
    private GeometryWktTextArea<Geometry> clipArea() {
        GeometryWktTextArea<Geometry> area =
                new GeometryWktTextArea<>("clipArea", Geometry.class, model().bind("clipArea"));
        area.setEnabled(false);
        return area;
    }

    private Component defaultStyle() {
        return new Label("defaultStyle", model().bind("defaultStyle"));
    }

    private Component allowedStyles() {
        return CollectionLabel.ofStrings("allowedStyles", model().bind("allowedStyles"));
    }

    private Component attributesRO() {
        return attributesLabel("attributes_ro", AccessType.READONLY);
    }

    private Component attributesRW() {
        return attributesLabel("attributes_rw", AccessType.READWRITE);
    }

    private Component attributesHidden() {
        return attributesLabel("attributes_hidden", AccessType.NONE);
    }

    private Component attributesLabel(final String id, final AccessType type) {
        IModel<List<LayerAttribute>> model = model().bind("attributes");
        SerializablePredicate<LayerAttribute> filter = att -> att.getAccess() == type;
        return new CollectionLabel<>(id, model, LayerAttribute::getName, filter);
    }

    private static class CollectionLabel<T> extends Label {

        private SerializableFunction<T, String> converter;
        private SerializablePredicate<T> filter;

        public static CollectionLabel<String> ofStrings(String id, IModel<? extends Collection<String>> model) {
            return new CollectionLabel<>(id, model, v -> v);
        }

        public CollectionLabel(
                String id, IModel<? extends Collection<T>> model, SerializableFunction<T, String> converter) {
            this(id, model, converter, c -> true);
        }

        public CollectionLabel(
                String id,
                IModel<? extends Collection<T>> model,
                SerializableFunction<T, String> converter,
                SerializablePredicate<T> filter) {
            super(id, model);
            this.converter = converter;
            this.filter = filter;
        }

        protected @Override IConverter<?> createConverter(Class<?> type) {
            if (!Collection.class.isAssignableFrom(type)) return null;
            return new IConverter<Collection<T>>() {
                public @Override Collection<T> convertToObject(String value, Locale locale) throws ConversionException {
                    throw new UnsupportedOperationException();
                }

                public @Override String convertToString(Collection<T> value, Locale locale) {
                    return value.stream()
                            .filter(filter)
                            .map(converter)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.joining(", "));
                }
            };
        }
    }

    private Component cqlFilterRead() {
        TextArea<String> filter = new TextArea<>("cqlFilterRead", model().bind("cqlFilterRead"));
        filter.setEnabled(false);
        return filter;
    }

    private Component cqlFilterWrite() {
        TextArea<String> filter = new TextArea<>("cqlFilterWrite", model().bind("cqlFilterWrite"));
        filter.setEnabled(false);
        return filter;
    }

    @SuppressWarnings("unchecked")
    private CompoundPropertyModel<AccessInfo> model() {
        return (CompoundPropertyModel<AccessInfo>) super.getDefaultModel();
    }
}
