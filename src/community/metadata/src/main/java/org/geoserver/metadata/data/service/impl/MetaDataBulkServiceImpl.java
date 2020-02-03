package org.geoserver.metadata.data.service.impl;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.CustomNativeMappingService;
import org.geoserver.metadata.data.service.GeonetworkImportService;
import org.geoserver.metadata.data.service.GlobalModelService;
import org.geoserver.metadata.data.service.MetaDataBulkService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetaDataBulkServiceImpl implements MetaDataBulkService {

    private static final Logger LOGGER = Logging.getLogger(MetaDataBulkServiceImpl.class);

    @Autowired private Catalog catalog;

    @Autowired private MetadataTemplateService templateService;

    @Autowired private ComplexMetadataService metadataService;

    @Autowired private CustomNativeMappingService nativeToCustomService;

    @Autowired private GeonetworkImportService geonetworkService;

    @Autowired private GlobalModelService globalModelService;

    @Override
    public void clearAll(boolean templatesToo, UUID progressKey) throws IOException {
        try {
            int counter = 0;
            List<ResourceInfo> resources = catalog.getResources(ResourceInfo.class);
            for (ResourceInfo info : resources) {
                info.getMetadata().remove(MetadataConstants.CUSTOM_METADATA_KEY);
                info.getMetadata().remove(MetadataConstants.DERIVED_KEY);
                catalog.save(info);
                if (progressKey != null) {
                    globalModelService.put(
                            progressKey, ((float) counter++) / (resources.size() + 1));
                }
            }
            if (templatesToo) {
                templateService.saveList(Collections.emptyList());
            } else {
                List<MetadataTemplate> templates = templateService.list();
                for (MetadataTemplate template : templates) {
                    template.getLinkedLayers().clear();
                    templateService.save(template);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (progressKey != null) {
                globalModelService.put(progressKey, 1.0f);
            }
        }
    }

    @Override
    public void fixAll(UUID progressKey) {
        int counter = 0;
        List<ResourceInfo> resources = catalog.getResources(ResourceInfo.class);
        for (ResourceInfo info : catalog.getResources(ResourceInfo.class)) {
            Serializable custom = info.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
            if (custom instanceof HashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                ComplexMetadataMapImpl complex =
                        new ComplexMetadataMapImpl((Map<String, Serializable>) custom);
                metadataService.clean(complex);
                metadataService.init(complex);
                metadataService.derive(complex);

                // custom-to-native mapping
                for (LayerInfo layer : catalog.getLayers(info)) {
                    layer.setResource(info);
                    nativeToCustomService.mapCustomToNative(layer);
                    catalog.save(layer);
                }

                // save timestamp
                complex.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());
            }

            catalog.save(info);
            if (progressKey != null) {
                globalModelService.put(progressKey, ((float) counter++) / resources.size());
            }
        }
        if (progressKey != null) {
            globalModelService.put(progressKey, 1.0f);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importAndLink(String geonetwork, String csvFile, UUID progressKey) {
        List<MetadataTemplate> templates = templateService.list();
        int counter = 0;
        boolean succesful = true;
        String[] lines = csvFile.split("\n");
        for (String line : lines) {
            String[] cols = line.split(";");
            if (cols.length < 2) {
                LOGGER.warning("Skipping incomplete line");
                succesful = false;
                continue;
            }
            LayerInfo lInfo = catalog.getLayerByName(cols[0].trim());
            if (lInfo != null) {
                ResourceInfo rInfo =
                        catalog.getResource(lInfo.getResource().getId(), ResourceInfo.class);
                lInfo.setResource(rInfo);
                HashMap<String, Serializable> map = new HashMap<String, Serializable>();
                Serializable oldCustom =
                        rInfo.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
                if (oldCustom instanceof HashMap<?, ?>) {
                    for (Entry<? extends String, ? extends Serializable> entry :
                            ((Map<? extends String, ? extends Serializable>) oldCustom)
                                    .entrySet()) {
                        map.put(entry.getKey(), ComplexMetadataMapImpl.dimCopy(entry.getValue()));
                    }
                }
                rInfo.getMetadata().put(MetadataConstants.CUSTOM_METADATA_KEY, map);
                ComplexMetadataMap complex = new ComplexMetadataMapImpl(map);
                String uuid = cols[1].trim();
                if (uuid.length() > 0 && geonetwork != null) {
                    try {
                        geonetworkService.importLayer(rInfo, complex, geonetwork, uuid);
                    } catch (IOException | IllegalArgumentException e) {
                        LOGGER.log(Level.SEVERE, "Exception importing layer " + uuid, e);
                        succesful = false;
                    }
                }
                linkTemplates(
                        rInfo,
                        complex,
                        templates,
                        Sets.newHashSet(
                                Arrays.stream(Arrays.copyOfRange(cols, 2, cols.length))
                                        .map(s -> s.trim())
                                        .toArray(i -> new String[i])));
                nativeToCustomService.mapCustomToNative(lInfo);
                metadataService.derive(complex);
                // save timestamp
                complex.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());
                catalog.save(rInfo);
                catalog.save(lInfo);
            } else {
                LOGGER.warning("Couldn't find layer " + cols[0]);
                succesful = false;
            }
            if (progressKey != null) {
                globalModelService.put(progressKey, ((float) counter++) / lines.length);
            }
        }
        try {
            for (MetadataTemplate template : templates) {
                templateService.save(template);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception saving templates.", e);
            succesful = false;
        } finally {
            if (progressKey != null) {
                globalModelService.put(progressKey, 1.0f);
            }
        }
        return succesful;
    }

    private void linkTemplates(
            ResourceInfo resource,
            ComplexMetadataMap map,
            List<MetadataTemplate> templates,
            Set<String> templateNames) {
        List<ComplexMetadataMap> linkedTemplates = new ArrayList<ComplexMetadataMap>();
        for (MetadataTemplate template : templates) {
            if (templateNames.contains(template.getName())) {
                template.getLinkedLayers().add(resource.getId());
                linkedTemplates.add(new ComplexMetadataMapImpl(template.getMetadata()));
            } else {
                template.getLinkedLayers().remove(resource.getId());
            }
        }
        if (linkedTemplates.size() > 0) {
            @SuppressWarnings("unchecked")
            HashMap<String, List<Integer>> derivedAtts =
                    (HashMap<String, List<Integer>>)
                            resource.getMetadata()
                                    .computeIfAbsent(
                                            MetadataConstants.DERIVED_KEY, key -> new HashMap<>());
            metadataService.merge(map, linkedTemplates, derivedAtts);
            resource.getMetadata().put(MetadataConstants.DERIVED_KEY, derivedAtts);
        }
    }

    @Override
    public boolean nativeToCustom(List<Integer> indexes, String csvFile, UUID progressKey) {
        boolean success = true;
        int counter = 0;
        String[] resourceNames = csvFile.split("\n");
        for (String resourceName : resourceNames) {
            LayerInfo info = catalog.getLayerByName(resourceName.trim());
            if (info != null) {
                info.setResource(
                        catalog.getResource(info.getResource().getId(), ResourceInfo.class));
                nativeToCustomService.mapNativeToCustom(info, indexes);
                Serializable custom =
                        info.getResource().getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
                if (custom instanceof HashMap<?, ?>) {
                    @SuppressWarnings("unchecked")
                    ComplexMetadataMapImpl complex =
                            new ComplexMetadataMapImpl((Map<String, Serializable>) custom);
                    // save timestamp
                    complex.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());
                }
                catalog.save(info.getResource());
            } else {
                LOGGER.warning("Couldn't find layer " + resourceName);
                success = false;
            }
            if (progressKey != null) {
                globalModelService.put(progressKey, ((float) counter++) / resourceNames.length);
            }
        }
        if (progressKey != null) {
            globalModelService.put(progressKey, 1.0f);
        }
        return success;
    }

    @Override
    public void nativeToCustom(List<Integer> indexes, UUID progressKey) {
        int counter = 0;
        List<LayerInfo> layers = catalog.getLayers();
        for (LayerInfo info : layers) {
            info.setResource(catalog.getResource(info.getResource().getId(), ResourceInfo.class));
            nativeToCustomService.mapNativeToCustom(info, indexes);
            Serializable custom =
                    info.getResource().getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
            if (custom instanceof HashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                ComplexMetadataMapImpl complex =
                        new ComplexMetadataMapImpl((Map<String, Serializable>) custom);
                // save timestamp
                complex.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());
            }
            catalog.save(info.getResource());
            if (progressKey != null) {
                globalModelService.put(progressKey, ((float) counter++) / layers.size());
            }
        }
        if (progressKey != null) {
            globalModelService.put(progressKey, 1.0f);
        }
    }
}
