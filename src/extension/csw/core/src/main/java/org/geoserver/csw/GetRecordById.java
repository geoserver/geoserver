/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordByIdType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

/**
 * Runs the GetRecordById request
 *
 * @author Niels Charlier
 */
public class GetRecordById {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    CSWInfo csw;

    CatalogStore store;

    private List<RecordDescriptor> recordDescriptors;

    public GetRecordById(
            CSWInfo csw, CatalogStore store, List<RecordDescriptor> recordDescriptors) {
        this.csw = csw;
        this.store = store;
        this.recordDescriptors = recordDescriptors;
    }

    public CSWRecordsResult run(GetRecordByIdType request) {
        // mark the time the request started
        Date timestamp = new Date();

        try {
            // build the queries
            RecordDescriptor rd = getRecordDescriptor(request);
            List<Query> queries = toGtQueries(rd, request.getId(), request);

            // compute the number of records matched (in validate mode this is also a quick way
            // to check the request)
            int numberOfRecordsMatched = 0;
            int[] counts = new int[queries.size()];
            for (int i = 0; i < queries.size(); i++) {
                counts[i] = store.getRecordsCount(queries.get(i), Transaction.AUTO_COMMIT);
                numberOfRecordsMatched += counts[i];
            }

            FeatureCollection records = null;

            // time to run the queries if we are not in hits mode

            List<FeatureCollection> results = new ArrayList<FeatureCollection>();
            for (int i = 0; i < queries.size(); i++) {
                FeatureCollection collection =
                        store.getRecords(
                                queries.get(i), Transaction.AUTO_COMMIT, request.getOutputSchema());
                if (collection != null && collection.size() > 0) {
                    results.add(collection);
                }
            }

            if (results.size() == 1) {
                records = results.get(0);
            } else if (results.size() > 1) {
                records = new CompositeFeatureCollection(results);
            }

            ElementSetType elementSet = getElementSetName(request);

            CSWRecordsResult result =
                    new CSWRecordsResult(
                            elementSet,
                            request.getOutputSchema(),
                            numberOfRecordsMatched,
                            numberOfRecordsMatched,
                            0,
                            timestamp,
                            records);
            return result;
        } catch (IOException e) {
            throw new ServiceException("Request failed due to: " + e.getMessage(), e);
        }
    }

    private ElementSetType getElementSetName(GetRecordByIdType request) {
        ElementSetType elementSet =
                request.getElementSetName() != null ? request.getElementSetName().getValue() : null;
        if (elementSet == null) {
            // the default is "summary"
            elementSet = ElementSetType.SUMMARY;
        }
        return elementSet;
    }

    private List<Query> toGtQueries(RecordDescriptor rd, EList<URI> ids, GetRecordByIdType request)
            throws IOException {
        // prepare to build the queries

        Set<FeatureId> fids = new HashSet<FeatureId>();
        for (URI id : ids) {
            fids.add(FF.featureId(id.toString()));
        }

        Filter filter = FF.id(fids);

        // build one query

        Name typeName = rd.getFeatureDescriptor().getName();
        Query q = new Query(typeName.getLocalPart());
        q.setFilter(filter);

        // perform some necessary query adjustments
        Query adapted = rd.adaptQuery(q);

        // the specification demands that we throw an error if a spatial operator
        // is used against a non spatial property
        if (q.getFilter() != null) {
            rd.verifySpatialFilters(q.getFilter());
        }

        // smuggle base url
        adapted.getHints().put(GetRecords.KEY_BASEURL, request.getBaseUrl());

        List<Query> result = new ArrayList<Query>();
        result.add(adapted);

        return result;
    }

    /**
     * Search for the record descriptor maching the request, throws a service exception in case none
     * is found
     */
    private RecordDescriptor getRecordDescriptor(GetRecordByIdType request) {
        String outputSchema = request.getOutputSchema();
        if (outputSchema == null) {
            outputSchema = CSW.NAMESPACE;
            request.setOutputFormat(CSW.NAMESPACE);
        }

        for (RecordDescriptor rd : recordDescriptors) {
            if (outputSchema.equals(rd.getOutputSchema())) {
                return rd;
            }
        }

        throw new ServiceException(
                "Cannot encode records in output schema " + outputSchema,
                ServiceException.INVALID_PARAMETER_VALUE,
                "outputSchema");
    }
}
