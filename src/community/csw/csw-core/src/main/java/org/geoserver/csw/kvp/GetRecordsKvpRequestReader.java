/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.DistributedSearchType;
import net.opengis.cat.csw20.ElementSetNameType;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;

import org.geoserver.platform.ServiceException;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * GetRecords KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class GetRecordsKvpRequestReader extends CSWKvpRequestReader {
    
    private static final String FILTER = "FILTER";
    private static final String CQL_TEXT = "CQL_TEXT";
    private static final String CONSTRAINTLANGUAGE = "constraintlanguage";
    private static final String CONSTRAINT = "constraint";
    TypeNameResolver resolver = new TypeNameResolver();

    public GetRecordsKvpRequestReader() {
        super(GetRecordsType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // fix distributed search before we get into EMF reflection mode
        String ds = (String) kvp.remove("distributedSearch");
        Integer hopCount = (Integer) kvp.remove("hopCount");
        if (rawKvp.containsKey("distributedSearch")) {

            if ("true".equalsIgnoreCase(ds)) {
                DistributedSearchType dst = Csw20Factory.eINSTANCE.createDistributedSearchType();
                if (hopCount != null) {
                    dst.setHopCount(hopCount);
                } else {
                    dst.setHopCount(2);
                }
                kvp.put("distributedSearch", dst);
            }
        }

        // parse the query
        QueryType query = readQuery(kvp);
        kvp.put("query", query);

        return super.read(request, kvp, rawKvp);
    }

    private QueryType readQuery(Map kvp) throws Exception {
        Csw20Factory factory = Csw20Factory.eINSTANCE;
        QueryType query = factory.createQueryType();

        // parse the type names
        String typeNamesString = (String) kvp.get("typeNames");
        NamespaceSupport namespaces = (NamespaceSupport) kvp.get("namespaces");
        if (namespaces == null) {
            namespaces = new NamespaceSupport();
        }
        List<QName> typeNames = resolver.parseQNames(typeNamesString, namespaces);
        query.setTypeNames(typeNames);
        
        // handle the element set
        ElementSetType elementSet = (ElementSetType) kvp.remove("elementSetName");
        if (elementSet != null) {
            ElementSetNameType esn = Csw20Factory.eINSTANCE.createElementSetNameType();
            esn.setValue(elementSet);
            esn.setTypeNames(typeNames);
            query.setElementSetName(esn);
        } 
        
        // and the element names
        String elementNamesString = (String) kvp.remove("elementName");
        if(elementNamesString != null) {
            List<QName> elementNames = resolver.parseQNames(elementNamesString, namespaces);
            query.getElementName().addAll(elementNames);
        }

        // the filter
        if (kvp.get(CONSTRAINT) != null) {
            query.setConstraint(factory.createQueryConstraintType());
            Object language = kvp.get(CONSTRAINTLANGUAGE);
            String constraint = (String) kvp.get(CONSTRAINT);
            if (CQL_TEXT.equals(language) || language == null) {
                Filter filter = null;
                try {
                    filter = CQL.toFilter(constraint);
                } catch (Exception e) {
                    ServiceException se = new ServiceException("Invalid CQL expression: " + constraint,
                            ServiceException.INVALID_PARAMETER_VALUE, CONSTRAINT);
                    se.initCause(e);
                    throw se;
                }
                query.getConstraint().setCqlText(constraint);
                query.getConstraint().setFilter(filter);
            } else if (FILTER.equals(language)) {
                try {
                    Parser parser = new Parser(new OGCConfiguration());
                    parser.setFailOnValidationError(true);
                    parser.setValidating(true);
                    parser.getNamespaces().declarePrefix("ogc", OGC.NAMESPACE);
                    Filter filter = (Filter) parser.parse(new StringReader(constraint));
                    query.getConstraint().setFilter(filter);
                    query.getConstraint().setVersion("1.1.0");
                } catch(Exception e) {
                    ServiceException se = new ServiceException("Invalid FILTER 1.1 expression: " + constraint,
                            ServiceException.INVALID_PARAMETER_VALUE, CONSTRAINT);
                    se.initCause(e);
                    throw se;
                }
            } else {
                throw new ServiceException("Invalid constraint language: " + language
                        + ", valid values are " + CQL_TEXT + " and " + FILTER,
                        ServiceException.INVALID_PARAMETER_VALUE, CONSTRAINTLANGUAGE);
            }
        }

        

        return query;
    }

  
}
