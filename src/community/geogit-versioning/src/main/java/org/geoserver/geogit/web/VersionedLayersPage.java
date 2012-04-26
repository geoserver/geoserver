/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geogit.web;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.geogit.api.RevCommit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.geogit.GEOGIT;
import org.geoserver.task.LongTask;
import org.geoserver.task.web.LongTasksPanel;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

public class VersionedLayersPage extends GeoServerSecuredPage implements IHeaderContributor {

    private final VersionedLayerProvider provider;

    private GeoServerTablePanel<VersionedLayerInfo> publishedLayersTable;

    private GeoServerDialog removalDialog;

    private VersionedLayerSelectionRemovalLink removalLink;

    /**
     * @see org.apache.wicket.markup.html.IHeaderContributor#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
     */
    public void renderHead(IHeaderResponse header) {
        header.renderJavascriptReference("http://static.simile.mit.edu/timeline/api-2.3.0/timeline-api.js?bundle=true");
        // header.renderJavascriptReference(new ResourceReference(VersionedLayersPage.class,
        // "timeline.js"));
        header.renderOnLoadJavascript("onLoad()");
        header.renderOnEventJavascript("window", "resize", "onResize()");
    }

    public VersionedLayersPage() throws Exception {
        provider = new VersionedLayerProvider();

        // get the date of the latest change to position the timeline at
        final Date lastChange;
        {
            final GEOGIT facade = GEOGIT.get();
            Iterator<RevCommit> commits = facade.getGeoGit().log().call();
            if (commits.hasNext()) {
                lastChange = new Date(commits.next().getTimestamp());
            } else {
                lastChange = new Date();
            }
        }
        // map model for javascript parameter substitution
        IModel<Map<String, Object>> variablesModel = new AbstractReadOnlyModel<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;

            private Map<String, Object> variables;

            @Override
            public Map<String, Object> getObject() {
                if (variables == null) {
                    variables = new HashMap<String, Object>();
                    variables.put("date",
                            new SimpleDateFormat("MMM dd yyyy HH:mm:ss 'GMT'").format(lastChange));
                }
                return variables;
            }
        };
        add(TextTemplateHeaderContributor.forJavaScript(VersionedLayersPage.class, "timeline.js",
                variablesModel));

        publishedLayersTable = new GeoSynchronizedTypesTablePanel("table", provider);
        publishedLayersTable.setOutputMarkupId(true);
        publishedLayersTable.setItemsPerPage(10);
        add(publishedLayersTable);

        LongTasksPanel longTasksPanel = new LongTasksPanel("importingPanel") {
            private static final long serialVersionUID = 1L;

            /**
             * Override to also update {@link #publishedLayersTable} so it reflects finished tasks
             * 
             * @see org.geoserver.task.web.LongTasksPanel#onTimerInternal(org.apache.wicket.ajax.AjaxRequestTarget)
             */
            @Override
            protected void onTimerInternal(final AjaxRequestTarget target) {
                target.addComponent(publishedLayersTable);
            }
        };
        longTasksPanel.setItemsPerPage(10);
        longTasksPanel.setFilter(new LongTasksPanel.TaskFilter() {
            private static final long serialVersionUID = 1L;

            /**
             * @see org.geoserver.task.web.LongTasksPanel.TaskFilter#accept(org.geoserver.task.LongTask)
             */
            public boolean accept(LongTask<?> task) {
                return task instanceof Object;
            }

        });
        add(longTasksPanel);

        // traded this panel in favour of the timeline widget
        // add(new ChangesPanel("changesPanel"));

        // the confirm dialog
        add(removalDialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    /**
     * Overrides to return {@code null}, as the default ajax indicator gets annoying very quickly on
     * the home page if there's some ajax timer to refresh some status, and it's not like we're
     * going to have any "save" button on the home page that could be pressed twice anyways.
     * 
     * @see IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
     */
    @Override
    public String getAjaxIndicatorMarkupId() {
        return null;
    }

    private Component cachedLayerLink(String id, VersionedLayerInfo layerInfo) {
        Name layerName = layerInfo.getName();
        String prefixedName;
        FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(layerName);
        if (featureType == null) {
            NamespaceInfo namespace = getCatalog().getNamespaceByURI(layerName.getNamespaceURI());
            if (namespace == null) {
                prefixedName = layerName.toString();
            } else {
                prefixedName = namespace.getPrefix() + ":" + layerName.getLocalPart();
            }
        } else {
            prefixedName = featureType.getPrefixedName();
        }
        Label link = new Label(id, prefixedName);
        if (null != layerInfo.getErrorMessage()) {
            link.add(new AttributeModifier("title", true, new Model<String>(layerInfo
                    .getErrorMessage())));
            link.add(new AttributeModifier("style", true, new Model<String>(
                    "font-style: italic; text-decoration: line-through;")));
        }

        return link;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewVersionedLayerPage.class));

        // the removal button
        header.add(removalLink = new VersionedLayerSelectionRemovalLink("removeSelected",
                publishedLayersTable, removalDialog));
        removalLink.setEnabled(false);
        // removal.setOutputMarkupId(true);
        // removal.setEnabled(false);

        return header;
    }

    private final class GeoSynchronizedTypesTablePanel extends
            GeoServerTablePanel<VersionedLayerInfo> {
        private static final long serialVersionUID = 1L;

        private GeoSynchronizedTypesTablePanel(final String id,
                final GeoServerDataProvider<VersionedLayerInfo> dataProvider) {
            super(id, dataProvider, true);
        }

        @Override
        protected void onSelectionUpdate(AjaxRequestTarget target) {
            VersionedLayersPage.this.removalLink.setEnabled(publishedLayersTable.getSelection()
                    .size() > 0);
            target.addComponent(removalLink);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<VersionedLayerInfo> property) {

            if (property == VersionedLayerProvider.TYPE) {
                final CatalogIconFactory icons = CatalogIconFactory.get();
                Fragment f = new Fragment(id, "iconFragment", VersionedLayersPage.this);
                final VersionedLayerInfo layerInfo = (VersionedLayerInfo) itemModel.getObject();

                ResourceReference layerIcon;

                Class<? extends Geometry> geometryType = layerInfo.getGeometryType();
                if (geometryType == null) {
                    layerIcon = CatalogIconFactory.UNKNOWN_ICON;
                } else {
                    layerIcon = icons.getVectorIcon(geometryType);
                }

                f.add(new Image("layerIcon", layerIcon));
                return f;
            } else if (property == VersionedLayerProvider.NAME) {
                final VersionedLayerInfo layerInfo = (VersionedLayerInfo) itemModel.getObject();
                return cachedLayerLink(id, layerInfo);
            }
            throw new IllegalArgumentException("Don't know a property named " + property.getName());
        }
    }

    private static class VersionedLayerSelectionRemovalLink extends AjaxLink<VersionedLayerInfo> {

        private static final long serialVersionUID = 1L;

        public VersionedLayerSelectionRemovalLink(String string,
                GeoServerTablePanel<VersionedLayerInfo> table, GeoServerDialog dialog) {
            super(string);
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {

        }
    }

    private static class VersionedLayerProvider extends GeoServerDataProvider<VersionedLayerInfo> {

        private static final long serialVersionUID = 4641819017764643297L;

        static final Property<VersionedLayerInfo> TYPE = new BeanProperty<VersionedLayerInfo>(
                "type", "geometryType") {

            private static final long serialVersionUID = 1L;

            @Override
            public Comparator<VersionedLayerInfo> getComparator() {
                return new Comparator<VersionedLayerInfo>() {
                    @Override
                    public int compare(VersionedLayerInfo o1, VersionedLayerInfo o2) {
                        Class<? extends Geometry> gt1 = o1.getGeometryType();
                        Class<? extends Geometry> gt2 = o2.getGeometryType();
                        if (gt1 == null) {
                            return gt2 == null ? 0 : -1;
                        }
                        if (gt2 == null) {
                            return 1;
                        }
                        return gt1.getName().compareTo(gt2.getName());
                    }
                };
            }
        };

        static final Property<VersionedLayerInfo> NAME = new BeanProperty<VersionedLayerInfo>(
                "name", "name");

        @SuppressWarnings("unchecked")
        static final List<Property<VersionedLayerInfo>> PROPERTIES = Arrays.asList(TYPE, NAME);

        /**
         * @see org.geoserver.web.wicket.GeoServerDataProvider#getItems()
         */
        @Override
        protected List<VersionedLayerInfo> getItems() {
            return VersionedLayerDetachableModel.getItems();
        }

        /**
         * @see org.geoserver.web.wicket.GeoServerDataProvider#getProperties()
         */
        @Override
        protected List<Property<VersionedLayerInfo>> getProperties() {
            return PROPERTIES;
        }

        /**
         * @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object)
         */
        public IModel<VersionedLayerInfo> newModel(final Object versionedLayerInfo) {
            return new VersionedLayerDetachableModel((VersionedLayerInfo) versionedLayerInfo);
        }

        /**
         * @see org.geoserver.web.wicket.GeoServerDataProvider#getComparator
         */
        @Override
        protected Comparator<VersionedLayerInfo> getComparator(SortParam sort) {
            return super.getComparator(sort);
        }
    }

}
