/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.ParamResourceModel;


/**
 * Allows to edit the root layer of a layer group
 */
@SuppressWarnings("serial")
public class RootLayerEntryPanel extends Panel {

    @SuppressWarnings({ "rawtypes" })
    public RootLayerEntryPanel(String id, final Form form, WorkspaceInfo workspace) {
        super(id);
        
        setOutputMarkupId(true);
        
        final TextField<LayerInfo> rootLayerField = new TextField<LayerInfo>("rootLayer") {
            @Override
            public IConverter getConverter(Class<?> type) { 
                return form.getConverter(type);
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
        
        DropDownChoice<StyleInfo> styleField = new DropDownChoice<StyleInfo>("rootLayerStyle", styles) {
            @Override
            public IConverter getConverter(Class<?> type) { 
                return form.getConverter(type);
            }             
        };
        styleField.setNullValid(true);
        add(styleField);
        
        final ModalWindow popupWindow = new ModalWindow("popup");
        add(popupWindow);
        add(new AjaxLink("add") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                popupWindow.setContent(new LayerListPanel(popupWindow.getContentId()) {
                    @Override
                    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {
                        popupWindow.close(target);
                        ((LayerGroupInfo) form.getModelObject()).setRootLayer(layer);
                        target.addComponent(rootLayerField);
                    }
                });

                popupWindow.show(target);
            }
        });
    }
}
