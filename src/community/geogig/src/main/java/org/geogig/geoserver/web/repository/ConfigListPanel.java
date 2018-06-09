/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class ConfigListPanel extends GeoServerTablePanel<ConfigEntry> {

    private static final long serialVersionUID = 4583765525525250302L;

    private static final PackageResourceReference REMOVE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    private final ModalWindow popupWindow;

    private final GeoServerDialog dialog;

    private final ConfigProvider provider;

    private final FeedbackPanel pingFeedbackPanel;

    public ConfigListPanel(final String id, final Map<String, String> config) {
        super(id, new ConfigProvider(config), false);
        super.setFilterable(false);
        this.provider = (ConfigProvider) super.getDataProvider();
        this.setOutputMarkupId(true);
        // set the reuse strategy, otherwise the list does not get updated when the popup window
        // closes
        this.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        add(dialog = new GeoServerDialog("dialog"));
        add(pingFeedbackPanel = new FeedbackPanel("feedback"));
        pingFeedbackPanel.setOutputMarkupId(true);
    }

    public void add(ConfigEntry config) {
        this.provider.getItems().add(config);
    }

    public Iterable<ConfigEntry> getConfigs() {
        return provider.getItems();
    }

    @Override
    protected Component getComponentForProperty(
            String id, IModel<ConfigEntry> itemModel, Property<ConfigEntry> property) {

        if (property == ConfigProvider.NAME) {
            String key = (String) ConfigProvider.NAME.getModel(itemModel).getObject();
            if (ConfigEntry.isRestricted(key)) {
                return new Label(id, key);
            }
            return nameLink(id, itemModel);
        } else if (property == ConfigProvider.VALUE) {
            String value = (String) ConfigProvider.VALUE.getModel(itemModel).getObject();
            Label label = new Label(id, value);
            return label;
        } else if (property == ConfigProvider.REMOVELINK) {
            String key = (String) ConfigProvider.NAME.getModel(itemModel).getObject();
            if (ConfigEntry.isRestricted(key)) {
                return new Label(id, "-");
            }
            return removeLink(id, itemModel);
        }
        return null;
    }

    private Component nameLink(String id, IModel<ConfigEntry> itemModel) {
        IModel<?> nameModel = ConfigProvider.NAME.getModel(itemModel);

        SimpleAjaxLink<ConfigEntry> link =
                new SimpleAjaxLink<ConfigEntry>(id, itemModel, nameModel) {

                    private static final long serialVersionUID = 3999079486003240692L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<ConfigEntry> model = this.getModel();
                        ConfigListPanel table = ConfigListPanel.this;
                        ConfigEditPanel editPanel =
                                new ConfigEditPanel(
                                        popupWindow.getContentId(), model, popupWindow, table);

                        popupWindow.setContent(editPanel);
                        popupWindow.setTitle(new ResourceModel("ConfigEditPanel.title"));
                        popupWindow.show(target);
                    }
                };
        return link;
    }

    protected Component removeLink(final String id, final IModel<ConfigEntry> itemModel) {

        return new ImageAjaxLink(id, REMOVE_ICON) {

            private static final long serialVersionUID = 4251171731728162708L;

            private final IModel<ConfigEntry> model = itemModel;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GeoServerDialog dialog = ConfigListPanel.this.dialog;
                dialog.setTitle(
                        new ParamResourceModel("ConfigListPanel.confirmRemoval.title", this));

                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = -450822090965263894L;

                            @Override
                            protected Component getContents(String id) {
                                return new ConfirmRemovePanel(id, model);
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                boolean closeConfirmDialog = true;

                                final ConfigEntry config = model.getObject();
                                List<ConfigEntry> items = ConfigListPanel.this.provider.getItems();
                                items.remove(config);
                                target.add(ConfigListPanel.this);
                                return closeConfirmDialog;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                target.add(ConfigListPanel.this);
                            }
                        });
            }
        };
    }

    static class ConfirmRemovePanel extends Panel {

        private static final long serialVersionUID = 653769682579422516L;

        public ConfirmRemovePanel(String id, IModel<ConfigEntry> config) {
            super(id);

            add(
                    new Label(
                            "aboutRemoveMsg",
                            new ParamResourceModel(
                                    "ConfigListPanel$ConfirmRemovePanel.aboutRemove",
                                    this,
                                    config.getObject().getName())));
        }
    }

    static class ConfigProvider extends GeoServerDataProvider<ConfigEntry> {

        private static final long serialVersionUID = 4883560661021761394L;

        static final Property<ConfigEntry> NAME = new BeanProperty<>("name", "name");

        static final Property<ConfigEntry> VALUE = new BeanProperty<>("value", "value");

        static final Property<ConfigEntry> REMOVELINK =
                new AbstractProperty<ConfigEntry>("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getPropertyValue(ConfigEntry item) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public boolean isSearchable() {
                        return false;
                    }
                };

        final List<Property<ConfigEntry>> PROPERTIES = Arrays.asList(NAME, VALUE, REMOVELINK);

        private final List<ConfigEntry> config;

        public ConfigProvider(final Map<String, String> config) {
            this.config = ConfigEntry.fromConfig(config);
        }

        @Override
        protected List<ConfigEntry> getItems() {
            return config;
        }

        @Override
        protected List<Property<ConfigEntry>> getProperties() {
            return PROPERTIES;
        }

        @Override
        protected Comparator<ConfigEntry> getComparator(SortParam sort) {
            return super.getComparator(sort);
        }

        @Override
        public IModel<ConfigEntry> newModel(ConfigEntry object) {
            return new Model<>(object);
        }
    }
}
