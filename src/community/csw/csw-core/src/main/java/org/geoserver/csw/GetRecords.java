/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import net.opengis.cat.csw20.ResultType;

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
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Runs the GetRecords request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GetRecords {
    
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    CSWInfo csw;

    CatalogStore store;

    private List<RecordDescriptor> recordDescriptors;

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
            RecordDescriptor rd = getRecordDescriptor(request);
            QueryType cswQuery = (QueryType) request.getQuery();
            List<Query> queries = toGtQueries(rd, cswQuery);
            
            // see how many records we have to return
            int maxRecords;
            if(request.getMaxRecords() == null) {
                maxRecords = 10;
            } else {
                maxRecords = request.getMaxRecords();
            }
            
            // and check what kind of result is desired
            ResultType resultType = request.getResultType();
            if(maxRecords == 0 && resultType == ResultType.RESULTS) {
                resultType = ResultType.HITS;
            }
            
            // compute the number of records matched (in validate mode this is also a quick way
            // to check the request)
            int numberOfRecordsMatched = 0;
            int[] counts = new int[queries.size()];
            for (int i = 0; i < queries.size(); i++) {
                counts[i] = store.getRecordsCount(queries.get(i), Transaction.AUTO_COMMIT);
                numberOfRecordsMatched += counts[i];
            }
    
            int numberOfRecordsReturned = 0;
            int nextRecord = 0;
            FeatureCollection records = null;
            if(resultType != ResultType.VALIDATE) {
                // get the offset too and compute the number of records we're returning and the next record
                int offset = request.getStartPosition() == null ? 0 : request.getStartPosition();
                if(numberOfRecordsMatched - offset <= maxRecords) {
                    numberOfRecordsReturned = numberOfRecordsMatched - offset;
                    nextRecord = -1;
                } else {
                    numberOfRecordsReturned = maxRecords;
                    nextRecord = offset + numberOfRecordsMatched + 1;
                }
    
                // time to run the queries if we are not in hits mode
                if(resultType == ResultType.RESULTS) {
                    if(resultType != resultType.HITS) {
                        List<FeatureCollection> results = new ArrayList<FeatureCollection>();
                        for (int i = 0; i < queries.size() && maxRecords > 0; i++) {
                            Query q = queries.get(i);
                            if(offset > 0) {
                                if(offset > counts[i]) {
                                    // skip the query altogheter
                                    offset -= counts[i];
                                    continue;
                                } else {
                                    q.setStartIndex(offset);
                                    offset = 0;
                                }
                            }
                            
                            if(maxRecords > 0) {
                                q.setMaxFeatures(maxRecords);
                                maxRecords -= counts[i];
                            } else {
                                // skip the query, we already have enough results
                                continue;
                            }
                            
                            results.add(store.getRecords(q, Transaction.AUTO_COMMIT));
                        }
                        
                        if(results.size() == 1) {
                            records = results.get(0);
                        } else if(results.size() > 1) {
                            records = new CompositeFeatureCollection(results);
                        }
                    }
                }
            }
            
            CSWRecordsResult result = new CSWRecordsResult(cswQuery.getElementSetName().getValue(), 
                    request.getOutputSchema(), numberOfRecordsMatched, numberOfRecordsReturned, nextRecord, timestamp, records);
            return result;
        } catch(IOException e) {
            throw new ServiceException("Request failed due to: " + e.getMessage(), e);
        }
    }

    private List<Query> toGtQueries(RecordDescriptor rd, QueryType query) throws IOException {
        // prepare to build the queries
        Filter filter = query.getConstraint().getFilter();
        Set<Name> supportedTypes = getSupportedTypes();
        
        // build one query per type name, forgetting about paging for the time being
        List<Query> result = new ArrayList<Query>();
        for (QName qName : query.getTypeNames()) {
            Name typeName = new NameImpl(qName);
            if (!supportedTypes.contains(typeName)) {
                throw new ServiceException("Unsupported record type " + typeName,
                        ServiceException.INVALID_PARAMETER_VALUE, "typeNames");
            }
            
            Query q = new Query(typeName.getLocalPart());
            q.setFilter(filter);
            q.setProperties(getPropertyNames(rd, query));
            q.setSortBy(query.getSortBy());
        }
        
        return result;
    }

    private List<PropertyName> getPropertyNames(RecordDescriptor rd, QueryType query) {
        if(query.getElementSetName() != null) {
            Set<Name> properties = rd.getPropertiesForElementSet(query.getElementSetName().getValue());
            if(properties != null) {
                // turn the names into PropertyName
                NamespaceSupport namespaces = rd.getNamespaceSupport();
                List<PropertyName> result = new ArrayList<PropertyName>();
                for (Name name : properties) {
                    String ns = name.getNamespaceURI();
                    String localName = name.getLocalPart();
                    PropertyName property = buildPropertyName(rd, namespaces, ns, localName);
                    result.add(property);
                }
                return result;
            } else {
                // the profile is the full one
                return null;
            }
        } else if(query.getElementName() != null) {
            // turn the QName into PropertyName. We don't do any verification cause the
            // elements in the actual feature could be parts of substitution groups 
            // of the elements in the feature's schema
            NamespaceSupport namespaces = rd.getNamespaceSupport();
            List<PropertyName> result = new ArrayList<PropertyName>();
            for (QName qn : query.getElementName()) {
                String ns = qn.getNamespaceURI();
                String localName = qn.getLocalPart();
                PropertyName property = buildPropertyName(rd, namespaces, ns, localName);
                result.add(property);
            }
            return result;
        } else {
            // return all properties
            return null;
        }
    }

    private PropertyName buildPropertyName(RecordDescriptor rd, NamespaceSupport namespaces,
            String ns, String localName) {
        // if we don't a namespace use the default one for that record
        if(ns == null) {
            ns = rd.getFeatureType().getName().getNamespaceURI();
        }
        String prefix = namespaces.getPrefix(ns);
        // build the xpath with the prefix, or not if we don't have one
        String xpath;
        if(prefix != null && !"".equals(prefix)) {
            xpath = prefix + ":" + localName;
        } else {
            xpath = localName;
        }
        
        PropertyName property = FF.property(xpath, namespaces);
        return property;
    }

    private Set<Name> getSupportedTypes() throws IOException {
        Set<Name> result = new HashSet<Name>();
        for (FeatureType ft : store.getRecordSchemas()) {
            result.add(ft.getName());
        }

        return result;
    }

    /**
     * Search for the record descriptor maching the request, throws a service exception in case none
     * is found
     * 
     * @param request
     * @return
     */
    private RecordDescriptor getRecordDescriptor(GetRecordsType request) {
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

        throw new ServiceException("Cannot encode records in output schema " + outputSchema,
                ServiceException.INVALID_PARAMETER_VALUE, "outputSchema");
    }

}
