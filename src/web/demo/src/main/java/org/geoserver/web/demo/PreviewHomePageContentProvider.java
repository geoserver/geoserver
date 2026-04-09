/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wms.GetMapOutputFormat;
import org.geotools.util.logging.Logging;

/** Contributes preview section to GeoServer Home page. */
public class PreviewHomePageContentProvider implements GeoServerHomePageContentProvider {

    protected static final Logger LOGGER = Logging.getLogger(GeoServerHomePage.class);

    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return !isAdmin && layerInfo != null;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new PreviewPanel(id);
    }

    /**
     * Preview panel for {@link GeoServerHomePage}. Panel models will require {@code getPage()} lookup to determine
     * context.
     */
    static class PreviewPanel extends Panel {
        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(PreviewHomePageContentProvider.PreviewPanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), toResourceName(getClass(), "css"))));
            }
        }

        public PreviewPanel(String id) {
            super(id);
            add(commonPreview("commonPreview"));
            add(mapPreview("mapPreview"));
            add(vectorPreview("vectorPreview"));
        }

        /** Return the application instance. */
        protected GeoServerApplication getGeoServerApplication() {
            return (GeoServerApplication) getApplication();
        }
        /**
         * Setup commonPreview div, which will only be visible when commonFormatLinks are available.
         *
         * @param id wicket id
         * @return common preview
         */
        private Component commonPreview(String id) {
            final WebMarkupContainer commonPreview = new WebMarkupContainer(id);
            commonPreview.setOutputMarkupId(true);

            LoadableDetachableModel<List<ExternalLink>> previewLinks = new LoadableDetachableModel<>() {
                @Override
                protected List<ExternalLink> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) PreviewPanel.this.getPage();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();

                    List<ExternalLink> links = commonFormatLinks(new PreviewLayer(layerInfo));
                    commonPreview.setVisible(!links.isEmpty());
                    return links;
                }
            };
            ListView<ExternalLink> commonFormats = new ListView<>("commonFormats", previewLinks) {
                @Override
                public void populateItem(ListItem<ExternalLink> item) {
                    item.add(item.getModelObject());
                }
            };
            commonPreview.add(commonFormats);

            return commonPreview;
        }

        private List<ExternalLink> commonFormatLinks(PreviewLayer layer) {
            List<ExternalLink> links = new ArrayList<>();
            List<CommonFormatLink> formats = getGeoServerApplication().getBeansOfType(CommonFormatLink.class);
            Collections.sort(formats);
            for (CommonFormatLink link : formats) {
                ExternalLink externaLink = link.getFormatLink(layer);
                if (externaLink != null && externaLink.isVisible()) {
                    // check links are visible (links may be invisible due to their service being disabled)
                    links.add(externaLink);
                }
            }
            return links;
        }

        private Component mapPreview(String id) {
            final WebMarkupContainer mapPreview = new WebMarkupContainer(id);
            mapPreview.setOutputMarkupId(true);

            LoadableDetachableModel<List<ExternalLink>> previewLinks = new LoadableDetachableModel<>() {
                @Override
                protected List<ExternalLink> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) PreviewPanel.this.getPage();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();

                    List<ExternalLink> links = mapFormatLinks(new PreviewLayer(layerInfo));
                    mapPreview.setVisible(!links.isEmpty());
                    return links;
                }
            };

            ListView<ExternalLink> mapFormats = new ListView<>("mapFormats", previewLinks) {
                @Override
                public void populateItem(ListItem<ExternalLink> item) {
                    item.add(item.getModelObject());
                }
            };
            mapPreview.add(mapFormats);

            return mapPreview;
        }

        private List<ExternalLink> mapFormatLinks(PreviewLayer layer) {
            List<String> formats = new ArrayList<>();

            final List<GetMapOutputFormat> outputFormats =
                    getGeoServerApplication().getBeansOfType(GetMapOutputFormat.class);
            for (GetMapOutputFormat producer : outputFormats) {
                Set<String> producerFormats = new HashSet<>(producer.getOutputFormatNames());
                producerFormats.add(producer.getMimeType());
                String knownFormat = producer.getMimeType();
                for (String formatName : producerFormats) {
                    String translatedFormatName = translateFormat("format.wms.", formatName);
                    if (!formatName.equals(translatedFormatName)) {
                        knownFormat = formatName;
                        break;
                    }
                }
                formats.add(knownFormat);
            }
            prepareFormatList(formats, new FormatComparator("format.wms."));

            List<ExternalLink> mapFormatLinks = new ArrayList<>();
            if (layer.hasServiceSupport("WMS")) {
                for (String formatName : formats) {
                    String labelText = translateFormat("format.wms.", formatName);
                    String href = layer.getWmsLink() + "&format=" + formatName;
                    ExternalLink mapLink = new ExternalLink("theLink", Model.of(href), Model.of(labelText));
                    mapLink.add(AttributeModifier.append("title", formatName));
                    mapLink.setVisible(layer.hasServiceSupport("WMS"));
                    mapFormatLinks.add(mapLink);
                }
            }
            return mapFormatLinks;
        }

        private Component vectorPreview(String id) {
            final WebMarkupContainer vectorPreview = new WebMarkupContainer(id);
            vectorPreview.setOutputMarkupId(true);

            LoadableDetachableModel<List<ExternalLink>> previewLinks = new LoadableDetachableModel<>() {
                @Override
                protected List<ExternalLink> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) PreviewPanel.this.getPage();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();

                    List<ExternalLink> links = vectorFormatLinks(new PreviewLayer(layerInfo));
                    vectorPreview.setVisible(!links.isEmpty());
                    return links;
                }
            };

            ListView<ExternalLink> mapFormats = new ListView<>("vectorFormats", previewLinks) {
                @Override
                public void populateItem(ListItem<ExternalLink> item) {
                    ExternalLink link = item.getModelObject();
                    item.add(link);
                }
            };
            vectorPreview.add(mapFormats);

            return vectorPreview;
        }

        private List<ExternalLink> vectorFormatLinks(PreviewLayer layer) {
            List<String> formats = new ArrayList<>();
            final GeoServerApplication application = getGeoServerApplication();
            for (WFSGetFeatureOutputFormat producer : application.getBeansOfType(WFSGetFeatureOutputFormat.class)) {
                for (String format : producer.getOutputFormats()) {
                    formats.add(format);
                }
            }
            prepareFormatList(formats, new FormatComparator("format.wfs."));

            List<ExternalLink> vectorFormatLinks = new ArrayList<>();

            if (layer.getType() == PreviewLayer.PreviewLayerType.Vector && layer.hasServiceSupport("WFS")) {
                for (String formatName : formats) {
                    String labelText = translateFormat("format.wfs.", formatName);

                    String href = layer.buildWfsLink() + "&format=" + formatName;
                    ExternalLink downloadLink = new ExternalLink("theLink", Model.of(href), Model.of(labelText));
                    downloadLink.add(AttributeModifier.append("title", formatName));

                    vectorFormatLinks.add(downloadLink);
                }
            }

            return vectorFormatLinks;
        }

        private void prepareFormatList(List<String> formats, FormatComparator comparator) {
            Collections.sort(formats, comparator);
            String prev = null;
            for (Iterator<String> it = formats.iterator(); it.hasNext(); ) {
                String format = it.next();
                if (prev != null && comparator.compare(format, prev) == 0) it.remove();
                prev = format;
            }
        }

        /**
         * Translate format (if translation available).
         *
         * @param prefix protocol
         * @param format output format
         * @return format translation (defaults to format if unavailable)
         */
        private String translateFormat(String prefix, String format) {
            try {
                return getLocalizer().getString(prefix + format, this);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return format;
            }
        }

        /**
         * Sorts the formats using the i18n translated name
         *
         * @author aaime
         */
        private class FormatComparator implements Comparator<String> {
            String prefix;

            public FormatComparator(String prefix) {
                this.prefix = prefix;
            }

            @Override
            public int compare(String f1, String f2) {
                String t1 = translateFormat(prefix, f1);
                String t2 = translateFormat(prefix, f2);
                return t1.compareTo(t2);
            }
        }
    }
}
