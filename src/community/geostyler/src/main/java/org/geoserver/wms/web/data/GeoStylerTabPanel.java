package org.geoserver.wms.web.data;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.template.TemplateUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/** */
public class GeoStylerTabPanel extends StyleEditTabPanel implements IHeaderContributor {

    private static final Logger LOGGER = Logging.getLogger(GeoStylerTabPanel.class);

    private final Component geoStylerDiv;
    private static final Configuration templates;

    static {
        templates = TemplateUtils.getSafeConfiguration();
        templates.setClassForTemplateLoading(GeoStylerTabPanel.class, "");
        templates.setObjectWrapper(new DefaultObjectWrapper());
    }

    /** @param id The id given to the panel. */
    public GeoStylerTabPanel(String id, AbstractStylePage parent) {
        super(id, parent);
        CodeMirrorEditor editor = parent.editor;

        LOGGER.info("Create a new instance of GeoStylerTabPanel");

        // Change layer link
        PropertyModel<String> layerNameModel =
                new PropertyModel<>(parent.getLayerModel(), "prefixedName");
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

        this.geoStylerDiv = new WebMarkupContainer("geoStylerDiv").setOutputMarkupId(true);

        add(geoStylerDiv);
        geoStylerDiv.setMarkupId("geoStylerDiv");
        setOutputMarkupId(true);
        GeoServerDialog dialog = new GeoServerDialog("dialog");
        add(dialog);
        add(new HelpLink("styleGroupHelp").setDialog(dialog));
    }

    /** Add required CSS and Javascript resources */
    public void renderHead(IHeaderResponse header) {
        super.renderHead(header);
        try {
            renderHeaderCss(header);
            renderHeaderScript(header);
        } catch (IOException | TemplateException e) {
            throw new WicketRuntimeException(e);
        }
    }

    private void renderHeaderCss(IHeaderResponse header) throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<>();
        context.put("id", geoStylerDiv.getMarkupId());
        // TODO: Maybe we need to include custom CSS here
        // Template template = templates.getTemplate("ol-style.ftl");
        StringWriter css = new java.io.StringWriter();
        // template.process(context, css);
        header.render(CssHeaderItem.forCSS(css.toString(), "geostyler-css"));
    }

    private void renderHeaderScript(IHeaderResponse header) throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<String, Object>();
        StyleInfo styleInfo = getStylePage().getStyleInfo();
        LayerInfo layerInfo = getStylePage().getLayerInfo();
        ReferencedEnvelope bbox = layerInfo.getResource().getLatLonBoundingBox();
        context.put("layer", layerInfo.prefixedName());
        context.put("style", styleInfo.prefixedName());
        WorkspaceInfo workspace = styleInfo.getWorkspace();
        context.put("id", this.geoStylerDiv.getMarkupId());

        String styleUrl = StringUtils.EMPTY;
        /** TODO: Proxy */
        /*
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
        */
        styleUrl = styleUrl + "/styles";
        if (workspace != null) {
            context.put("styleWorkspace", workspace.getName());
            styleUrl = styleUrl + "/" + workspace.getName();
        }
        String styleFile = styleInfo.getFilename();
        // If we are in a format other than sld, convert to sld
        styleFile = styleFile.substring(0, styleFile.lastIndexOf(".")) + ".sld";
        styleUrl = styleUrl + "/" + styleFile;
        context.put("styleUrl", styleUrl);

        context.put("resolution", Math.max(bbox.getSpan(0), bbox.getSpan(1)) / 256.0);
        HttpServletRequest req = GeoServerApplication.get().servletRequest();
        String base = ResponseUtils.baseURL(req);
        String baseUrl = ResponseUtils.buildURL(base, "/", null, URLMangler.URLType.RESOURCE);
        context.put("baseUrl", canonicUrl(baseUrl));

        Template template = templates.getTemplate("geostyler-init.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);

        header.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(GeoStylerTabPanel.class, "js/geostyler.css")));
        header.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(GeoStylerTabPanel.class, "js/antd.min.css")));

        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStylerTabPanel.class, "js/react.production.min.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStylerTabPanel.class, "js/react-dom.production.min.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(GeoStylerTabPanel.class, "js/geostyler.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStylerTabPanel.class, "js/geoJsonDataParser.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStylerTabPanel.class, "js/sldStyleParser.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStylerTabPanel.class, "js/wfsDataParser.js")));
        header.render(OnLoadHeaderItem.forScript(script.toString()));
    }

    /**
     * Makes sure the url does not end with "/", otherwise we would have URLs like
     * "http://localhost:8080/geoserver//wms?LAYERS=..." and Jetty 6.1 won't digest them...
     */
    private String canonicUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            return baseUrl;
        }
    }
}
