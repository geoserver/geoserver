/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

/** Component editing a list of {@link AttributeTypeInfo} */
class LinkInfoEditor extends FormComponentPanel<List<LinkInfo>> {

    static final Logger LOGGER = Logging.getLogger(LinkInfoEditor.class);

    // editor panel constants
    private static final Property<LinkInfo> REL = new GeoServerDataProvider.BeanProperty<>("rel");
    private static final Property<LinkInfo> TYPE = new GeoServerDataProvider.BeanProperty<>("type");
    private static final Property<LinkInfo> HREF = new GeoServerDataProvider.BeanProperty<>("href");

    private static final Property<LinkInfo> TITLE =
            new GeoServerDataProvider.BeanProperty<>("title");

    private static final Property<LinkInfo> SERVICE =
            new GeoServerDataProvider.BeanProperty<>("service");

    private static final Property<LinkInfo> REMOVE =
            new GeoServerDataProvider.PropertyPlaceholder<>("remove");

    private final ReorderableTablePanel<LinkInfo> table;
    private final Component parent;

    private static final LoadableDetachableModel<List<Property<LinkInfo>>> propertiesModel =
            new LoadableDetachableModel<>() {
                @Override
                protected List<Property<LinkInfo>> load() {
                    return Arrays.asList(REL, TYPE, HREF, TITLE, SERVICE, REMOVE);
                }
            };

    /**
     * Constructs a {@link LinkInfoEditor} given the component id, a list of {@link LinkInfo} to be
     * edited, and a parent component for AJAX refresh.
     */
    public LinkInfoEditor(String id, IModel<List<LinkInfo>> links, Component parent) {
        super(id, links);
        this.parent = parent;

        add(new AddLinkLink());

        table = new EditorTable(links.getObject(), this);
        table.setItemReuseStrategy(new DefaultItemReuseStrategy());
        table.setPageable(false);
        table.setFilterable(false);
        add(table);
    }

    @Override
    public void convertInput() {
        List<LinkInfo> links = table.getItems();
        if (links != null && !links.isEmpty()) {
            setConvertedInput(links);
        } else {
            setConvertedInput(null);
        }
    }

    private class EditorTable extends ReorderableTablePanel<LinkInfo> {
        private final Component targetComponent;

        public EditorTable(List<LinkInfo> attributes, Component targetComponent) {
            super("table", LinkInfo.class, attributes, LinkInfoEditor.propertiesModel);
            this.targetComponent = targetComponent;
        }

        /**
         * Note, all editors returned are without validation and with a behavior that updates the
         * server side whenever the editor loses focus, to make sure drag/up/down don't end up
         * resetting the client side content (as they happen server side, with a subsequent AJAX
         * redraw on the client).
         */
        @Override
        @SuppressWarnings("unchecked")
        protected Component getComponentForProperty(
                String id, IModel<LinkInfo> itemModel, Property<LinkInfo> property) {
            IModel model = property.getModel(itemModel);
            if (property == SERVICE) {
                Fragment f = new Fragment(id, "service", getParent());
                DropDownChoice<String> service =
                        new DropDownChoice<>("service", model, getAPIServices());
                service.add(new UpdateModelBehavior());
                f.add(service);
                return f;
            } else if (property == REL || property == TYPE) {
                Fragment f = new Fragment(id, "text", getParent());
                TextField<String> nameField = new TextField<>("text", model);
                nameField.add(new UpdateModelBehavior());
                nameField.setRequired(property != SERVICE);
                f.add(nameField);
                return f;
            } else if (property == HREF || property == TITLE) {
                Fragment f = new Fragment(id, "area", getParent());
                TextArea<String> source = new TextArea<>("area", model);
                source.add(new UpdateModelBehavior());
                source.setRequired(property != TITLE);
                f.add(source);
                return f;
            } else if (property == REMOVE) {
                final LinkInfo entry = itemModel.getObject();
                PackageResourceReference icon =
                        new PackageResourceReference(getClass(), "../img/icons/silk/delete.png");
                ImageAjaxLink<Object> link =
                        new ImageAjaxLink<>(id, icon) {

                            @Override
                            protected void onClick(AjaxRequestTarget target) {
                                getItems().remove(entry);
                                target.add(targetComponent);
                            }
                        };
                return link;
            }

            return null;
        }
    }

    private List<String> getAPIServices() {
        ApplicationContext ctx = ((GeoServerApplication) getApplication()).getApplicationContext();
        Map<String, Object> beans = ctx.getBeansWithAnnotation(APIService.class);
        return beans.values().stream()
                .map(s -> APIDispatcher.getApiServiceAnnotation(s.getClass()))
                .filter(a -> a != null && a.core())
                .map(a -> a.service())
                .sorted()
                .collect(Collectors.toList());
    }

    private static class UpdateModelBehavior extends AjaxFormComponentUpdatingBehavior {

        public UpdateModelBehavior() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            // nothing to do, the mere presence is enough to update the server side model
            // before up/down/drag actions are performed
        }
    }

    private class AddLinkLink extends AjaxLink<Object> {

        public AddLinkLink() {
            super("addLink");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            table.getItems().add(new LinkInfoImpl());
            target.add(parent);
        }
    }
}
