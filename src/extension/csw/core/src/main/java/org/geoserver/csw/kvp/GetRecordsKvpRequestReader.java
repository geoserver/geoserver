/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.Csw20Factory;
import net.opengis.cat.csw20.DistributedSearchType;
import net.opengis.cat.csw20.ElementSetNameType;
import net.opengis.cat.csw20.ElementSetType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.QueryType;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSW;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xsd.Parser;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * GetRecords KVP request reader
 *
 * @author Andrea Aime, GeoSolutions
 */
public class GetRecordsKvpRequestReader extends CSWKvpRequestReader
        implements ApplicationContextAware {

    private static final String FILTER = "FILTER";
    private static final String CQL_TEXT = "CQL_TEXT";
    private static final String CONSTRAINTLANGUAGE = "constraintlanguage";
    private static final String CONSTRAINT = "constraint";

    /** Resolves the type names into proper QName objects */
    TypeNamesResolver resolver = new TypeNamesResolver();

    HashMap<String, RecordDescriptor> descriptors;

    EntityResolverProvider resolverProvider;

    public GetRecordsKvpRequestReader(EntityResolverProvider resolverProvider) {
        super(GetRecordsType.class);
        setRepeatedParameters(true);
        this.resolverProvider = resolverProvider;
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
        QueryType query = readQuery(kvp, request);
        kvp.put("query", query);

        return super.read(request, kvp, rawKvp);
    }

    private QueryType readQuery(Map kvp, Object request) throws Exception {
        Csw20Factory factory = Csw20Factory.eINSTANCE;
        QueryType query = factory.createQueryType();

        // parse the type names
        String typeNamesString = (String) kvp.get("typeNames");
        if (typeNamesString == null) {
            throw new ServiceException(
                    "Mandatory parameter typeNames is missing",
                    ServiceException.MISSING_PARAMETER_VALUE,
                    "typeNames");
        }
        NamespaceSupport namespaces = (NamespaceSupport) kvp.get("namespace");
        if (namespaces == null) {
            // by spec, "NAMSPACE, If not included, all qualified names are in default namespace"
            String outputSchema = (String) kvp.get("outputSchema");
            if (outputSchema == null || descriptors.get(outputSchema) == null) {
                outputSchema = CSW.NAMESPACE;
            }
            namespaces = descriptors.get(outputSchema).getNamespaceSupport();
        }
        List<QName> typeNames = resolver.parseQNames(typeNamesString, namespaces);
        query.setTypeNames(typeNames);

        // handle the element set
        ElementSetType elementSet = (ElementSetType) kvp.remove("ELEMENTSETNAME");
        if (elementSet != null) {
            ElementSetNameType esn = Csw20Factory.eINSTANCE.createElementSetNameType();
            esn.setValue(elementSet);
            esn.setTypeNames(typeNames);
            query.setElementSetName(esn);
        }

        // and the element names
        String elementNamesString = (String) kvp.remove("ELEMENTNAME");
        if (elementNamesString != null) {
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
                    ServiceException se =
                            new ServiceException(
                                    "Invalid CQL expression: " + constraint,
                                    ServiceException.INVALID_PARAMETER_VALUE,
                                    CONSTRAINT);
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
                    parser.setEntityResolver(resolverProvider.getEntityResolver());
                    parser.getNamespaces().declarePrefix("ogc", OGC.NAMESPACE);
                    Filter filter = (Filter) parser.parse(new StringReader(constraint));
                    query.getConstraint().setFilter(filter);
                    query.getConstraint().setVersion("1.1.0");
                } catch (Exception e) {
                    ServiceException se =
                            new ServiceException(
                                    "Invalid FILTER 1.1 expression: " + constraint,
                                    ServiceException.INVALID_PARAMETER_VALUE,
                                    CONSTRAINT);
                    se.initCause(e);
                    throw se;
                }
            } else {
                throw new ServiceException(
                        "Invalid constraint language: "
                                + language
                                + ", valid values are "
                                + CQL_TEXT
                                + " and "
                                + FILTER,
                        ServiceException.INVALID_PARAMETER_VALUE,
                        CONSTRAINTLANGUAGE);
            }
        }

        // check if we have to sort the request
        if (kvp.get("SORTBY") != null) {
            query.setSortBy((SortBy[]) kvp.get("SORTBY"));
        }

        return query;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        descriptors = new HashMap<String, RecordDescriptor>();

        // gather all the prefix to namespace associations in the set of records we are going to
        // support, we will use them to qualify the property names in the filters
        List<RecordDescriptor> allDescriptors =
                GeoServerExtensions.extensions(RecordDescriptor.class, applicationContext);
        for (RecordDescriptor rd : allDescriptors) {
            descriptors.put(rd.getOutputSchema(), rd);
        }
    }
}
