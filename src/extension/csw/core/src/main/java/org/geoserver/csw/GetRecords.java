/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import net.opengis.cat.csw20.ResultType;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.feature.CompositeFeatureCollection;
import org.geoserver.platform.ServiceException;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.csw.CSW;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.Types;
import org.geotools.util.factory.Hints;

/**
 * Runs the GetRecords request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetRecords {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public static final Hints.Key KEY_BASEURL = new Hints.Key(String.class);

    CSWInfo csw;

    CatalogStore store;

    private List<RecordDescriptor> recordDescriptors;

    protected static class WrappedQuery {
        Query query;
        RecordDescriptor rd;

        public WrappedQuery(Query query, RecordDescriptor rd) {
            this.query = query;
            this.rd = rd;
        }
    }

    public GetRecords(CSWInfo csw, CatalogStore store, List<RecordDescriptor> recordDescriptors) {
        this.csw = csw;
        this.store = store;
        this.recordDescriptors = recordDescriptors;
    }

    public CSWRecordsResult run(GetRecordsType request) {
        // mark the time the request started
        Date timestamp = new Date();

        try {
            // build the queries
            List<RecordDescriptor> outputRd = getRecordDescriptors(request);
            QueryType cswQuery = (QueryType) request.getQuery();
            List<WrappedQuery> queries = toGtQueries(outputRd, cswQuery, request);
            // see how many records we have to return
            int maxRecords;
            if (request.getMaxRecords() == null) {
                maxRecords = 10;
            } else {
                maxRecords = request.getMaxRecords();
            }

            // get and check the offset (which is 1 based, but our API is 0 based)
            int offset = request.getStartPosition() == null ? 0 : request.getStartPosition() - 1;
            if (offset < 0) {
                throw new ServiceException(
                        "startPosition must be a positive number",
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "startPosition");
            }

            // and check what kind of result is desired
            ResultType resultType = request.getResultType();
            if (maxRecords == 0 && resultType == ResultType.RESULTS) {
                resultType = ResultType.HITS;
            }

            // compute the number of records matched (in validate mode this is also a quick way
            // to check the request)
            int numberOfRecordsMatched = 0;
            int[] counts = new int[queries.size()];
            for (int i = 0; i < queries.size(); i++) {
                counts[i] = store.getRecordsCount(queries.get(i).query, Transaction.AUTO_COMMIT, queries.get(i).rd);
                numberOfRecordsMatched += counts[i];
            }

            ElementSetType elementSet = getElementSet(cswQuery);

            int numberOfRecordsReturned = 0;
            int nextRecord = 0;
            FeatureCollection records = null;
            if (resultType != ResultType.VALIDATE) {
                // compute the number of records we're returning and the next record
                if (offset > numberOfRecordsMatched) {
                    numberOfRecordsReturned = 0;
                    nextRecord = 0;
                } else if (numberOfRecordsMatched - offset <= maxRecords) {
                    numberOfRecordsReturned = numberOfRecordsMatched - offset;
                    nextRecord = 0;
                } else {
                    numberOfRecordsReturned = maxRecords;
                    // mind, nextRecord is 1 based too
                    nextRecord = offset + numberOfRecordsReturned + 1;
                }

                // time to run the queries if we are not in hits mode
                if (resultType == ResultType.RESULTS) {
                    if (resultType != ResultType.HITS) {
                        List<FeatureCollection<FeatureType, Feature>> results = new ArrayList<>();
                        for (int i = 0; i < queries.size() && maxRecords > 0; i++) {
                            WrappedQuery q = queries.get(i);
                            int remaining = counts[i] - offset;
                            if (offset > 0) {
                                if (offset > counts[i]) {
                                    // skip the query altogether
                                    offset -= counts[i];
                                    continue;
                                } else {
                                    q.query.setStartIndex(offset);
                                    offset = 0;
                                }
                            }

                            if (maxRecords > 0) {
                                q.query.setMaxFeatures(maxRecords);
                                maxRecords -= remaining;
                            } else {
                                // skip the query, we already have enough results
                                continue;
                            }

                            results.add(store.getRecords(q.query, Transaction.AUTO_COMMIT, q.rd));
                        }

                        if (results.size() == 1) {
                            records = results.get(0);
                        } else if (results.size() > 1) {
                            records = new CompositeFeatureCollection<>(results);
                        }
                    }
                }
            }

            // in case this is a hits request we are actually not returning any record
            if (resultType == ResultType.HITS) {
                numberOfRecordsReturned = 0;
            }

            CSWRecordsResult result = new CSWRecordsResult(
                    elementSet,
                    request.getOutputSchema(),
                    numberOfRecordsMatched,
                    numberOfRecordsReturned,
                    nextRecord,
                    timestamp,
                    records);
            return result;
        } catch (IOException e) {
            throw new ServiceException("Request failed due to: " + e.getMessage(), e);
        }
    }

    private List<WrappedQuery> toGtQueries(List<RecordDescriptor> outputRds, QueryType query, GetRecordsType request)
            throws IOException {
        // prepare to build the queries
        Filter filter = query.getConstraint() != null ? query.getConstraint().getFilter() : null;
        Set<Name> supportedTypes = getSupportedTypes();

        // the CSW specification expects like filters to be case insensitive (by CITE tests)
        // but we default to have filters case sensitive instead
        if (filter != null) {
            filter = (Filter) filter.accept(new CaseInsenstiveFilterTransformer(), null);
        }

        // build one query per type name, forgetting about paging for the time being
        List<WrappedQuery> result = new ArrayList<>();
        for (RecordDescriptor outputRd : outputRds) {
            for (QName qName : query.getTypeNames()) {
                Name typeName = new NameImpl(qName);
                if (!supportedTypes.contains(typeName)) {
                    throw new ServiceException(
                            "Unsupported record type " + typeName,
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "typeNames");
                }

                Query q = new Query(typeName.getLocalPart());
                q.setFilter(filter);
                q.setProperties(getPropertyNames(outputRd, query));
                q.setSortBy(query.getSortBy());
                try {
                    q.setNamespace(new URI(typeName.getNamespaceURI()));
                } catch (URISyntaxException e) {
                }

                // prepare later for multiple queryables mappings support
                q.getHints().put(CatalogStore.KEY_UNPREPARED, true);

                // smuggle base url
                q.getHints().put(KEY_BASEURL, request.getBaseUrl());

                result.add(new WrappedQuery(q, outputRd));
            }
        }

        return result;
    }

    private List<PropertyName> getPropertyNames(RecordDescriptor rd, QueryType query) {
        if (query.getElementName() != null && !query.getElementName().isEmpty()) {
            // turn the QName into PropertyName. We don't do any verification cause the
            // elements in the actual feature could be parts of substitution groups
            // of the elements in the feature's schema
            List<PropertyName> result = new ArrayList<>();
            for (QName qn : query.getElementName()) {
                result.addAll(store.translateToPropertyNames(rd, Types.toTypeName(qn)));
            }
            return result;
        } else {
            ElementSetType elementSet = getElementSet(query);
            List<Name> properties = rd.getPropertiesForElementSet(elementSet);
            if (properties != null) {
                List<PropertyName> result = new ArrayList<>();
                for (Name pn : properties) {
                    result.addAll(store.translateToPropertyNames(rd, pn));
                }
                return result;
            } else {
                // the profile is the full one
                return null;
            }
        }
    }

    private ElementSetType getElementSet(QueryType query) {
        if (query.getElementName() != null && !query.getElementName().isEmpty()) {
            return ElementSetType.FULL;
        }
        ElementSetType elementSet =
                query.getElementSetName() != null ? query.getElementSetName().getValue() : null;
        if (elementSet == null) {
            // the default is "summary"
            elementSet = ElementSetType.SUMMARY;
        }
        return elementSet;
    }

    private Set<Name> getSupportedTypes() throws IOException {
        Set<Name> result = new HashSet<>();
        for (RecordDescriptor rd : store.getRecordDescriptors()) {
            result.add(rd.getFeatureDescriptor().getName());
        }

        return result;
    }

    /** Search for the record descriptor maching the request, throws a service exception in case none is found */
    protected List<RecordDescriptor> getRecordDescriptors(GetRecordsType request) {
        String outputSchema = request.getOutputSchema();
        if (outputSchema == null) {
            outputSchema = CSW.NAMESPACE;
            request.setOutputFormat(CSW.NAMESPACE);
        }

        List<RecordDescriptor> list = new ArrayList<>();
        for (RecordDescriptor rd : recordDescriptors) {
            if (outputSchema.equals(rd.getOutputSchema())) {
                list.add(rd);
            }
        }

        if (list.isEmpty()) {
            throw new ServiceException(
                    "Cannot encode records in output schema " + outputSchema,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "outputSchema");
        }
        return list;
    }
}
