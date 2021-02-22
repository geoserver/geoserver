/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.util.Comparator;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

/**
 * Sorts feature by "code", that is, the operation name, along the service operation list, if
 * available, Lexicographically otherwise
 *
 * @author Andrea Aime - GeoSolutions
 */
public class LinkFeatureComparator implements Comparator<SimpleFeature> {

    public static final LinkFeatureComparator INSTANCE = new LinkFeatureComparator();

    private LinkFeatureComparator() {};

    static final Comparator<String> STRING_COMPARATOR =
            new NullSafeComparator<String>(new ComparableComparator<>(), true);

    @Override
    public int compare(SimpleFeature f1, SimpleFeature f2) {
        String off1 = (String) f1.getAttribute("offering");
        String code1 = (String) f1.getAttribute("code");
        String code2 = (String) f2.getAttribute("code");

        // order by the list of operations in the service if possible
        if (off1 != null) {
            Service service = getServiceFromOffering(off1);
            if (service != null) {
                int idx1 = service.getOperations().indexOf(code1);
                int idx2 = service.getOperations().indexOf(code2);
                if (idx1 == -1) {
                    return idx2 == -1 ? STRING_COMPARATOR.compare(code1, code2) : -1;
                } else {
                    return idx1 - idx2;
                }
            }
        }
        // fallback, service not found, order lexicographically
        return STRING_COMPARATOR.compare(code1, code2);
    }

    private Service getServiceFromOffering(String off1) {
        final int idx = off1.lastIndexOf('/');
        if (idx < 0 && idx >= off1.length()) {
            return null;
        }
        String serviceName = off1.substring(idx);
        List<Service> services = GeoServerExtensions.extensions(Service.class);
        for (Service service : services) {
            if (serviceName.equalsIgnoreCase(service.getId())) {
                return service;
            }
        }
        return null;
    }
}
