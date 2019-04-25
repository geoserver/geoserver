/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.Query;
import org.geotools.xsd.EMFUtils;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class LockFeatureKvpRequestReader extends BaseFeatureKvpRequestReader {

    public LockFeatureKvpRequestReader(GeoServer geoServer, FilterFactory filterFactory) {
        super(LockFeatureType.class, WfsFactory.eINSTANCE, geoServer, filterFactory);
    }

    protected void querySet(EObject request, String property, List values) throws WFSException {
        // no values specified, do nothing
        if (values == null) {
            return;
        }

        if ("typeName".equalsIgnoreCase(property)) {
            // in lock typename is not a list, it's a single qname
            values =
                    (List) values.stream().map(o -> ((List) o).get(0)).collect(Collectors.toList());
        }

        LockFeatureType lockFeature = (LockFeatureType) request;
        EList lock = lockFeature.getLock();

        int m = values.size();
        int n = lock.size();

        if ((m == 1) && (n > 1)) {
            // apply single value to all queries
            EMFUtils.set(lock, property, values.get(0));

            return;
        }

        // WfsFactory wfsFactory = (WfsFactory) getFactory();
        // match up sizes
        if (m > n) {
            if (n == 0) {
                // make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    lock.add(WfsFactory.eINSTANCE.createLockType());
                }
            } else if (n == 1) {
                // clone single object up to
                EObject q = (EObject) lock.get(0);

                for (int i = 1; i < m; i++) {
                    lock.add(EMFUtils.clone(q, WfsFactory.eINSTANCE, false));
                }

                return;
            } else {
                // illegal
                String msg = "Specified " + m + " " + property + " for " + n + " locks.";
                throw new WFSException(request, msg);
            }
        }
        if (m < n) {
            // fill the rest with nulls
            List newValues = new ArrayList<>();
            newValues.addAll(values);
            for (int i = 0; i < n - m; i++) {
                newValues.add(null);
            }
            values = newValues;
        }

        EMFUtils.set(lock, property, values);
    }

    protected void buildStoredQueries(EObject request, List<URI> storedQueryIds, Map kvp) {
        throw new UnsupportedOperationException("No stored queries in WFS 1.0 or 1.1");
    }

    protected List<Query> getQueries(EObject eObject) {
        throw new UnsupportedOperationException();
    }

    protected void handleBBOX(Map kvp, EObject eObject) throws Exception {
        // set filter from bbox
        Envelope bbox = (Envelope) kvp.get("bbox");

        List<LockType> queries = ((LockFeatureType) eObject).getLock();
        for (Iterator it = queries.iterator(); it.hasNext(); ) {
            LockType lock = (LockType) it.next();

            Filter filter = bboxFilter(bbox);
            lock.setFilter(filter);
        }
    }
}
