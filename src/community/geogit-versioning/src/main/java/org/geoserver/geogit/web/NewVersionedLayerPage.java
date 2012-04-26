/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.geogit.GEOGIT;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.IconWithLabel;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A page listing the resources contained in a store, and whose links will bring the user to a new
 * resource configuration page
 * 
 * @author groldan
 */
@SuppressWarnings("serial")
public class NewVersionedLayerPage extends GeoServerSecuredPage {

    String storeId;

    private NewVersionedLayerPageProvider provider;

    private GeoServerTablePanel<VersionedLayerInfo> layers;

    private WebMarkupContainer layersContainer;

    private WebMarkupContainer selectLayersContainer;

    private WebMarkupContainer selectLayers;

    private Component publishLink;

    private GeoServerDialog dialog;

    private WebMarkupContainer createTypeContainer;

    public NewVersionedLayerPage() {
        this(null);
    }

    public NewVersionedLayerPage(String storeId) {
        this.storeId = storeId;

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());

        // the store selector, used when no store is initially known
        Form selector = new Form("selector");
        selector.add(storesDropDown());
        selector.setVisible(storeId == null);
        add(selector);

        // the layer choosing block
        // visible when in any way a store has been chosen
        selectLayersContainer = new WebMarkupContainer("selectLayersContainer");
        selectLayersContainer.setOutputMarkupId(true);
        add(selectLayersContainer);
        selectLayers = new WebMarkupContainer("selectLayers");
        selectLayers.setVisible(storeId != null);
        selectLayersContainer.add(selectLayers);

        provider = new NewVersionedLayerPageProvider();
        provider.setStoreId(storeId);
        provider.setShowPublished(true);
        layers = new GeoServerTablePanel<VersionedLayerInfo>("layers", provider, true) {

            @SuppressWarnings("rawtypes")
            @Override
            protected CheckBox selectOneCheckbox(Item item) {
                VersionedLayerInfo info = (VersionedLayerInfo) item.getModelObject();
                boolean enabled = !(info.isReadOnly() || info.isPublished());
                CheckBox itemCheck;
                if (enabled) {
                    itemCheck = super.selectOneCheckbox(item);
                } else {
                    itemCheck = new CheckBox("selectItem", new Model<Boolean>(Boolean.FALSE));
                    itemCheck.setEnabled(enabled);
                }
                return itemCheck;
            }

            @Override
            public List<VersionedLayerInfo> getSelection() {
                List<VersionedLayerInfo> selection = new ArrayList<VersionedLayerInfo>(
                        super.getSelection());
                for (ListIterator<VersionedLayerInfo> li = selection.listIterator(); li.hasNext();) {
                    VersionedLayerInfo next = li.next();
                    if (next.isPublished() || next.isReadOnly()) {
                        li.remove();
                    }
                }
                return selection;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                NewVersionedLayerPage.this.publishLink.setEnabled(getSelection().size() > 0);
                target.addComponent(NewVersionedLayerPage.this.publishLink);
            }

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<VersionedLayerInfo> property) {

                final VersionedLayerInfo versionedType = (VersionedLayerInfo) itemModel.getObject();

                if (property == NewVersionedLayerPageProvider.PUBLISHED) {
                    final CatalogIconFactory icons = CatalogIconFactory.get();
                    final ResourceReference icon;
                    final String labelKey;
                    if (versionedType.isPublished()) {
                        icon = icons.getEnabledIcon();
                        labelKey = "NewVersionedLayerPage.alreadyPublished";
                    } else if (versionedType.isReadOnly()) {
                        icon = new ResourceReference(NewVersionedLayerPage.class, "prohibited.gif");
                        labelKey = "NewVersionedLayerPage.readOnly";
                    } else {
                        icon = icons.getDisabledIcon();
                        labelKey = "NewVersionedLayerPage.unpublished";
                    }
                    IModel<String> label = new ResourceModel(labelKey);
                    IconWithLabel iconLabel = new IconWithLabel(id, icon, label);
                    return iconLabel;
                }
                if (property == NewVersionedLayerPageProvider.GEOMTYPE) {
                    final CatalogIconFactory icons = CatalogIconFactory.get();
                    final Class<? extends Geometry> geometryType = versionedType.getGeometryType();
                    final ResourceReference icon;
                    if (geometryType == null) {
                        icon = CatalogIconFactory.UNKNOWN_ICON;
                    } else {
                        icon = icons.getVectorIcon(geometryType);
                    }
                    Fragment f = new Fragment(id, "iconFragment", NewVersionedLayerPage.this);
                    f.add(new Image("layerIcon", icon));
                    return f;
                }
                if (property == NewVersionedLayerPageProvider.NAME) {
                    return new Label(id, property.getModel(itemModel));
                }

                throw new IllegalArgumentException("Don't know of property " + property.getName());

            }

        };
        layers.setFilterVisible(true);

        selectLayers.add(layers);

        createTypeContainer = new WebMarkupContainer("createTypeContainer");
        createTypeContainer.setVisible(false);
        selectLayersContainer.add(createTypeContainer);

        // case where the store is selected, or we have just created new one
        if (storeId != null) {
            DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
            updateSpecialFunctionPanels(store);
        }
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        publishLink = new AjaxLink<List<VersionedLayerInfo>>("publishLink") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final List<VersionedLayerInfo> selection = layers.getSelection();
                DialogDelegate delegate = new DialogDelegate() {

                    boolean submitted = false;

                    @Override
                    protected boolean onSubmit(final AjaxRequestTarget target, Component contents) {
                        try {
                            publishLayers(selection);
                            submitted = true;
                        } catch (Exception e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (submitted) {
                            NewVersionedLayerPage.this.setResponsePage(VersionedLayersPage.class);
                        }
                    }

                    @Override
                    protected Component getContents(String id) {
                        StringBuilder names = new StringBuilder();
                        for (VersionedLayerInfo l : selection) {
                            names.append(l.getName()).append("   ");
                        }
                        return new Label(id,
                                "Confirm publishing the following FeatureTypes as versioned?:  "
                                        + names);
                    }

                };
                dialog.showOkCancel(target, delegate);
            }
        };
        publishLink.setOutputMarkupId(true);
        header.add(publishLink);
        publishLink.setEnabled(false);

        return header;
    }

    private void publishLayers(List<VersionedLayerInfo> selection) throws Exception {
        final GEOGIT geogitFacade = GEOGIT.get();
        Name featureTypeName;
        for (VersionedLayerInfo info : selection) {
            featureTypeName = info.getName();
            geogitFacade.initialize(featureTypeName);
        }
    }

    private DropDownChoice storesDropDown() {
        final DropDownChoice stores = new DropDownChoice("storesDropDown", new Model(),
                new StoreListModel(), new StoreListChoiceRenderer());
        stores.setOutputMarkupId(true);
        stores.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (stores.getModelObject() != null) {
                    DataStoreInfo store = (DataStoreInfo) stores.getModelObject();
                    NewVersionedLayerPage.this.storeId = store.getId();
                    provider.setStoreId(store.getId());
                    selectLayers.setVisible(true);

                    // make sure we can actually list the contents, it may happen
                    // the store is actually unreachable, in that case we
                    // want to display an error message
                    try {
                        provider.getItems();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error retrieving layers for the specified store",
                                e);
                        error(e.getMessage());
                        selectLayers.setVisible(false);
                    }

                    updateSpecialFunctionPanels(store);

                } else {
                    selectLayers.setVisible(false);
                    createTypeContainer.setVisible(false);
                }
                target.addComponent(selectLayersContainer);
                target.addComponent(feedbackPanel);

            }

        });
        return stores;
    }

    void updateSpecialFunctionPanels(DataStoreInfo store) {
        createTypeContainer.setVisible(true);
    }

    /**
     * Returns the storeId provided during construction, or the one pointed by the drop down if none
     * was provided during construction
     * 
     * @return
     */
    String getSelectedStoreId() {
        // the provider is always up to date
        return provider.getStoreId();
    }

    final class StoreListModel extends LoadableDetachableModel<List<DataStoreInfo>> {
        @Override
        protected List<DataStoreInfo> load() {
            List<DataStoreInfo> stores = new ArrayList<DataStoreInfo>(getCatalog().getDataStores());
            for (Iterator<DataStoreInfo> it = stores.iterator(); it.hasNext();) {
                if (!it.next().isEnabled()) {
                    it.remove();
                }
            }
            Collections.sort(stores, new Comparator<StoreInfo>() {
                public int compare(StoreInfo o1, StoreInfo o2) {
                    if (o1.getWorkspace().equals(o2.getWorkspace())) {
                        return o1.getName().compareTo(o2.getName());
                    }
                    return o1.getWorkspace().getName().compareTo(o2.getWorkspace().getName());
                }
            });
            return stores;
        }
    }

    static final class StoreListChoiceRenderer implements IChoiceRenderer<DataStoreInfo> {

        /**
         * @see org.apache.wicket.markup.html.form.IChoiceRenderer#getDisplayValue(java.lang.Object)
         */
        public Object getDisplayValue(DataStoreInfo info) {
            return new StringBuilder(info.getWorkspace().getName()).append(':')
                    .append(info.getName()).toString();
        }

        /**
         * @return {@link DataStoreInfo#getId()}
         * @see org.apache.wicket.markup.html.form.IChoiceRenderer#getIdValue(java.lang.Object, int)
         */
        public String getIdValue(DataStoreInfo store, int index) {
            return store.getId();
        }

    }

}
