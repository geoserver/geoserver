/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.DELETE;
import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.GROUP_CHANGED;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.web.RepositoryEditPage;
import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class RepositoriesListPanel extends GeoServerTablePanel<RepositoryInfo> {

    private static final long serialVersionUID = 5957961031378924960L;

    private static final PackageResourceReference REMOVE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    private final ModalWindow popupWindow;

    private final GeoServerDialog dialog;

    public RepositoriesListPanel(final String id) {
        super(id, new RepositoryProvider(), false);

        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        add(dialog = new GeoServerDialog("dialog"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected Component getComponentForProperty(
            String id, IModel<RepositoryInfo> itemModel, Property<RepositoryInfo> property) {

        if (property == RepositoryProvider.NAME) {
            return nameLink(id, itemModel);
        } else if (property == RepositoryProvider.LOCATION) {
            String location =
                    RepositoryProvider.LOCATION.getModel(itemModel).getObject().toString();
            Label label = new Label(id, location);
            // label.add(new SimpleAttributeModifier("style", "word-wrap:break-word;"));
            return label;
        } else if (property == RepositoryProvider.REMOVELINK) {
            return removeLink(id, itemModel);
        }
        return null;
    }

    private Component nameLink(String id, IModel<RepositoryInfo> itemModel) {
        @SuppressWarnings("unchecked")
        IModel<?> nameModel = RepositoryProvider.NAME.getModel(itemModel);

        SimpleAjaxLink<RepositoryInfo> link =
                new SimpleAjaxLink<RepositoryInfo>(id, itemModel, nameModel) {
                    private static final long serialVersionUID = -18292070541084372L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<RepositoryInfo> model = getModel();
                        RepositoriesListPanel.this.setResponsePage(new RepositoryEditPage(model));
                    }
                };
        return link;
    }

    protected Component removeLink(final String id, final IModel<RepositoryInfo> itemModel) {

        return new ImageAjaxLink(id, REMOVE_ICON) {

            private static final long serialVersionUID = -3061812114487970427L;

            private final IModel<RepositoryInfo> model = itemModel;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GeoServerDialog dialog = RepositoriesListPanel.this.dialog;
                dialog.setTitle(
                        new ParamResourceModel("RepositoriesListPanel.confirmRemoval.title", this));

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

                                final String repoId = model.getObject().getId();
                                RepositoryManager.get().delete(repoId);

                                return closeConfirmDialog;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                target.add(RepositoriesListPanel.this);
                            }
                        });
            }
        };
    }

    static class ConfirmRemovePanel extends Panel {

        private static final long serialVersionUID = 653769682579422516L;

        public ConfirmRemovePanel(String id, IModel<RepositoryInfo> repo) {
            super(id);

            final String repoName = repo.getObject().getRepoName();
            add(
                    new Label(
                            "aboutRemoveMsg",
                            new ParamResourceModel(
                                    "RepositoriesListPanel$ConfirmRemovePanel.aboutRemove",
                                    this,
                                    repoName)));

            final String repoId = repo.getObject().getId();
            final List<? extends CatalogInfo> stores;
            stores = RepositoryManager.get().findDataStores(repoId);

            // collect the objects that will be removed (besides the roots)
            Catalog catalog = GeoServerApplication.get().getCatalog();

            CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

            for (CatalogInfo info : stores) {
                info.accept(visitor);
            }
            // visitor.removeAll(stores);

            // removed objects root (we show it if any removed object is on the list)
            WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
            List<CatalogInfo> cascaded = visitor.getObjects(CatalogInfo.class, DELETE);
            // remove the resources, they are cascaded, but won't be show in the UI
            for (Iterator<CatalogInfo> it = cascaded.iterator(); it.hasNext(); ) {
                CatalogInfo catalogInfo = it.next();
                if (catalogInfo instanceof ResourceInfo) {
                    it.remove();
                }
            }
            removed.setVisible(!cascaded.isEmpty());
            add(removed);

            // removed stores
            WebMarkupContainer str = new WebMarkupContainer("storesRemoved");
            removed.add(str);
            str.setVisible(!stores.isEmpty());
            str.add(new Label("stores", names(stores)));

            // removed layers
            WebMarkupContainer lar = new WebMarkupContainer("layersRemoved");
            removed.add(lar);
            List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, DELETE);
            if (layers.isEmpty()) lar.setVisible(false);
            lar.add(new Label("layers", names(layers)));

            // modified objects root (we show it if any modified object is on the list)
            WebMarkupContainer modified = new WebMarkupContainer("modifiedObjects");
            modified.setVisible(visitor.getObjects(null, GROUP_CHANGED).size() > 0);
            add(modified);

            // groups modified
            WebMarkupContainer grm = new WebMarkupContainer("groupsModified");
            modified.add(grm);
            List<LayerGroupInfo> groups = visitor.getObjects(LayerGroupInfo.class, GROUP_CHANGED);
            grm.setVisible(!groups.isEmpty());
            grm.add(new Label("groups", names(groups)));
        }

        String names(List<? extends CatalogInfo> objects) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < objects.size(); i++) {
                sb.append(name(objects.get(i)));
                if (i < (objects.size() - 1)) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }

        String name(Object object) {
            try {
                return (String) BeanUtils.getProperty(object, "name");
            } catch (Exception e) {
                throw new RuntimeException(
                        "A catalog object that does not have "
                                + "a 'name' property has been used, this is unexpected",
                        e);
            }
        }
    }

    static class RepositoryProvider extends GeoServerDataProvider<RepositoryInfo> {

        private static final long serialVersionUID = 4883560661021761394L;

        static final Property<RepositoryInfo> NAME = new BeanProperty<>("name", "repoName");

        static final Property<RepositoryInfo> LOCATION =
                new BeanProperty<>("location", "maskedLocation");

        static final Property<RepositoryInfo> REMOVELINK =
                new AbstractProperty<RepositoryInfo>("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getPropertyValue(RepositoryInfo item) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public boolean isSearchable() {
                        return false;
                    }
                };

        final List<Property<RepositoryInfo>> PROPERTIES = Arrays.asList(NAME, LOCATION, REMOVELINK);

        public RepositoryProvider() {}

        @Override
        protected List<RepositoryInfo> getItems() {
            return RepositoryManager.get().getAll();
        }

        @Override
        protected List<Property<RepositoryInfo>> getProperties() {
            return PROPERTIES;
        }

        @Override
        protected Comparator<RepositoryInfo> getComparator(SortParam sort) {
            return super.getComparator(sort);
        }

        @Override
        public IModel<RepositoryInfo> newModel(RepositoryInfo object) {
            return new RepositoryInfoDetachableModel(object);
        }
    }
}
