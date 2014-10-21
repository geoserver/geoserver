/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.web;

import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.wfs.WFSInfo;

/**
 * Panel for the WMS admin page to set the WMS INSPIRE extension preferences.
 */
public class InspireAdminPanel extends AdminPagePanel {

    private static final long serialVersionUID = -7670555379263411393L;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public InspireAdminPanel(final String id, final IModel<ServiceInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata = new PropertyModel<MetadataMap>(model, "metadata");

        add(new LanguageDropDownChoice("language", new MapModel(metadata, LANGUAGE.key)));

        TextField textField = new TextField("metadataURL", new MapModel(metadata,
                SERVICE_METADATA_URL.key));
        add(textField);
        textField.add(new AttributeModifier("title", true, new ResourceModel(
                "InspireAdminPanel.metadataURL.title")));

        final Map<String, String> mdUrlTypes = new HashMap<String, String>();
        mdUrlTypes.put("application/vnd.ogc.csw.GetRecordByIdResponse_xml",
                "CSW GetRecordById Response");
        mdUrlTypes.put("application/vnd.iso.19139+xml", "ISO 19139 ServiceMetadata record");

        IModel<String> urlTypeModel = new MapModel(metadata, SERVICE_METADATA_TYPE.key);

        IChoiceRenderer<String> urlTypeChoiceRenderer = new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(final String key) {
                final String resourceKey = "InspireAdminPanel.metadataURLType." + key;// as found in
                                                                                      // GeoServerApplication.properties
                final String defaultValue = key;
                final String displayValue = new ResourceModel(resourceKey, defaultValue)
                        .getObject();
                return displayValue;
            }

            public String getIdValue(final String key, int index) {
                return key;
            }
        };
        List<String> urlTypeChoices = new ArrayList<String>(mdUrlTypes.keySet());
        DropDownChoice<String> serviceMetadataRecordType = new DropDownChoice<String>(
                "metadataURLType", urlTypeModel, urlTypeChoices, urlTypeChoiceRenderer);

        add(serviceMetadataRecordType);
        
        // this is WFS specific, will appear only if the service is WFS
        WebMarkupContainer identifiersContainer = new WebMarkupContainer(
                "datasetIdentifiersContainer");
        boolean isWfs = model.getObject() instanceof WFSInfo;
        identifiersContainer.setVisible(isWfs);
        add(identifiersContainer);
//        IModel<SpatialDatasetIdentifiers> sdiModel;
//        if(isWfs) {
//            SpatialDatasetIdentifiers identifiers = model.getObject().getMetadata().get(SPATIAL_DATASET_IDENTIFIER_TYPE.key, SpatialDatasetIdentifiers.class);
//            if(identifiers != null) {
//                model.getObject().getMetadata().put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, identifiers);
//            } else {
//                identifiers = new SpatialDatasetIdentifiers();
//            }
//            sdiModel = new Model<SpatialDatasetIdentifiers>(identifiers);
//        } else {
//            sdiModel = new Model<SpatialDatasetIdentifiers>(null);
//        }
        IModel<UniqueResourceIdentifiers> sdiModel = new MetadataMapModel(metadata, SPATIAL_DATASET_IDENTIFIER_TYPE.key, UniqueResourceIdentifiers.class);
        UniqueResourceIdentifiersEditor identifiersEditor = new UniqueResourceIdentifiersEditor(
                "spatialDatasetIdentifiers", sdiModel);
        identifiersContainer.add(identifiersEditor);
    }
}
