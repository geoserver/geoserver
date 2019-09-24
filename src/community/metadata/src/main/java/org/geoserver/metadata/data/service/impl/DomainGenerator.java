/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexAttributeGenerator;
import org.geoserver.metadata.web.layer.MetadataTabPanel;
import org.geoserver.metadata.web.panel.GenerateDomainPanel;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

@org.springframework.stereotype.Component
public class DomainGenerator implements ComplexAttributeGenerator {

    private static final long serialVersionUID = 3179273148205046941L;

    private static final Logger LOGGER = Logging.getLogger(MetadataTabPanel.class);

    @Override
    public String getType() {
        return MetadataConstants.DOMAIN_TYPENAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void generate(
            AttributeConfiguration attributeConfiguration,
            ComplexMetadataMap metadata,
            LayerInfo layerInfo,
            Object data) {
        String attName =
                metadata.get(String.class, MetadataConstants.FEATURE_ATTRIBUTE_NAME).getValue();

        FeatureTypeInfo fti = (FeatureTypeInfo) layerInfo.getResource();

        // clear everything and build again
        metadata.delete(attributeConfiguration.getKey());
        try {
            Map<String, Object> map = (Map<String, Object>) data;
            if ((Boolean) map.get("method")) {
                Name tableName = (Name) map.get("tableName");
                Name valueAttributeName = (Name) map.get("valueAttributeName");
                Name defAttributeName = (Name) map.get("defAttributeName");
                DataAccess<? extends FeatureType, ? extends Feature> dataAccess =
                        getDataAccess(fti);
                if (dataAccess == null) {
                    return;
                }
                FeatureCollection<? extends FeatureType, ? extends Feature> features =
                        dataAccess.getFeatureSource(tableName).getFeatures(Filter.INCLUDE);
                AtomicInteger index = new AtomicInteger(0);
                features.accepts(
                        new FeatureVisitor() {
                            @Override
                            public void visit(Feature feature) {
                                Object value = feature.getProperty(valueAttributeName).getValue();
                                Object def = feature.getProperty(defAttributeName).getValue();
                                ComplexMetadataMap domainMap =
                                        metadata.subMap(
                                                attributeConfiguration.getKey(),
                                                index.getAndIncrement());
                                domainMap
                                        .get(String.class, MetadataConstants.DOMAIN_ATT_VALUE)
                                        .setValue(Converters.convert(value, String.class));
                                domainMap
                                        .get(String.class, MetadataConstants.DOMAIN_ATT_DEFINITION)
                                        .setValue(Converters.convert(def, String.class));
                            }
                        },
                        null);
            } else {
                FeatureCollection<? extends FeatureType, ? extends Feature> features =
                        fti.getFeatureSource(null, null).getFeatures(Filter.INCLUDE);
                final UniqueVisitor visitor = new UniqueVisitor(attName);
                features.accepts(visitor, null);
                AtomicInteger index = new AtomicInteger(0);
                visitor.getUnique()
                        .stream()
                        .filter(value -> value != null)
                        .sorted()
                        .forEach(
                                value -> {
                                    ComplexMetadataMap domainMap =
                                            metadata.subMap(
                                                    attributeConfiguration.getKey(),
                                                    index.getAndIncrement());
                                    domainMap
                                            .get(String.class, MetadataConstants.DOMAIN_ATT_VALUE)
                                            .setValue(Converters.convert(value, String.class));
                                });
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve domain for " + fti.getName(), e);
        }
    }

    @Override
    public boolean supports(ComplexMetadataMap metadata, LayerInfo layerInfo) {
        return layerInfo.getResource() instanceof FeatureTypeInfo;
    }

    @Override
    public Component getDialogContent(String id, LayerInfo layerInfo) {
        return new GenerateDomainPanel(id, (FeatureTypeInfo) layerInfo.getResource());
    }

    @Override
    public int getDialogContentHeight() {
        return 360;
    }

    public static DataAccess<? extends FeatureType, ? extends Feature> getDataAccess(
            FeatureTypeInfo fti) {
        Map<String, Serializable> connectionParams =
                new HashMap<>(fti.getStore().getConnectionParameters());
        connectionParams.put(JDBCDataStoreFactory.EXPOSE_PK.getName(), true);
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess;
        try {
            dataAccess = DataAccessFinder.getDataStore(connectionParams);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to access datastore for " + fti.getName(), e);
            dataAccess = null;
        }
        return dataAccess;
    }
}
