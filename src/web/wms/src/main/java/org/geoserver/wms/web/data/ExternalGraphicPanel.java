/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;

/** Allows setting the data for using an ExternalImage */
@SuppressWarnings("serial")
public class ExternalGraphicPanel extends Panel {
    private static final long serialVersionUID = 5098470683723890874L;

    private TextField<String> onlineResource;
    private TextField<String> format;
    private TextField<Integer> width;
    private TextField<Integer> height;
    private WebMarkupContainer table;
    private GeoServerAjaxFormLink autoFill;

    private Form<StyleInfo> showhideForm;

    private AjaxButton show;
    private AjaxButton hide;

    private Model<String> showhideStyleModel = new Model<String>("");

    /**
     * @param id
     * @param model Must return a {@link ResourceInfo}
     */
    public ExternalGraphicPanel(
            String id, final CompoundPropertyModel<StyleInfo> styleModel, final Form<?> styleForm) {
        super(id, styleModel);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("externalGraphicContainer");
        container.setOutputMarkupId(true);
        add(container);

        table = new WebMarkupContainer("list");
        table.setOutputMarkupId(true);

        IModel<String> bind = styleModel.bind("legend.onlineResource");
        onlineResource = new TextField<String>("onlineResource", bind);
        onlineResource.add(
                new IValidator<String>() {
                    final List<String> EXTENSIONS =
                            Arrays.asList(new String[] {"png", "gif", "jpeg", "jpg"});

                    @Override
                    public void validate(IValidatable<String> input) {
                        String value = input.getValue();
                        int last = value == null ? -1 : value.lastIndexOf('.');
                        if (last == -1
                                || !EXTENSIONS.contains(value.substring(last + 1).toLowerCase())) {
                            ValidationError error = new ValidationError();
                            error.setMessage("Not an image");
                            error.addKey("nonImage");
                            input.error(error);
                            return;
                        }
                        URI uri = null;
                        try {
                            uri = new URI(value);
                        } catch (URISyntaxException e1) {
                            // Unable to check if absolute
                        }
                        if (uri != null && uri.isAbsolute()) {
                            try {
                                String baseUrl = baseURL(onlineResource.getForm());
                                if (!value.startsWith(baseUrl)) {
                                    onlineResource.warn(
                                            "Recommend use of styles directory at " + baseUrl);
                                }
                                URL url = uri.toURL();
                                URLConnection conn = url.openConnection();
                                if ("text/html".equals(conn.getContentType())) {
                                    ValidationError error = new ValidationError();
                                    error.setMessage("Unable to access image");
                                    error.addKey("imageUnavailable");
                                    input.error(error);
                                    return; // error message back!
                                }
                            } catch (MalformedURLException e) {
                                ValidationError error = new ValidationError();
                                error.setMessage("Unable to access image");
                                error.addKey("imageUnavailable");
                                input.error(error);
                            } catch (IOException e) {
                                ValidationError error = new ValidationError();
                                error.setMessage("Unable to access image");
                                error.addKey("imageUnavailable");
                                input.error(error);
                            }
                            return; // no further checks possible
                        } else {
                            GeoServerResourceLoader resources =
                                    GeoServerApplication.get().getResourceLoader();
                            try {
                                File styles = resources.find("styles");
                                String[] path = value.split(Pattern.quote(File.separator));
                                WorkspaceInfo wsInfo = styleModel.getObject().getWorkspace();
                                File test = null;
                                if (wsInfo != null) {
                                    String wsName = wsInfo.getName();
                                    List<String> list = new ArrayList();
                                    list.addAll(Arrays.asList("workspaces", wsName, "styles"));
                                    list.addAll(Arrays.asList(path));
                                    test = resources.find(list.toArray(new String[list.size()]));
                                }
                                if (test == null) {
                                    test = resources.find(styles, path);
                                }
                                if (test == null) {
                                    ValidationError error = new ValidationError();
                                    error.setMessage("File not found in styles directory");
                                    error.addKey("imageNotFound");
                                    input.error(error);
                                }
                            } catch (IOException e) {
                                ValidationError error = new ValidationError();
                                error.setMessage("File not found in styles directory");
                                error.addKey("imageNotFound");
                                input.error(error);
                            }
                        }
                    }
                });
        onlineResource.setOutputMarkupId(true);
        table.add(onlineResource);

        // add the autofill button
        autoFill =
                new GeoServerAjaxFormLink("autoFill", styleForm) {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form<?> form) {

                        URLConnection conn = getExternalGraphic(target, form);
                        if (conn == null) {
                            ValidationError error = new ValidationError();
                            error.setMessage("Unable to access image");
                            error.addKey("imageUnavailable");
                            onlineResource.error(error);
                        } else {
                            format.setModelValue(new String[] {conn.getContentType()});
                            BufferedImage image;
                            try {
                                image = ImageIO.read(conn.getInputStream());
                                width.setModelValue(new String[] {"" + image.getWidth()});
                                height.setModelValue(new String[] {"" + image.getHeight()});
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            target.add(format);
                            target.add(width);
                            target.add(height);
                        }
                    }
                };

        table.add(autoFill);

        format = new TextField<String>("format", styleModel.bind("legend.format"));
        format.setOutputMarkupId(true);
        table.add(format);

        width = new TextField<Integer>("width", styleModel.bind("legend.width"), Integer.class);
        width.add(RangeValidator.minimum(0));
        width.setRequired(true);
        width.setOutputMarkupId(true);
        table.add(width);

        height = new TextField<Integer>("height", styleModel.bind("legend.height"), Integer.class);
        height.add(RangeValidator.minimum(0));
        height.setRequired(true);
        height.setOutputMarkupId(true);
        table.add(height);

        table.add(new AttributeModifier("style", showhideStyleModel));

        container.add(table);

        showhideForm =
                new Form<StyleInfo>("showhide") {
                    @Override
                    protected void onSubmit() {
                        super.onSubmit();
                    }
                };
        showhideForm.setMarkupId("showhideForm");
        container.add(showhideForm);
        showhideForm.setMultiPart(true);

        show =
                new AjaxButton("show") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateVisibility(true);
                        target.add(ExternalGraphicPanel.this);
                    }
                };
        container.add(show);
        showhideForm.add(show);

        hide =
                new AjaxButton("hide") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        onlineResource.setModelObject("");
                        onlineResource.clearInput();
                        format.setModelObject("");
                        format.clearInput();
                        width.setModelObject(0);
                        width.clearInput();
                        height.setModelObject(0);
                        height.clearInput();

                        updateVisibility(false);
                        target.add(ExternalGraphicPanel.this);
                    }
                };
        container.add(hide);
        showhideForm.add(hide);

        LegendInfo legend = styleModel.getObject().getLegend();
        boolean visible =
                legend != null
                        && legend.getOnlineResource() != null
                        && !legend.getOnlineResource().isEmpty();
        updateVisibility(visible);
    }

    /**
     * Lookup base URL using provided form
     *
     * @param form
     * @see ResponseUtils
     * @return baseUrl
     */
    protected String baseURL(Form<?> form) {
        HttpServletRequest httpServletRequest =
                (HttpServletRequest) form.getRequest().getContainerRequest();
        String baseUrl = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (StringUtils.isEmpty(baseUrl)) {
            GeoServer gs = GeoServerApplication.get().getGeoServer();
            baseUrl = gs.getGlobal().getSettings().getProxyBaseUrl();
            if (StringUtils.isEmpty(baseUrl)) {
                return ResponseUtils.baseURL(httpServletRequest);
            }
        }
        return baseUrl;
    }

    /**
     * Validates the external graphic and returns a connection to the graphic. If validation fails,
     * error messages will be added to the passed form
     *
     * @param target
     * @param form
     * @return URLConnection to the External Graphic file
     */
    protected URLConnection getExternalGraphic(AjaxRequestTarget target, Form<?> form) {
        onlineResource.processInput();
        if (onlineResource.getModelObject() != null) {
            URL url = null;
            try {
                String baseUrl = baseURL(form);
                String external = onlineResource.getModelObject().toString();

                URI uri = new URI(external);
                if (uri.isAbsolute()) {
                    url = uri.toURL();
                    if (!external.startsWith(baseUrl)) {
                        form.warn("Recommend use of styles directory at " + baseUrl);
                    }
                } else {
                    WorkspaceInfo wsInfo = ((StyleInfo) getDefaultModelObject()).getWorkspace();
                    if (wsInfo != null) {
                        url =
                                new URL(
                                        ResponseUtils.appendPath(
                                                baseUrl, "styles", wsInfo.getName(), external));
                    } else {
                        url = new URL(ResponseUtils.appendPath(baseUrl, "styles", external));
                    }
                }

                URLConnection conn = url.openConnection();
                if ("text/html".equals(conn.getContentType())) {
                    form.error("Unable to access url");
                    return null; // error message back!
                }
                return conn;

            } catch (FileNotFoundException notFound) {
                form.error("Unable to access " + url);
            } catch (Exception e) {
                e.printStackTrace();
                form.error("Recommend use of styles directory at " + e);
            }
        }
        return null;
    }

    /** @return the value of the onlineResource field */
    protected String getOnlineResource() {
        return onlineResource.getInput();
    }

    private void updateVisibility(boolean b) {
        if (b) {
            showhideStyleModel.setObject("");
        } else {
            showhideStyleModel.setObject("display:none");
        }
        // table.setVisible(b);
        autoFill.setVisible(b);
        hide.setVisible(b);
        show.setVisible(!b);
    }
}
