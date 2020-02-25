/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.template.TemplateUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Style page tab for displaying an OpenLayers 2 layer preview and legend. Includes a link for
 * changing the current preview layer.
 */
public class OpenLayersPreviewPanel extends StyleEditTabPanel implements IHeaderContributor {

    private static final long serialVersionUID = -8742721113748106000L;

    static final Logger LOGGER = Logging.getLogger(OpenLayersPreviewPanel.class);
    static final Configuration templates;

    static {
        templates = TemplateUtils.getSafeConfiguration();
        templates.setClassForTemplateLoading(OpenLayersPreviewPanel.class, "");
        templates.setObjectWrapper(new DefaultObjectWrapper());
    }

    final Random rand = new Random();
    final Component olPreview;

    boolean isPreviewStyleGroup;
    GeoServerDialog dialog;

    public OpenLayersPreviewPanel(String id, AbstractStylePage parent) {
        super(id, parent);
        this.olPreview = new WebMarkupContainer("olPreview").setOutputMarkupId(true);

        // Change layer link
        PropertyModel<String> layerNameModel =
                new PropertyModel<String>(parent.getLayerModel(), "prefixedName");
        add(
                new SimpleAjaxLink<String>("change.layer", layerNameModel) {
                    private static final long serialVersionUID = 7341058018479354596L;

                    public void onClick(AjaxRequestTarget target) {
                        ModalWindow popup = parent.getPopup();

                        popup.setInitialHeight(400);
                        popup.setInitialWidth(600);
                        popup.setTitle(new Model<String>("Choose layer to preview"));
                        popup.setContent(new LayerChooser(popup.getContentId(), parent));
                        popup.show(target);
                    }
                });
        add(olPreview);
        olPreview.setMarkupId("olPreview");
        setOutputMarkupId(true);

        CheckBox previewStyleGroup =
                new CheckBox(
                        "previewStyleGroup",
                        new PropertyModel<Boolean>(this, "isPreviewStyleGroup"));

        previewStyleGroup.add(
                new AjaxFormComponentUpdatingBehavior("click") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        parent.configurationChanged();
                        parent.addFeedbackPanels(target);
                        target.add(parent.styleForm);
                    }
                });
        add(previewStyleGroup);

        add(dialog = new GeoServerDialog("dialog"));
        add(new HelpLink("styleGroupHelp").setDialog(dialog));

        try {
            ensureLegendDecoration();
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to put legend layout file in the data directory, the legend decoration will not appear",
                    e);
        }
    }

    private void ensureLegendDecoration() throws IOException {
        GeoServerDataDirectory dd =
                GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
        Resource layouts = dd.get("layouts");
        Resource legend = layouts.get("style-editor-legend.xml");
        if (!Resources.exists(legend)) {
            String legendLayout =
                    IOUtils.toString(
                            OpenLayersPreviewPanel.class.getResourceAsStream(
                                    "style-editor-legend.xml"),
                            "UTF-8");
            OutputStream os = legend.out();
            try {
                IOUtils.write(legendLayout, os, "UTF-8");
            } finally {
                os.close();
            }
        }
    }

    public void renderHead(IHeaderResponse header) {
        super.renderHead(header);
        try {
            renderHeaderCss(header);
            renderHeaderScript(header);
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        } catch (TemplateException e) {
            throw new WicketRuntimeException(e);
        }
    }

    private void renderHeaderCss(IHeaderResponse header) throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("id", olPreview.getMarkupId());
        Template template = templates.getTemplate("ol-style.ftl");
        StringWriter css = new java.io.StringWriter();
        template.process(context, css);
        header.render(CssHeaderItem.forCSS(css.toString(), null));
    }

    private void renderHeaderScript(IHeaderResponse header) throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<String, Object>();
        ReferencedEnvelope bbox =
                getStylePage().getLayerInfo().getResource().getLatLonBoundingBox();
        WorkspaceInfo workspace = getStylePage().getStyleInfo().getWorkspace();
        context.put("minx", bbox.getMinX());
        context.put("miny", bbox.getMinY());
        context.put("maxx", bbox.getMaxX());
        context.put("maxy", bbox.getMaxY());
        context.put("id", olPreview.getMarkupId());
        context.put("layer", getStylePage().getLayerInfo().prefixedName());
        context.put("style", getStylePage().getStyleInfo().prefixedName());

        String styleUrl;
        String proxyBaseUrl = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (StringUtils.isEmpty(proxyBaseUrl)) {
            GeoServer gs = stylePage.getGeoServer();
            proxyBaseUrl = gs.getGlobal().getSettings().getProxyBaseUrl();
            if (StringUtils.isEmpty(proxyBaseUrl)) {
                Request r = getRequest();
                Url clientUrl = r.getClientUrl();
                styleUrl =
                        clientUrl.getProtocol()
                                + "://"
                                + clientUrl.getHost()
                                + ":"
                                + clientUrl.getPort()
                                + r.getContextPath();
            } else {
                styleUrl = proxyBaseUrl;
            }
        } else {
            styleUrl = proxyBaseUrl;
        }
        styleUrl = styleUrl + "/styles";
        if (workspace != null) {
            context.put("styleWorkspace", workspace.getName());
            styleUrl = styleUrl + "/" + workspace.getName();
        }
        String styleFile = getStylePage().getStyleInfo().getFilename();
        // If we are in a format other than sld, convert to sld
        styleFile = styleFile.substring(0, styleFile.lastIndexOf(".")) + ".sld";
        styleUrl = styleUrl + "/" + styleFile;
        context.put("styleUrl", styleUrl);
        context.put("previewStyleGroup", isPreviewStyleGroup);

        context.put("cachebuster", rand.nextInt());
        context.put("resolution", Math.max(bbox.getSpan(0), bbox.getSpan(1)) / 256.0);
        HttpServletRequest req = GeoServerApplication.get().servletRequest();
        String base = ResponseUtils.baseURL(req);
        String baseUrl = ResponseUtils.buildURL(base, "/", null, URLType.RESOURCE);
        context.put("baseUrl", canonicUrl(baseUrl));
        Template template = templates.getTemplate("ol-load.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);
        header.render(
                new CssUrlReferenceHeaderItem(
                        ResponseUtils.buildURL(base, "/openlayers3/ol.css", null, URLType.RESOURCE),
                        null,
                        null));
        header.render(
                new JavaScriptUrlReferenceHeaderItem(
                        ResponseUtils.buildURL(base, "/openlayers3/ol.js", null, URLType.RESOURCE),
                        null,
                        false,
                        "UTF-8",
                        null));
        header.render(OnLoadHeaderItem.forScript(script.toString()));
    }

    /**
     * Makes sure the url does not end with "/", otherwise we would have URL lik
     * "http://localhost:8080/geoserver//wms?LAYERS=..." and Jetty 6.1 won't digest them...
     */
    private String canonicUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            return baseUrl;
        }
    }

    public String getUpdateCommand() throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("id", olPreview.getMarkupId());
        context.put("cachebuster", rand.nextInt());

        Template template = templates.getTemplate("ol-update.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);
        return script.toString();
    }
}
