/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layer.SQLViewEditPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;

@SuppressWarnings("serial")
public class FeatureResourceConfigurationPanel extends ResourceConfigurationPanel {
    static final Logger LOGGER = Logging.getLogger(FeatureResourceConfigurationPanel.class);

    ModalWindow reloadWarningDialog;
    
    public FeatureResourceConfigurationPanel(String id, final IModel model) {
        super(id, model);
        
        final Fragment attributePanel = new Fragment("attributePanel", "attributePanelFragment", this);
        attributePanel.setOutputMarkupId(true);
        add(attributePanel);
        
        // We need to use the resourcePool directly because we're playing with an edited
        // FeatureTypeInfo and the info.getFeatureType() and info.getAttributes() will hit
        // the resource pool without the modified properties (since it passes "this" into calls
        // to the ResourcePoool
        
        // just use the direct attributes, this is not editable atm
        ListView attributes = new ListView("attributes", new AttributeListModel()) {
            @Override
            protected void populateItem(ListItem item) {
                
                // odd/even style
                item.add(new SimpleAttributeModifier("class",
                        item.getIndex() % 2 == 0 ? "even" : "odd"));

                // dump the attribute information we have
                AttributeTypeInfo attribute = (AttributeTypeInfo) item.getModelObject();
                item.add(new Label("name", attribute.getName()));
                item.add(new Label("minmax", attribute.getMinOccurs() + "/" + attribute.getMaxOccurs()));
                try {
                    // working around a serialization issue
                    FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
                    final ResourcePool resourcePool = GeoServerApplication.get().getCatalog().getResourcePool();
                    final FeatureType featureType = resourcePool.getFeatureType(typeInfo);
                    org.opengis.feature.type.PropertyDescriptor pd = featureType.getDescriptor(attribute.getName());
                    String typeName = "?";
                    String nillable = "?";
                    try {
                        typeName = pd.getType().getBinding().getSimpleName();
                        nillable = String.valueOf(pd.isNillable());
                    } catch(Exception e) {
                        LOGGER.log(Level.INFO, "Could not find attribute " + attribute.getName() + " in feature type " + featureType, e);
                    }
                    item.add(new Label("type", typeName));
                    item.add(new Label("nillable", nillable));
                } catch(IOException e) {
                    item.add(new Label("type", "?"));
                    item.add(new Label("nillable", "?"));
                }
            }
            
        };
        attributePanel.add(attributes);
        
        // reload links
        WebMarkupContainer reloadContainer = new WebMarkupContainer("reloadContainer");
        attributePanel.add(reloadContainer);
        GeoServerAjaxFormLink reload = new GeoServerAjaxFormLink("reload") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                GeoServerApplication app = (GeoServerApplication) getApplication();
                
                FeatureTypeInfo ft = (FeatureTypeInfo)getResourceInfo();
                app.getCatalog().getResourcePool().clear(ft);
                app.getCatalog().getResourcePool().clear(ft.getStore());
                target.addComponent(attributePanel);
            }
        };
        reloadContainer.add(reload);
        
        GeoServerAjaxFormLink warning = new GeoServerAjaxFormLink("reloadWarning") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                reloadWarningDialog.show(target);
            }
        };
        reloadContainer.add(warning);
        
        add(reloadWarningDialog = new ModalWindow("reloadWarningDialog"));
        reloadWarningDialog.setPageCreator(new ModalWindow.PageCreator() {
            public Page createPage() {
                return new ReloadWarningDialog(
                    new StringResourceModel("featureTypeReloadWarning", FeatureResourceConfigurationPanel.this, null));
            }
        });
        reloadWarningDialog.setTitle(new StringResourceModel("warning", (Component)  null, null));
        reloadWarningDialog.setInitialHeight(100);
        reloadWarningDialog.setInitialHeight(200);
        
        // sql view handling
        WebMarkupContainer sqlViewContainer = new WebMarkupContainer("editSqlContainer");
        attributePanel.add(sqlViewContainer);
        sqlViewContainer.add(new Link("editSql") {

            @Override
            public void onClick() {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
                try {
                    setResponsePage(new SQLViewEditPage(typeInfo, ((ResourceConfigurationPage) this.getPage())));
                } catch(Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure opening the sql view edit page", e);
                    error(e.toString());
                }
            }
            
           
        });
        
        // which one do we show, reload or edit?
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
        reloadContainer.setVisible(typeInfo.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class) == null);
        sqlViewContainer.setVisible(!reloadContainer.isVisible());
    }
    
    static class ReloadWarningDialog extends WebPage {
        public ReloadWarningDialog(StringResourceModel message) {
            add(new Label("message", message));
        }
    }
    
    class AttributeListModel extends LoadableDetachableModel<List<AttributeTypeInfo>> {

        @Override
        protected List<AttributeTypeInfo> load() {
            try {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) getDefaultModelObject();
                Catalog catalog = GeoServerApplication.get().getCatalog();
                final ResourcePool resourcePool = catalog.getResourcePool();
                return resourcePool.getAttributes(typeInfo);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Grabbing the attribute list failed", e);
                String error = new ParamResourceModel("attributeListingFailed", FeatureResourceConfigurationPanel.this, e.getMessage()).getString();
                FeatureResourceConfigurationPanel.this.getPage().error(error);
                return Collections.emptyList();
            }

        }
        
    }
}
