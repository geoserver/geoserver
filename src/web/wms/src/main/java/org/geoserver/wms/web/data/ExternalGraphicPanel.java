/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
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
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
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
                        if (uri != null && uri.isAbsolute() && isUrl(value)) {
                            try {
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
                            try {

                                WorkspaceInfo wsInfo = styleModel.getObject().getWorkspace();
                                getIconFromStyleDirectory(wsInfo, value);
                            } catch (Exception e) {
                                ValidationError error = new ValidationError();
                                error.setMessage(
                                        "File not found in styles directory or given path is invalid");
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
     * @return URLConnection to the External Graphic file
     */
    protected URLConnection getExternalGraphic(AjaxRequestTarget target, Form<?> form) {
        onlineResource.processInput();
        if (onlineResource.getModelObject() != null) {
            URL url = null;
            try {
                String external = onlineResource.getModelObject().toString();

                URI uri = new URI(external);
                if (uri.isAbsolute() && isUrl(external)) {
                    url = uri.toURL();
                } else {
                    WorkspaceInfo wsInfo = ((StyleInfo) getDefaultModelObject()).getWorkspace();
                    Resource icon = getIconFromStyleDirectory(wsInfo, external);

                    if (icon == null) {
                        throw new FileNotFoundException();
                    }
                    url = icon.file().toURI().toURL();
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

    private boolean isUrl(final String uri) {
        return uri.startsWith("http");
    }

    private Resource getIconFromStyleDirectory(WorkspaceInfo wsInfo, String value)
            throws Exception {
        GeoServerResourceLoader resources = GeoServerApplication.get().getResourceLoader();
        GeoServerDataDirectory gsDataDir = new GeoServerDataDirectory(resources);
        Resource icon = null;
        if (wsInfo != null) {
            icon = gsDataDir.getStyles(wsInfo, value);
        }
        if (icon == null) {
            icon = gsDataDir.getStyles(value);
            if (icon == null) throw new FileNotFoundException("file not found");
            else if (icon.getType() != Resource.Type.RESOURCE)
                throw new Exception("given path is a directory");
        }
        return icon;
    }
}
