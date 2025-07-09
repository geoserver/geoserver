/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.geoserver.gwc.web.layer.CachedLayerProvider.ACTIONS;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.BLOBSTORE;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.ENABLED;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.NAME;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.PREVIEW_LINKS;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.QUOTA_LIMIT;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.QUOTA_USAGE;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CachingImage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.geowebcache.seed.TruncateAllRequest;

/**
 * @author groldan
 * @see GWC#removeTileLayers(List)
 */
public class CachedLayersPage extends GeoServerSecuredPage {

    private static Logger log = Logging.getLogger(CachedLayersPage.class);

    private static final long serialVersionUID = -6795610175856538774L;

    private static final PackageResourceReference JS_FILE =
            new PackageResourceReference(CachedLayersPage.class, "CachedLayersPage.js");

    private CachedLayerProvider provider = new CachedLayerProvider();

    private GeoServerTablePanel<TileLayer> table;

    private GeoServerDialog dialog;

    private CachedLayerSelectionRemovalLink removal;

    public CachedLayersPage() {

        table = new GeoServerTablePanel<>("table", provider, true) {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings({"unchecked"})
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<TileLayer> itemModel, Property<TileLayer> property) {

                if (property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    TileLayer layer = itemModel.getObject();
                    PackageResourceReference layerIcon = GWCIconFactory.getSpecificLayerIcon(layer);
                    f.add(new CachingImage("layerIcon", layerIcon));
                    return f;
                } else if (property == NAME) {
                    return nameLink(id, itemModel);
                } else if (property == QUOTA_LIMIT) {
                    IModel<Quota> quotaLimitModel = (IModel<Quota>) property.getModel(itemModel);
                    return quotaLink(id, quotaLimitModel);
                } else if (property == QUOTA_USAGE) {
                    IModel<Quota> quotaUsageModel = (IModel<Quota>) property.getModel(itemModel);
                    return quotaLink(id, quotaUsageModel);
                } else if (property == ENABLED) {
                    TileLayer layerInfo = itemModel.getObject();
                    boolean enabled = layerInfo.isEnabled();
                    PackageResourceReference icon;
                    if (enabled) {
                        icon = GWCIconFactory.getEnabledIcon();
                    } else {
                        icon = GWCIconFactory.getDisabledIcon();
                    }
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    f.add(new CachingImage("layerIcon", icon));
                    return f;
                } else if (property == PREVIEW_LINKS) {
                    return previewLinks(id, itemModel);
                } else if (property == ACTIONS) {
                    return actionsLinks(id, itemModel);
                } else if (property == BLOBSTORE) {
                    return null;
                }

                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(!table.getSelection().isEmpty());
                target.add(removal);
            }
        };
        table.setTableChangeJS("CachedLayersPage_SetOnChange();");
        add(table.setOutputMarkupId(true));

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(360);
        dialog.setInitialHeight(180);
        setHeaderPanel(headerPanel());

        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg = new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning").getObject();
            super.warn(warningMsg);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptReferenceHeaderItem.forReference(JS_FILE));
    }

    private Component quotaLink(String id, IModel<Quota> quotaModel) {
        Quota quota = quotaModel.getObject();
        String formattedQuota;
        if (null == quota) {
            formattedQuota = new ResourceModel("CachedLayersPage.quotaLimitNotSet").getObject();
        } else {
            formattedQuota = quota.toNiceString();
        }
        return new Label(id, formattedQuota);
    }

    private Component nameLink(String id, IModel<TileLayer> itemModel) {

        Component link;

        final TileLayer layer = itemModel.getObject();

        final String layerName = layer.getName();
        if (layer instanceof GeoServerTileLayer) {
            link = new ConfigureCachedLayerAjaxLink(id, itemModel, CachedLayersPage.class);
        } else {
            link = new Label(id, layerName);
        }

        return link;
    }

    private Component actionsLinks(String id, IModel<TileLayer> tileLayerNameModel) {
        final String name = tileLayerNameModel.getObject().getName();
        final String baseURL = ResponseUtils.baseURL(getGeoServerApplication().servletRequest());
        // Since we're working with an absolute URL, build the URL this way to ensure proxy
        // mangling is applied.
        final String href = ResponseUtils.buildURL(baseURL, "gwc/rest/seed/" + name, null, URLType.EXTERNAL);

        // openlayers preview
        Fragment f = new Fragment(id, "actionsFragment", this);
        f.add(new ExternalLink("seedLink", href, new ResourceModel("CachedLayersPage.seed").getObject()));
        f.add(truncateLink("truncateLink", tileLayerNameModel));
        return f;
    }

    private SimpleAjaxLink<String> truncateLink(final String id, IModel<TileLayer> tileLayerNameModel) {

        String layerName = tileLayerNameModel.getObject().getName();
        IModel<String> model = new Model<>(layerName);
        IModel<String> labelModel = new ResourceModel("truncate");

        SimpleAjaxLink<String> link = new SimpleAjaxLink<>(id, model, labelModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target) {

                dialog.setTitle(new ParamResourceModel("confirmTruncateTitle", CachedLayersPage.this));
                dialog.setDefaultModel(getDefaultModel());

                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component getContents(final String id) {
                        final String layerName = getDefaultModelObjectAsString();

                        // show a confirmation panel for all the objects we have to
                        // remove
                        final GWC gwcFacade = GWC.get();
                        Quota usedQuota = gwcFacade.getUsedQuota(layerName);
                        if (usedQuota == null) {
                            usedQuota = new Quota();
                        }
                        final String usedQuotaStr = usedQuota.toNiceString();
                        IModel<String> model = new ParamResourceModel(
                                "CachedLayersPage.confirmTruncateMessage",
                                CachedLayersPage.this,
                                layerName,
                                usedQuotaStr);
                        Label confirmLabel = new Label(id, model);
                        confirmLabel.setEscapeModelStrings(false); // allow some html inside, like
                        // <b></b>, etc
                        return confirmLabel;
                    }

                    @Override
                    protected boolean onSubmit(final AjaxRequestTarget target, final Component contents) {
                        final String layerName = getDefaultModelObjectAsString();
                        GWC facade = GWC.get();
                        facade.truncate(layerName);
                        return true;
                    }

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.add(table);
                    }
                });
            }
        };

        return link;
    }

    private Component previewLinks(String id, IModel<TileLayer> tileLayerModel) {

        final TileLayer layer = tileLayerModel.getObject();
        if (!layer.isEnabled()) {
            return new Label(id, new ResourceModel("previewDisabled"));
        }
        final Set<String> gridSubsets = new TreeSet<>(layer.getGridSubsets());
        final List<MimeType> mimeTypes = new ArrayList<>(layer.getMimeTypes());
        Collections.sort(mimeTypes, (o1, o2) -> o1.getFormat().compareTo(o2.getFormat()));

        Fragment f = new Fragment(id, "menuFragment", this);

        WebMarkupContainer menu = new WebMarkupContainer("menu");

        RepeatingView previewLinks = new RepeatingView("previewLink");

        // build the wms request, redirect to it in a new window, reset the selection
        final String baseURL = ResponseUtils.baseURL(getGeoServerApplication().servletRequest());
        // Since we're working with an absolute URL, build the URL this way to ensure proxy
        // mangling is applied.

        // make the URL workspace-based (in case global services are turned off)
        String workspaceName = "";
        if (layer.getName().contains(":")) {
            workspaceName = layer.getName().substring(0, layer.getName().indexOf(":")) + "/";
        }
        final String demoURL =
                ResponseUtils.buildURL(baseURL + workspaceName, "gwc/demo/" + layer.getName(), null, URLType.EXTERNAL)
                        + "?gridSet=";

        int i = 0;
        for (String gridSetId : gridSubsets) {
            for (MimeType mimeType : mimeTypes) {
                String label = gridSetId + " / " + mimeType.getFileExtension();
                // build option with text and value
                Label format = new Label(String.valueOf(i++), label);
                String value = demoURL + gridSetId + "&format=" + mimeType.getFormat();
                format.add(new AttributeModifier("value", new Model<>(value)));
                previewLinks.add(format);
            }
        }
        menu.add(previewLinks);

        f.add(menu);
        return f;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<>("addNew", NewCachedLayerPage.class));

        // the removal button
        header.add(removal = new CachedLayerSelectionRemovalLink("removeSelected"));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        // the clear All GWC cache link
        header.add(new TruncateAllLink("clearGwcLink"));

        return header;
    }

    private class CachedLayerSelectionRemovalLink extends AjaxLink<TileLayer> {

        public CachedLayerSelectionRemovalLink(String string) {
            super(string);
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {

            List<TileLayer> selection = CachedLayersPage.this.table.getSelection();
            if (selection.isEmpty()) {
                return;
            }

            final List<String> selectedNames = new ArrayList<>();
            for (TileLayer layer : selection) {
                selectedNames.add(layer.getName());
            }
            dialog.setTitle(new ParamResourceModel("confirmRemoval", CachedLayersPage.this));

            // if there is something to cancel, let's warn the user about what
            // could go wrong, and if the user accepts, let's delete what's needed
            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Component getContents(final String id) {
                    // show a confirmation panel for all the objects we have to remove
                    final GWC gwcFacade = GWC.get();
                    Quota totalQuota = new Quota();
                    for (String layerName : selectedNames) {
                        Quota usedQuota = gwcFacade.getUsedQuota(layerName);
                        if (usedQuota != null) {
                            totalQuota.add(usedQuota);
                        }
                    }
                    final String usedQuotaStr = totalQuota.toNiceString();
                    final Integer selectedLayerCount = selectedNames.size();

                    IModel<String> model = new StringResourceModel(
                                    "CachedLayersPage.confirmSelectionRemoval", CachedLayerSelectionRemovalLink.this)
                            .setParameters(new Object[] {selectedLayerCount.toString(), usedQuotaStr});
                    Label confirmLabel = new Label(id, model);
                    confirmLabel.setEscapeModelStrings(false); // allow some html inside, like
                    // <b></b>, etc
                    return confirmLabel;
                }

                @Override
                protected boolean onSubmit(final AjaxRequestTarget target, final Component contents) {
                    GWC facade = GWC.get();
                    facade.removeTileLayers(selectedNames);
                    table.clearSelection();
                    return true;
                }

                @Override
                public void onClose(final AjaxRequestTarget target) {
                    // if the selection has been cleared out it's sign a deletion
                    // occurred, so refresh the table
                    List<TileLayer> selection = table.getSelection();
                    if (selection.isEmpty()) {
                        setEnabled(false);
                        target.add(CachedLayerSelectionRemovalLink.this);
                        target.add(table);
                    }
                }
            });
        }
    }

    private class TruncateAllLink extends AjaxLink<String> {

        public TruncateAllLink(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setTitle(new ParamResourceModel("confirmGwcTruncateTitle", CachedLayersPage.this));
            dialog.setDefaultModel(getDefaultModel());

            GeoServerDialog.DialogDelegate delegate = new GeoServerDialog.DialogDelegate() {
                private TruncateAllRequest truncateAllRequest;

                @Override
                protected Component getContents(final String id) {
                    Label confirmLabel =
                            new Label(id, new ParamResourceModel("confirmGWCClean", CachedLayersPage.this));
                    confirmLabel.setEscapeModelStrings(false); // allow some html inside, like
                    // <b></b>, etc
                    return confirmLabel;
                }

                @Override
                protected boolean onSubmit(final AjaxRequestTarget target1, final Component contents) {

                    GWC facade = GWC.get();
                    try {
                        truncateAllRequest = facade.truncateAll();
                    } catch (Exception e) {
                        error(message("confirmGWCClean"));
                        log.log(Level.SEVERE, "An Error while clearing GWC cache", e);
                        return false;
                    }
                    return true;
                }

                private String message(String key) {
                    return new ParamResourceModel(key, CachedLayersPage.this).getString();
                }

                @Override
                public void onClose(final AjaxRequestTarget target) {
                    target.add(table);
                    if (truncateAllRequest != null)
                        if (truncateAllRequest.getTrucatedLayers().length() == 0) warn(message("warnGWCClean"));
                        else info(message("confirmGWCCleanInfo"));
                    else error(message("errorGWCClean2"));
                    setResponsePage(getPage());
                }
            };
            dialog.showOkCancel(target, delegate);
        }
    }
}
