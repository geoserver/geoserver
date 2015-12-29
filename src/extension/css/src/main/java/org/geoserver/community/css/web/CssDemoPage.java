/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.StyledLayerDescriptor;

public class CssDemoPage extends GeoServerSecuredPage {

    /**
     * Eventually prefixes the
     * 
     * @author Andrea Aime - GeoSolutions
     *
     */
    public class StyleNameModel extends LoadableDetachableModel<String> {

        private StyleInfo style;

        public StyleNameModel(StyleInfo style) {
            this.style = style;
        }

        @Override
        protected String load() {
            if (style.getWorkspace() != null) {
                return style.getWorkspace().getName() + ":" + style.getName();
            } else {
                return style.getName();
            }
        }

    }

    /*
     * @TODO: externalize this string to a classpath resource
     */
    public static String defaultStyle = "* { fill: lightgrey; }";

    public OpenLayersMapPanel map = null;

    public Label sldPreview = null;

    private LayerInfo layer = null;

    private StyleInfo style = null;

    private GeoServerDialog dialog;

    public CssDemoPage() {
        this(new PageParameters());
    }

    public CssDemoPage(PageParameters params) {
        this.layer = extractLayer(params, getCatalog());
        this.style = extractStyle(params, getCatalog(), this.layer);
        if (this.layer == null || this.style == null) {
            doErrorLayout();
        } else {
            doMainLayout();
        }
    }

    private static LayerInfo extractLayer(PageParameters params, Catalog catalog) {
        if (params.getNamedKeys().contains("layer")) {
            String name = params.get("layer").toString();
            return catalog.getLayerByName(name);
        } else {
            // TODO: Revisit this behavior
            // give some slight preference to the topp:states layer to make
            // demoing a bit more consistent.
            LayerInfo states = catalog.getLayerByName("topp:states");
            if (states != null) {
                return states;
            } else {
                List<LayerInfo> layers = catalog.getLayers();
                if (layers.size() > 0) {
                    return layers.get(0);
                } else {
                    return null;
                }
            }
        }
    }

    private static StyleInfo extractStyle(PageParameters params, Catalog catalog, LayerInfo layer) {
        if (params.getNamedKeys().contains("style")) {
            String style = params.get("style").toString();
            String[] parts = style.split(":", 2);
            if (parts.length == 1) {
                return catalog.getStyleByName(parts[0]);
            } else if (parts.length == 2) {
                return catalog.getStyleByName(parts[0], parts[1]);
            } else {
                throw new IllegalStateException(
                        "After splitting, there should be only 1 or 2 parts.  Got: "
                                + Arrays.toString(parts));
            }
        } else {
            if (layer != null) {
                return layer.getDefaultStyle();
            } else {
                return null;
            }
        }
    }

    public Resource findStyleFile(StyleInfo style) {
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(getCatalog()
                .getResourceLoader());
        return datadir.style(style);
    }

    public Catalog catalog() {
        return getCatalog();
    }

    public StyleInfo getStyleInfo() {
        return this.style;
    }

    public LayerInfo getLayer() {
        return this.layer;
    }

    String cssText2sldText(String css, StyleInfo styleInfo) {
        try {
            CssHandler cssHandler = getGeoServerApplication().getBeanOfType(CssHandler.class);
            StyledLayerDescriptor sld = cssHandler.convertToSLD(css);

            SLDTransformer tx = new org.geotools.styling.SLDTransformer();
            tx.setIndentation(2);
            StringWriter sldChars = new java.io.StringWriter();
            tx.transform(sld, sldChars);
            return sldChars.toString();
        } catch (Exception e) {
            throw new WicketRuntimeException("Error while parsing stylesheet [" + css + "] : " + e);
        }
    }

    void createCssTemplate(String workspace, String name) {
        try {
            Catalog catalog = getCatalog();
            if (catalog.getStyleByName(workspace, name) == null) {
                StyleInfo style = catalog.getFactory().createStyle();
                style.setName(name);
                style.setFilename(name + ".sld");
                if (workspace != null) {
                    WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
                    if (ws == null) {
                        throw new WicketRuntimeException("Workspace does not exist: " + workspace);
                    }
                    style.setWorkspace(ws);
                }

                Resource sld = findStyleFile(style);
                if (sld == null || !Resources.exists(sld)) {
                    catalog.getResourcePool().writeStyle(
                            style,
                            new ByteArrayInputStream(cssText2sldText(defaultStyle, style)
                                    .getBytes()));
                    sld = findStyleFile(style);
                }

                Resource css = sld.parent().get(name + ".css");
                if (!Resources.exists(css)) {
                    OutputStreamWriter writer = new OutputStreamWriter(css.out());
                    writer.write(defaultStyle);
                    writer.close();
                }

                catalog.add(style);
            }
        } catch (IOException ioe) {
            throw new WicketRuntimeException(ioe);
        }
    }

    private void doErrorLayout() {
        add(new Fragment("main-content", "loading-failure", this));
    }

    private void doMainLayout() {
        final Fragment mainContent = new Fragment("main-content", "normal", this);

        final ModalWindow popup = new ModalWindow("popup");
        mainContent.add(popup);
        final StyleNameModel styleNameModel = new StyleNameModel(style);
        final PropertyModel layerNameModel = new PropertyModel(layer, "prefixedName");

        mainContent.add(new AjaxLink("create.style", new ParamResourceModel(
                "CssDemoPage.createStyle", this)) {
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                popup.setInitialHeight(200);
                popup.setInitialWidth(300);
                popup.setTitle(new Model("Choose name for new style"));
                popup.setContent(new StyleNameInput(popup.getContentId(), CssDemoPage.this));
                popup.show(target);
            }
        });

        mainContent.add(new SimpleAjaxLink("change.style", styleNameModel) {
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model("Choose style to edit"));
                popup.setContent(new StyleChooser(popup.getContentId(), CssDemoPage.this));
                popup.show(target);
            }
        });
        mainContent.add(new SimpleAjaxLink("change.layer", layerNameModel) {
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model("Choose layer to edit"));
                popup.setContent(new LayerChooser(popup.getContentId(), CssDemoPage.this));
                popup.show(target);
            }
        });
        mainContent.add(new AjaxLink("associate.styles", new ParamResourceModel(
                "CssDemoPage.associateStyles", this)) {
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model("Choose layers to associate"));
                popup.setContent(new MultipleLayerChooser(popup.getContentId(), CssDemoPage.this));
                popup.show(target);
            }
        });
        ParamResourceModel associateToLayer = new ParamResourceModel(
                "CssDemoPage.associateDefaultStyle", this, styleNameModel, layerNameModel);
        final SimpleAjaxLink associateDefaultStyle = new SimpleAjaxLink("associate.default.style",
                new Model(), associateToLayer) {
            public void onClick(final AjaxRequestTarget linkTarget) {
                final Component theComponent = this;
                dialog.setResizable(false);
                dialog.setHeightUnit("em");
                dialog.setWidthUnit("em");
                dialog.setInitialHeight(7);
                dialog.setInitialWidth(50);
                dialog.showOkCancel(linkTarget, new DialogDelegate() {
                    boolean success = false;

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        layer.setDefaultStyle(style);
                        getCatalog().save(layer);
                        theComponent.setEnabled(false);
                        success = true;
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        super.onClose(target);
                        target.add(theComponent);
                        if (success) {
                            CssDemoPage.this.info(new ParamResourceModel(
                                    "CssDemoPage.styleAssociated", CssDemoPage.this,
                                    styleNameModel, layerNameModel).getString());
                            target.add(getFeedbackPanel());
                        }
                    }

                    @Override
                    protected Component getContents(String id) {
                        ParamResourceModel confirm = new ParamResourceModel(
                                "CssDemoPage.confirmAssocation", CssDemoPage.this, styleNameModel
                                        .getObject(), layerNameModel.getObject(), layer
                                        .getDefaultStyle().getName());
                        return new Label(id, confirm);
                    }
                });
            }
        };
        associateDefaultStyle.setOutputMarkupId(true);
        if (layer.getDefaultStyle().equals(style)) {
            associateDefaultStyle.setEnabled(false);
        }

        mainContent.add(associateDefaultStyle);

        final IModel<String> sldModel = new AbstractReadOnlyModel<String>() {
            public String getObject() {
                try {
                    // if file already in css format transform to sld, otherwise load the SLD file
                    if (CssHandler.FORMAT.equals(style.getFormat())) {
                        StyledLayerDescriptor sld = Styles.sld(style.getStyle());
                        return Styles.string(sld, new SLDHandler(), SLDHandler.VERSION_10, true);
                    } else {
                        Resource file = findStyleFile(style);
                        if (file != null && file.getType() == Type.RESOURCE) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(new InputStreamReader(file.in()));
                                StringBuilder builder = new StringBuilder();
                                char[] line = new char[4096];
                                int len = 0;
                                while ((len = reader.read(line, 0, 4096)) >= 0)
                                    builder.append(line, 0, len);
                                return builder.toString();
                            } finally {
                                if (reader != null)
                                    reader.close();
                            }
                        } else {
                            return "No SLD file found for this style. One will be generated automatically if you save the CSS.";
                        }
                    }
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }
            }
        };

        final CompoundPropertyModel model = new CompoundPropertyModel<CssDemoPage>(CssDemoPage.this);
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(new PanelCachingTab(new AbstractTab(new Model("Generated SLD")) {
            public Panel getPanel(String id) {
                SLDPreviewPanel panel = new SLDPreviewPanel(id, sldModel);
                sldPreview = panel.getLabel();
                return panel;
            }
        }));
        tabs.add(new PanelCachingTab(new AbstractTab(new Model("Map")) {
            public Panel getPanel(String id) {
                return map = new OpenLayersMapPanel(id, layer, style);
            }
        }));
        if (layer.getResource() instanceof FeatureTypeInfo) {
            tabs.add(new PanelCachingTab(new AbstractTab(new Model("Data")) {
                public Panel getPanel(String id) {
                    try {
                        return new DataPanel(id, model, (FeatureTypeInfo) layer.getResource());
                    } catch (IOException e) {
                        throw new WicketRuntimeException(e);
                    }
                };
            }));
        } else if (layer.getResource() instanceof CoverageInfo) {
            tabs.add(new PanelCachingTab(new AbstractTab(new Model("Data")) {
                public Panel getPanel(String id) {
                    return new BandsPanel(id, (CoverageInfo) layer.getResource());
                };
            }));
        }
        tabs.add(new AbstractTab(new Model("CSS Reference")) {
            public Panel getPanel(String id) {
                return new DocsPanel(id);
            }
        });

        Resource sldFile = findStyleFile(style);
        Resource cssFile = sldFile.parent().get(style.getName() + ".css");

        mainContent.add(new StylePanel("style.editing", model, CssDemoPage.this, cssFile));

        mainContent.add(new AjaxTabbedPanel("context", tabs));

        add(mainContent);
        add(dialog = new GeoServerDialog("dialog"));
    }
}
