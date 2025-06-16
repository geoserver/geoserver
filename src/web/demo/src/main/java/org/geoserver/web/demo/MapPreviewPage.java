/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geoserver.web.demo.PreviewLayerProvider.ALL;
import static org.geoserver.web.demo.PreviewLayerProvider.COMMON;
import static org.geoserver.web.demo.PreviewLayerProvider.NAME;
import static org.geoserver.web.demo.PreviewLayerProvider.TITLE;
import static org.geoserver.web.demo.PreviewLayerProvider.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.CachingImage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.GetMapOutputFormat;

/** Shows a paged list of the available layers and points to previews in various formats */
public class MapPreviewPage extends GeoServerBasePage {

    private static final long serialVersionUID = 1L;

    private static final PackageResourceReference JS_FILE =
            new PackageResourceReference(MapPreviewPage.class, "MapPreviewPage.js");

    PreviewLayerProvider provider = new PreviewLayerProvider();

    GeoServerTablePanel<PreviewLayer> table;

    private transient List<String> availableWMSFormats;
    // private transient List<String> availableWFSFormats;

    public MapPreviewPage() {
        // output formats for the drop downs
        final List<String> wmsOutputFormats = getAvailableWMSFormats();
        final List<String> wfsOutputFormats = getAvailableWFSFormats();

        // build the table
        table = new GeoServerTablePanel<>("table", provider) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<PreviewLayer> itemModel, Property<PreviewLayer> property) {
                PreviewLayer layer = itemModel.getObject();
                boolean wmsVisible = layer.hasServiceSupport("WMS");
                boolean wfsVisible = layer.hasServiceSupport("WFS");
                if (property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", MapPreviewPage.this);
                    f.add(new CachingImage("layerIcon", layer.getIcon()));
                    return f;
                } else if (property == NAME) {
                    return new Label(id, property.getModel(itemModel));
                } else if (property == TITLE) {
                    return new Label(id, property.getModel(itemModel));
                } else if (property == COMMON) {
                    Fragment f = new Fragment(id, "commonLinks", MapPreviewPage.this);
                    ListView<ExternalLink> lv = new ListView<>("commonFormat", commonFormatLinks(layer)) {
                        @Override
                        public void populateItem(ListItem<ExternalLink> item) {
                            final ExternalLink link = item.getModelObject();
                            item.add(link);
                        }
                    };
                    f.add(lv);
                    return f;
                } else if (property == ALL) {
                    return buildJSWMSSelect(
                            id,
                            wmsVisible ? wmsOutputFormats : Collections.emptyList(),
                            wfsVisible ? wfsOutputFormats : Collections.emptyList(),
                            layer);
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }
        };
        table.setTableChangeJS("MapPreviewPage_SetOnChange();");
        add(table.setOutputMarkupId(true));
        int maxFeatures = getGeoServer().getService(WFSInfo.class).getMaxNumberOfFeaturesForPreview();
        add(new HiddenField<>("maxFeatures", Model.of(maxFeatures)).setOutputMarkupId(true));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptReferenceHeaderItem.forReference(JS_FILE));
    }

    private List<ExternalLink> commonFormatLinks(PreviewLayer layer) {
        List<ExternalLink> links = new ArrayList<>();
        List<CommonFormatLink> formats = getGeoServerApplication().getBeansOfType(CommonFormatLink.class);
        Collections.sort(formats);
        for (CommonFormatLink link : formats) {
            links.add(link.getFormatLink(layer));
        }
        return links;
    }

    /**
     * Finds out the list of available WMS output formats supported bye the enable {@link GetMapOutputFormat}
     * implementations in the application context.
     *
     * <p>For format, either its {@link GetMapOutputFormat#getMimeType() MIME-Type} or one of its
     * {@link GetMapOutputFormat#getOutputFormatNames() alias} will be added to the resulting list. If one of them is
     * found to have a translation, that'll be used, otherwise the MIME-Type will be used as default.
     *
     * @return the list of available WMS GetMap output formats, giving precedence to the ones for which there is a
     *     translation.
     */
    private List<String> getAvailableWMSFormats() {
        List<String> formats = this.availableWMSFormats;
        if (formats != null) {
            return formats;
        }
        formats = new ArrayList<>();

        final GeoServerApplication application = getGeoServerApplication();
        final List<GetMapOutputFormat> outputFormats = application.getBeansOfType(GetMapOutputFormat.class);
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
        formats = new ArrayList<>(new HashSet<>(formats));
        prepareFormatList(formats, new FormatComparator("format.wms."));
        this.availableWMSFormats = formats;
        return formats;
    }

    private List<String> getAvailableWFSFormats() {
        List<String> formats = new ArrayList<>();

        final GeoServerApplication application = getGeoServerApplication();
        for (WFSGetFeatureOutputFormat producer : application.getBeansOfType(WFSGetFeatureOutputFormat.class)) {
            for (String format : producer.getOutputFormats()) {
                formats.add(format);
            }
        }
        prepareFormatList(formats, new FormatComparator("format.wfs."));

        return formats;
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

    /** Builds a select that reacts like a menu, fully javascript based, for wms outputs */
    private Component buildJSWMSSelect(
            String id, List<String> wmsOutputFormats, List<String> wfsOutputFormats, PreviewLayer layer) {
        Fragment f = new Fragment(id, "menuFragment", this);
        WebMarkupContainer menu = new WebMarkupContainer("menu");

        WebMarkupContainer wmsFormatsGroup = new WebMarkupContainer("wms");
        RepeatingView wmsFormats = new RepeatingView("wmsFormats");
        for (int i = 0; i < wmsOutputFormats.size(); i++) {
            String wmsOutputFormat = wmsOutputFormats.get(i);
            String label = translateFormat("format.wms.", wmsOutputFormat);
            // build option with text and value
            Label format = new Label(i + "", label);
            format.add(new AttributeModifier("value", new Model<>(ResponseUtils.urlEncode(wmsOutputFormat))));
            wmsFormats.add(format);
        }
        wmsFormatsGroup.add(wmsFormats);
        wmsFormatsGroup.setVisible(CollectionUtils.isNotEmpty(wmsOutputFormats));
        menu.add(wmsFormatsGroup);

        // the vector ones, it depends, we might have to hide them
        boolean vector = layer.groupInfo == null
                && (layer.layerInfo.getType() == PublishedType.VECTOR
                        || layer.layerInfo.getType() == PublishedType.REMOTE);
        WebMarkupContainer wfsFormatsGroup = new WebMarkupContainer("wfs");
        RepeatingView wfsFormats = new RepeatingView("wfsFormats");
        if (vector) {
            for (int i = 0; i < wfsOutputFormats.size(); i++) {
                String wfsOutputFormat = wfsOutputFormats.get(i);
                String label = translateFormat("format.wfs.", wfsOutputFormat);
                // build option with text and value
                Label format = new Label(i + "", label);
                format.add(new AttributeModifier("value", new Model<>(ResponseUtils.urlEncode(wfsOutputFormat))));
                wfsFormats.add(format);
            }
        }
        wfsFormatsGroup.add(wfsFormats);
        wfsFormatsGroup.setVisible(CollectionUtils.isNotEmpty(wfsOutputFormats));
        menu.add(wfsFormatsGroup);

        // onChange event handled by JS (see renderHeader)
        // we need 2 things;
        // 1. wmsLink
        // 2. wfsLink

        menu.add(new AttributeAppender("wmsLink", new Model<>(layer.getWmsLink()), ";"));
        menu.add(new AttributeAppender("wfsLink", new Model<>(layer.buildWfsLink()), ";"));

        f.add(menu);
        return f;
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

    private static class DelayedImageResource extends DynamicImageResource {
        private final IModel<PreviewLayer> itemModel;

        public DelayedImageResource(IModel<PreviewLayer> itemModel) {
            super("image/png");
            this.itemModel = itemModel;
        }

        @Override
        protected byte[] getImageData(Attributes attributes) {
            PreviewLayer layer = itemModel.getObject();
            try {
                return IOUtils.toByteArray(
                        layer.getIcon().getResource().getResourceStream().getInputStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
