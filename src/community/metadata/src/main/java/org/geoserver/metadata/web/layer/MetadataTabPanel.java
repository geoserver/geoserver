/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.CustomNativeMappingService;
import org.geoserver.metadata.data.service.GeonetworkImportService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.metadata.web.panel.CopyFromLayerPanel;
import org.geoserver.metadata.web.panel.ImportGeonetworkPanel;
import org.geoserver.metadata.web.panel.ImportTemplatePanel;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geotools.util.logging.Logging;

/**
 * A tabpanel that adds the metadata configuration to the layer.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class MetadataTabPanel extends PublishedEditTabPanel<LayerInfo> {

    private static final long serialVersionUID = -552158739086379566L;

    private static final Logger LOGGER = Logging.getLogger(MetadataTabPanel.class);

    private IModel<List<MetadataTemplate>> selectedTemplatesModel;

    private Map<String, List<Integer>> derivedAtts;

    private IModel<ComplexMetadataMap> metadataModel;

    @SuppressWarnings("unchecked")
    public MetadataTabPanel(
            String id, IModel<LayerInfo> model, IModel<List<MetadataTemplate>> templatesModel) {
        super(id, model);
        this.selectedTemplatesModel = templatesModel;

        Map<String, Serializable> custom;
        ResourceInfo resource = model.getObject().getResource();
        Serializable oldCustom = resource.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        if (oldCustom instanceof HashMap<?, ?>) {
            custom = (Map<String, Serializable>) oldCustom;
        } else {
            resource.getMetadata()
                    .put(
                            MetadataConstants.CUSTOM_METADATA_KEY,
                            (Serializable) (custom = new HashMap<>()));
        }
        metadataModel = new Model<ComplexMetadataMap>(new ComplexMetadataMapImpl(custom));

        Serializable oldDerivedAtts = resource.getMetadata().get(MetadataConstants.DERIVED_KEY);
        if (oldDerivedAtts instanceof HashMap<?, ?>) {
            derivedAtts = (Map<String, List<Integer>>) oldDerivedAtts;
        } else {
            resource.getMetadata()
                    .put(
                            MetadataConstants.DERIVED_KEY,
                            (Serializable) (derivedAtts = new HashMap<>()));
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ResourceInfo resource = ((LayerInfo) getDefaultModelObject()).getResource();

        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.clean(metadataModel.getObject());
        service.init(metadataModel.getObject());

        // Link with templates panel
        this.add(
                new ImportTemplatePanel("importTemplatePanel", selectedTemplatesModel) {
                    private static final long serialVersionUID = -8056914656580115202L;

                    @Override
                    protected void handleUpdate(AjaxRequestTarget target) {
                        updateAndRefresh(target, resource);
                    }
                });

        add(
                new MetadataPanel("metadataPanel", metadataModel, derivedAtts, resource)
                        .setOutputMarkupId(true));

        // Geonetwork import panel
        ImportGeonetworkPanel geonetworkPanel =
                new ImportGeonetworkPanel("geonetworkPanel") {
                    private static final long serialVersionUID = -4620394948554985874L;

                    @Override
                    public void handleImport(
                            String geoNetwork,
                            String uuid,
                            AjaxRequestTarget target,
                            FeedbackPanel feedbackPanel) {
                        try {
                            // First unlink all templates
                            importTemplatePanel()
                                    .unlinkTemplate(
                                            target, importTemplatePanel().getLinkedTemplates());
                            // Read the file
                            GeonetworkImportService importService =
                                    GeoServerApplication.get()
                                            .getApplicationContext()
                                            .getBean(GeonetworkImportService.class);
                            // import metadata
                            importService.importLayer(
                                    resource, metadataModel.getObject(), geoNetwork, uuid);

                            updateAndRefresh(target, resource);
                        } catch (IOException e) {
                            LOGGER.severe(e.getMessage());
                            feedbackPanel.error(e.getMessage());
                            target.add(feedbackPanel);
                        }
                        target.add(
                                metadataPanel()
                                        .replaceWith(
                                                new MetadataPanel(
                                                        "metadataPanel",
                                                        metadataModel,
                                                        derivedAtts,
                                                        resource)));
                    }
                };
        add(geonetworkPanel);

        add(
                new CopyFromLayerPanel("copyFromLayerPanel", resource.getId()) {
                    private static final long serialVersionUID = -4105294542603002567L;

                    @Override
                    public void handleCopy(ResourceInfo res, AjaxRequestTarget target) {
                        @SuppressWarnings("unchecked")
                        ComplexMetadataMap source =
                                new ComplexMetadataMapImpl(
                                        (Map<String, Serializable>)
                                                res.getMetadata()
                                                        .get(
                                                                MetadataConstants
                                                                        .CUSTOM_METADATA_KEY));
                        if (source != null) {
                            // First unlink all templates
                            importTemplatePanel()
                                    .unlinkTemplate(
                                            target, importTemplatePanel().getLinkedTemplates());
                            GeoServerApplication.get()
                                    .getApplicationContext()
                                    .getBean(ComplexMetadataService.class)
                                    .copy(source, metadataModel.getObject(), null, true);

                            MetadataTemplateService templateService =
                                    GeoServerApplication.get()
                                            .getApplicationContext()
                                            .getBean(MetadataTemplateService.class);
                            for (MetadataTemplate template : templateService.list()) {
                                if (template.getLinkedLayers().contains(res.getId())) {
                                    importTemplatePanel().linkTemplate(target, template);
                                }
                            }

                            ResourceInfo resource = getPublishedInfo().getResource();
                            resource.setAbstract(res.getAbstract());
                            resource.setTitle(res.getTitle());

                            updateAndRefresh(target, resource);
                        }
                    }
                });
    }

    protected void updateAndRefresh(AjaxRequestTarget target, ResourceInfo resource) {
        updateModel();
        target.add(
                metadataPanel()
                        .replaceWith(
                                new MetadataPanel(
                                        "metadataPanel", metadataModel, derivedAtts, resource)));
    }

    protected MetadataPanel metadataPanel() {
        return (MetadataPanel) get("metadataPanel");
    }

    protected ImportTemplatePanel importTemplatePanel() {
        return (ImportTemplatePanel) get("importTemplatePanel");
    }

    /** Merge the model and the linked templates. */
    private void updateModel() {
        MetadataTemplateService templateService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        ArrayList<ComplexMetadataMap> maps = new ArrayList<>();
        for (MetadataTemplate template : selectedTemplatesModel.getObject()) {
            // get latest version
            template = templateService.getById(template.getId());
            if (template != null) {
                maps.add(new ComplexMetadataMapImpl(template.getMetadata()));
            }
        }

        service.merge(metadataModel.getObject(), maps, derivedAtts);
    }

    @Override
    public void beforeSave() {
        updateModel();

        // calculate attributes of type DERIVED
        ComplexMetadataService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ComplexMetadataService.class);
        service.derive(metadataModel.getObject());

        // map to native attributes
        CustomNativeMappingService cnmService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(CustomNativeMappingService.class);
        cnmService.mapCustomToNative((LayerInfo) MetadataTabPanel.this.getDefaultModelObject());

        // save timestamp
        metadataModel
                .getObject()
                .get(Date.class, MetadataConstants.TIMESTAMP_KEY)
                .setValue(new Date());
    }

    @Override
    public void save() throws IOException {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        ResourceInfo resource = ((LayerInfo) getDefaultModelObject()).getResource();
        for (MetadataTemplate template : service.list()) {
            if (selectedTemplatesModel.getObject().contains(template)
                    && !template.getLinkedLayers().contains(resource.getId())) {
                template.getLinkedLayers().add(resource.getId());
                service.save(template);
            } else if (!selectedTemplatesModel.getObject().contains(template)
                    && template.getLinkedLayers().contains(resource.getId())) {
                template.getLinkedLayers().remove(resource.getId());
                service.save(template);
            }
        }
    }
}
