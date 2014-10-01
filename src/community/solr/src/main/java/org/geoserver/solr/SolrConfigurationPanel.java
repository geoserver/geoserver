/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.solr;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.solr.SolrLayerConfiguration;

/**
 * Resource configuration panel to show a link to open SOLR attribute modal dialog <br>
 * If the SOLR attribute are not configured for current layer, the modal dialog will be open at
 * first resource configuration window opening <br>
 * After modal dialog is closed the resource page is reloaded and feature configuration table
 * updated
 * 
 */
public class SolrConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 3382530429105288433L;

    private LayerInfo _layerInfo;

    private Boolean _isNew;

    /**
     * Adds SOLR configuration panel link, configure modal dialog and implements modal callback
     * 
     * @see {@link SolrConfigurationPage#done}
     */

    public SolrConfigurationPanel(final String panelId, final IModel model) {
        super(panelId, model);
        final FeatureTypeInfo fti = (FeatureTypeInfo) model.getObject();
        final ResourceConfigurationPanel current = this;

        final ModalWindow modal = new ModalWindow("modal");
        modal.setInitialWidth(800);
        modal.setTitle(new ParamResourceModel("modalTitle", SolrConfigurationPanel.this));
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            @Override
            public void onClose(AjaxRequestTarget target) {
                if (_layerInfo != null) {
                    GeoServerApplication app = (GeoServerApplication) getApplication();
                    FeatureTypeInfo ft = (FeatureTypeInfo) getResourceInfo();
                    app.getCatalog().getResourcePool().clear(ft);
                    app.getCatalog().getResourcePool().clear(ft.getStore());
                    setResponsePage(new ResourceConfigurationPage(_layerInfo, _isNew));
                }
            }
        });

        if (fti.getMetadata().get(SolrLayerConfiguration.KEY) == null) {
            modal.add(new OpenWindowOnLoadBehavior());
        }

        modal.setContent(new SolrConfigurationPage(panelId, model) {
            @Override
            void done(AjaxRequestTarget target, LayerInfo layerInfo, Boolean isNew) {
                _layerInfo = layerInfo;
                _isNew = isNew;
                modal.close(target);
            }
        });
        add(modal);

        AjaxLink findLink = new AjaxLink("edit") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                modal.show(target);
            }
        };
        final Fragment attributePanel = new Fragment("solrPanel", "solrPanelFragment", this);
        attributePanel.setOutputMarkupId(true);
        add(attributePanel);
        attributePanel.add(findLink);
    }

    /*
     * Open modal dialog on window load
     */
    private class OpenWindowOnLoadBehavior extends AbstractDefaultAjaxBehavior {
        @Override
        protected void respond(AjaxRequestTarget target) {
            ModalWindow window = (ModalWindow) getComponent();
            window.show(target);
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            response.renderOnLoadJavascript(getCallbackScript().toString());
        }
    }

}
