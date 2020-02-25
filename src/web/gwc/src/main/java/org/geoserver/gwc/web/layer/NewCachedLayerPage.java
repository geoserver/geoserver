/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.geoserver.gwc.web.layer.UnconfiguredCachedLayersProvider.ENABLED;
import static org.geoserver.gwc.web.layer.UnconfiguredCachedLayersProvider.NAME;
import static org.geoserver.gwc.web.layer.UnconfiguredCachedLayersProvider.TYPE;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.layer.TileLayer;

/**
 * A page that lists all {@link LayerInfo} and {@link LayerGroupInfo} that don't already have an
 * associated {@link GeoServerTileLayer} and allows to create a tile layer for each of one or in
 * bulk using the default settings.
 *
 * @author groldan
 */
public class NewCachedLayerPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 6458510742445385219L;

    private UnconfiguredCachedLayersProvider provider = new UnconfiguredCachedLayersProvider();

    private GeoServerTablePanel<TileLayer> table;

    private GeoServerDialog dialog;

    private BulkCachedLayerConfigurationLink bulkConfig;

    private Label insaneDefaultsMessage;

    public NewCachedLayerPage() {

        table =
                new GeoServerTablePanel<TileLayer>("table", provider, true) {

                    private static final long serialVersionUID = -5260899839139961722L;

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<TileLayer> itemModel, Property<TileLayer> property) {

                        if (property == TYPE) {
                            Fragment f = new Fragment(id, "iconFragment", NewCachedLayerPage.this);
                            TileLayer layer = (TileLayer) itemModel.getObject();
                            PackageResourceReference layerIcon =
                                    GWCIconFactory.getSpecificLayerIcon(layer);
                            f.add(new Image("layerIcon", layerIcon));
                            return f;
                        } else if (property == NAME) {
                            return nameLink(id, itemModel);
                        } else if (property == ENABLED) {
                            TileLayer layerInfo = (TileLayer) itemModel.getObject();
                            boolean enabled = layerInfo.isEnabled();
                            PackageResourceReference icon;
                            if (enabled) {
                                icon = GWCIconFactory.getEnabledIcon();
                            } else {
                                icon = GWCIconFactory.getDisabledIcon();
                            }
                            Fragment f = new Fragment(id, "iconFragment", NewCachedLayerPage.this);
                            f.add(new Image("layerIcon", icon));
                            return f;
                        }
                        throw new IllegalArgumentException(
                                "Don't know a property named " + property.getName());
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        updateBulkConfigLink();
                        target.add(bulkConfig);
                    }
                };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(360);
        dialog.setInitialHeight(180);
        setHeaderPanel(headerPanel());
    }

    private void updateBulkConfigLink() {
        int numSelected = table.getNumSelected();
        GWCConfig defaults = GWC.get().getConfig();
        boolean defaultsSane = defaults.isSane();

        bulkConfig.setEnabled(defaultsSane && numSelected > 0);

        insaneDefaultsMessage.setVisible(!defaultsSane);
    }

    private Component nameLink(String id, IModel<TileLayer> itemModel) {

        Component link;

        link = new ConfigureCachedLayerAjaxLink(id, itemModel, NewCachedLayerPage.class);

        return link;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(bulkConfig = new BulkCachedLayerConfigurationLink("bulkConfig"));
        bulkConfig.setOutputMarkupId(true);

        header.add(
                insaneDefaultsMessage =
                        new Label(
                                "insaneDefaultsMessage",
                                new ResourceModel("bulkConfig.insaneDefaults")));
        insaneDefaultsMessage.setOutputMarkupId(true);

        updateBulkConfigLink();
        return header;
    }

    /**
     * A simple ajax link that asks for confirmation and configures all the selected layers and
     * layer groups using the {@link GWC#getConfig() default settings}.
     */
    private class BulkCachedLayerConfigurationLink extends AjaxLink<String> {

        private static final long serialVersionUID = 1L;

        public BulkCachedLayerConfigurationLink(String string) {
            super(string, new ResourceModel("NewCachedLayerPage.bulkConfig"));
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {

            List<TileLayer> selection = NewCachedLayerPage.this.table.getSelection();
            if (selection.isEmpty()) {
                return;
            }

            // use a list of name instead of selection so its serializable, to be used in
            // showOkCancel, and so we don't fetch the selection again
            final List<String> selectedNames = new ArrayList<String>();
            for (TileLayer layer : selection) {
                selectedNames.add(layer.getName());
            }
            dialog.setTitle(
                    new ParamResourceModel("confirmBulkConfig.title", NewCachedLayerPage.this));

            // if there is something to cancel, let's warn the user about what
            // could go wrong, and if the user accepts, let's delete what's needed
            dialog.showOkCancel(
                    target,
                    new GeoServerDialog.DialogDelegate() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Component getContents(final String id) {
                            // show a confirmation panel for all the objects we have to remove

                            final Integer selectedLayerCount = selectedNames.size();

                            IModel<String> model =
                                    new StringResourceModel(
                                                    "NewCachedLayerPage.confirmBulkConfig.message",
                                                    BulkCachedLayerConfigurationLink.this)
                                            .setParameters(
                                                    new Object[] {selectedLayerCount.toString()});
                            Label confirmLabel = new Label(id, model);
                            confirmLabel.setEscapeModelStrings(
                                    false); // allow some html inside, like
                            // <b></b>, etc
                            return confirmLabel;
                        }

                        @Override
                        protected boolean onSubmit(
                                final AjaxRequestTarget target, final Component contents) {
                            GWC facade = GWC.get();
                            GWCConfig saneConfig = facade.getConfig().saneConfig();
                            saneConfig.setCacheLayersByDefault(true);
                            facade.autoConfigureLayers(selectedNames, saneConfig);
                            table.clearSelection();
                            return true;
                        }

                        @Override
                        public void onClose(final AjaxRequestTarget target) {
                            // if the selection has been cleared out it's sign a deletion
                            // occurred, so refresh the table
                            List<TileLayer> selection = table.getSelection();
                            if (selection.isEmpty()) {
                                updateBulkConfigLink();
                                target.add(BulkCachedLayerConfigurationLink.this);
                                target.add(table);
                            }
                        }
                    });
        }
    };
}
