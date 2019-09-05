/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.web.GeoServerApplication;

/**
 * Tracks changes made to list of templates and calculates affected resources
 *
 * @author Niels Charlier
 */
public class MetadataTemplateTracker implements Serializable {

    private static final long serialVersionUID = -8043280155829257867L;

    private Set<SortedSet<String>> changes = new HashSet<SortedSet<String>>();

    public void switchTemplates(MetadataTemplate one, MetadataTemplate two) {
        SortedSet<String> pair = new TreeSet<String>();
        pair.add(one.getId());
        pair.add(two.getId());

        // if the pair has been switched before, it is cancelled out
        if (changes.contains(pair)) {
            changes.remove(pair);
        } else {
            changes.add(pair);
        }
    }

    public void removeTemplates(Collection<MetadataTemplate> templates) {
        for (MetadataTemplate template : templates) {
            changes.removeIf(pair -> pair.contains(template.getId()));
        }
    }

    public Set<String> getAffectedResources() {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);

        Set<String> result = new HashSet<String>();
        for (SortedSet<String> pair : changes) {
            // intersect
            MetadataTemplate temp1 = service.getById(pair.first());
            MetadataTemplate temp2 = service.getById(pair.last());
            Set<String> set = new HashSet<>(temp1.getLinkedLayers());
            set.retainAll(temp2.getLinkedLayers());
            // union
            result.addAll(set);
        }
        return result;
    }
}
