/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.ParamResourceModel;

/** Allows to edit the root layer of a layer group */
public class RootLayerEntryPanel extends Panel {

    private static final long serialVersionUID = 3471204885852128002L;

    public RootLayerEntryPanel(
            String id, WorkspaceInfo workspace, final IModel<LayerGroupInfo> model) {
        super(id);

        setOutputMarkupId(true);

        final TextField<LayerInfo> rootLayerField =
                new TextField<LayerInfo>("rootLayer") {
                    private static final long serialVersionUID = -8033503312874828019L;

                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (LayerInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new LayerInfoConverter();
                        } else {
                            return super.getConverter(type);
                        }
                    }
                };
        rootLayerField.setOutputMarkupId(true);
        rootLayerField.setRequired(true);
        add(rootLayerField);

        // global styles
        List<StyleInfo> globalStyles = new ArrayList<StyleInfo>();
        List<StyleInfo> allStyles = GeoServerApplication.get().getCatalog().getStyles();
        for (StyleInfo s : allStyles) {
            if (s.getWorkspace() == null) {
                globalStyles.add(s);
            }
        }

        // available styles
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        styles.addAll(globalStyles);
        if (workspace != null) {
            styles.addAll(GeoServerApplication.get().getCatalog().getStylesByWorkspace(workspace));
        }

        DropDownChoice<StyleInfo> styleField =
                new DropDownChoice<StyleInfo>("rootLayerStyle", styles) {
                    private static final long serialVersionUID = 1190134258726393181L;

                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (StyleInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new StyleInfoConverter();
                        } else {
                            return super.getConverter(type);
                        }
                    }
                };
        styleField.setNullValid(true);
        add(styleField);

        final ModalWindow popupWindow = new ModalWindow("popup");
        add(popupWindow);
        add(
                new AjaxLink<Object>("add") {
                    private static final long serialVersionUID = 723787950130153037L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                        popupWindow.setContent(
                                new LayerListPanel(popupWindow.getContentId(), workspace) {
                                    private static final long serialVersionUID =
                                            -650599334132713975L;

                                    @Override
                                    protected void handleLayer(
                                            LayerInfo layer, AjaxRequestTarget target) {
                                        popupWindow.close(target);
                                        model.getObject().setRootLayer(layer);
                                        target.add(rootLayerField);
                                    }
                                });

                        popupWindow.show(target);
                    }
                });
    }
}
