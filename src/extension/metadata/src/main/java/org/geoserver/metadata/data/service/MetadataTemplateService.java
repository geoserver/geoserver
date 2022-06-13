/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.geoserver.metadata.data.model.MetadataTemplate;

/** @author Timothy De Bock */
public interface MetadataTemplateService {

    List<MetadataTemplate> list();

    void save(MetadataTemplate metadataTemplate) throws IOException;

    void saveList(List<MetadataTemplate> newList) throws IOException;

    void update(Collection<String> resourceIds, UUID progressKey);

    default void update(MetadataTemplate template, UUID progressKey) {
        update(template.getLinkedLayers(), progressKey);
    }

    MetadataTemplate findByName(String string);

    MetadataTemplate getById(String id);
}
