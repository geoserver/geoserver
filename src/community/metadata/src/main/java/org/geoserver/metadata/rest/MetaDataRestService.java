package org.geoserver.metadata.rest;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
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
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/metadata")
public class MetaDataRestService {

    private static final Logger LOGGER = Logging.getLogger(MetaDataRestService.class);

    @Autowired private Catalog catalog;

    @Autowired private MetadataTemplateService templateService;

    @Autowired private ComplexMetadataService metadataService;

    @Autowired private CustomNativeMappingService nativeToCustomService;

    @Autowired private GeonetworkImportService geonetworkService;

    @DeleteMapping
    public void clearAll(
            @RequestParam(required = false, defaultValue = "false") boolean iAmSure,
            @RequestParam(required = false, defaultValue = "false") boolean templatesToo,
            HttpServletResponse response)
            throws IOException {
        if (!iAmSure) {
            response.sendError(400, "You must be sure.");
        } else {
            for (ResourceInfo info : catalog.getResources(ResourceInfo.class)) {
                info.getMetadata().remove(MetadataConstants.CUSTOM_METADATA_KEY);
                info.getMetadata().remove(MetadataConstants.DERIVED_KEY);
                catalog.save(info);
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
        }
    }

    @GetMapping("fix")
    public String fixAll() {
        for (ResourceInfo info : catalog.getResources(ResourceInfo.class)) {
            Serializable custom = info.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
            if (custom instanceof HashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                ComplexMetadataMapImpl complex =
                        new ComplexMetadataMapImpl((Map<String, Serializable>) custom);
                metadataService.init(complex);
                metadataService.derive(complex);
                // save timestamp
                complex.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());
            }
            catalog.save(info);
        }
        return "Success.";
    }

    @PostMapping("nativeToCustom")
    public void nativeToCustom(
            @RequestParam(required = false) String indexes, @RequestBody String csvFile) {
        List<Integer> indexList = null;
        if (indexes != null) {
            indexList =
                    Arrays.stream(indexes.split(","))
                            .map(s -> Integer.parseInt((s.trim())))
                            .collect(Collectors.toList());
        }
        for (String resourceName : csvFile.split("\n")) {
            LayerInfo info = catalog.getLayerByName(resourceName.trim());
            if (info != null) {
                info.setResource(
                        catalog.getResource(info.getResource().getId(), ResourceInfo.class));
                nativeToCustomService.mapNativeToCustom(info, indexList);
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
            }
        }
    }

    @GetMapping("nativeToCustom")
    public String nativeToCustom(@RequestParam(required = false) String indexes) {
        List<Integer> indexList = null;
        if (indexes != null) {
            indexList =
                    Arrays.stream(indexes.split(","))
                            .map(s -> Integer.parseInt((s.trim())))
                            .collect(Collectors.toList());
        }
        for (LayerInfo info : catalog.getLayers()) {
            info.setResource(catalog.getResource(info.getResource().getId(), ResourceInfo.class));
            nativeToCustomService.mapNativeToCustom(info, indexList);
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
        }
        return "Success.";
    }

    @SuppressWarnings("unchecked")
    @PostMapping("import")
    public void importAndLink(
            @RequestParam(required = false) String geonetwork, @RequestBody String csvFile) {
        List<MetadataTemplate> templates = templateService.list();
        for (String line : csvFile.split("\n")) {
            String[] cols = line.split(";");
            if (cols.length < 2) {
                LOGGER.warning("Skipping incomplete line");
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
            }
        }
        try {
            for (MetadataTemplate template : templates) {
                templateService.save(template);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception saving templates.", e);
        }
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
}
