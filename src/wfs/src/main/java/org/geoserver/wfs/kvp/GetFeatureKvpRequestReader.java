/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.ParameterType;
import net.opengis.wfs20.StoredQueryType;
import net.opengis.wfs20.Wfs20Factory;

import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.NumericKvpParser;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.request.GetFeatureRequest.WFS20;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.xml.EMFUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.Envelope;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.ows.kvp.ViewParamsKvpParser;

/**
 * 
 * @author Niels Charlier : added 3D BBOX support
 *
 */
public class GetFeatureKvpRequestReader extends WFSKvpRequestReader {
    /**
     * Catalog used in qname parsing
     */
    Catalog catalog;

    /**
     * Factory used in filter parsing
     */
    FilterFactory filterFactory;

    public GetFeatureKvpRequestReader(Class requestBean, Catalog catalog, FilterFactory filterFactory) {
        this(requestBean, WfsFactory.eINSTANCE, catalog, filterFactory);
    }

    public GetFeatureKvpRequestReader(Class requestBean, EFactory factory, Catalog catalog, 
        FilterFactory filterFactory) {
        super(requestBean, factory);
        this.catalog = catalog;
        this.filterFactory = filterFactory;
    }
    
    /**
     * Performs additinon GetFeature kvp parsing requirements
     */
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        //hack but startIndex conflicts with WMS startIndex... which parses to different type, so 
        // we just parse manually
        if (rawKvp.containsKey("startIndex")) {
            kvp.put("startIndex", 
                new NumericKvpParser(null, BigInteger.class).parse((String)rawKvp.get("startIndex")));
        }
        
        request = super.read(request, kvp, rawKvp);
        
        //get feature has some additional parsing requirements
        EObject eObject = (EObject) request;

        // make sure the filter is specified in just one way
        ensureMutuallyExclusive(kvp, new String[] { "featureId", "resourceId", "filter", "bbox", "cql_filter" }, eObject);

        //outputFormat
        if (!EMFUtils.isSet(eObject, "outputFormat")) {
            //set the default
            String version = (String) EMFUtils.get(eObject, "version");
            switch(WFSInfo.Version.negotiate(version)) {
                case V_10:
                    EMFUtils.set(eObject, "outputFormat", "GML2"); break;
                case V_11:
                    EMFUtils.set(eObject, "outputFormat", "text/xml; subtype=gml/3.1.1"); break;
                case V_20:
                default:
                    EMFUtils.set(eObject, "outputFormat", "application/gml+xml; version=3.2");
            };
        }

        // did the user supply alternate namespace prefixes?
        NamespaceSupport namespaces = null;
        if (kvp.containsKey("namespace")) {
            if (kvp.get("namespace") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespace");
            } else {
                LOGGER.warning("There's a namespace parameter but it seems it wasn't parsed to a "
                        + NamespaceSupport.class.getName() + ": " + kvp.get("namespace"));
            }
        }
        
        //typeName (in WFS 2.0 it is typeNames, not typeName)
        if (kvp.containsKey("typeName") || kvp.containsKey("typeNames")) {
            //HACK, the kvp reader gives us a list of QName, need to wrap in 
            // another
            List typeName = (List) kvp.get("typeName");
            if (typeName == null) {
                typeName = (List) kvp.get("typeNames");
            }
            List list = new ArrayList();

            for (Iterator itr = typeName.iterator(); itr.hasNext();) {
                Object obj = itr.next();
                
                //we might get a list of qname, or a list of list of qname
                if (obj instanceof QName) {
                    QName qName = (QName) obj;
                    qName = checkTypeName(qName, namespaces, eObject);
                    
                    List l = new ArrayList();
                    l.add(qName);
                    list.add(l);
                }
                else {
                    List<QName> qNames = (List<QName>) obj;
                    for (int i = 0; i < qNames.size(); i++) {
                        qNames.set(i, checkTypeName(qNames.get(i), namespaces, eObject));
                    }

                    list.add(qNames);
                }
            }

            kvp.put("typeName", list);
            querySet(eObject, "typeName", list);
        } else {
            // check for featureId and infer typeName
            // in WFS 2.0 it is resourceId
            if (kvp.containsKey("featureId") || kvp.containsKey("resourceId")) {
                //use featureId to infer type Names
                List featureId = (List) kvp.get("featureId");
                featureId = featureId != null ? featureId : (List) kvp.get("resourceId");
                
                QNameKvpParser parser = new QNameKvpParser("typeName", catalog);

                Set<List> hTypeNames = new HashSet<>();
                for (int i = 0; i < featureId.size(); i++) {
                    String fid = (String) featureId.get(i);
                    int pos = fid.indexOf(".");

                    if (pos != -1) {
                        String typeName = fid.substring(0, fid.lastIndexOf("."));

                        //add to a list to set on the query
                        List<QName> parsed = (List) parser.parse(typeName);
                        hTypeNames.add(parsed);
                    }
                }
                
                //remove duplicate typeNames from the list
                List typeNames = new ArrayList<>(hTypeNames);
                querySet(eObject, "typeName", typeNames);
            } else {
                //check for stored query id, i have seen both storedQueryId and storedQuery_Id used
                // so support both
                List<URI> storedQueryId = null;
                if (kvp.containsKey("storedQuery_Id")) {
                    storedQueryId = (List<URI>) kvp.get("storedQuery_Id");
                }
                if (storedQueryId == null && kvp.containsKey("storedQueryId")) {
                    storedQueryId = (List<URI>) kvp.get("storedQueryId");
                }
                if (storedQueryId != null) {
                    buildStoredQueries(eObject, storedQueryId, kvp);
                }
                else {
                    throw new WFSException(eObject, "The query should specify either typeName, featureId filter" +
                        ", or a stored query id", "MissingParameterValue");
                }
            }
        }
        
        //filter
        if (kvp.containsKey("filter")) {
            querySet(eObject, "filter", (List) kvp.get("filter"));
        } else if (kvp.containsKey("cql_filter")) {
            querySet(eObject, "filter", (List) kvp.get("cql_filter"));
        } else if (kvp.containsKey("featureId") || kvp.containsKey("resourceId")) {
            //set filter from featureId
            List featureIdList = (List) kvp.get("featureId");
            featureIdList = featureIdList != null ? featureIdList : (List) kvp.get("resourceId"); 
            Set ids = new HashSet();

            for (Iterator i = featureIdList.iterator(); i.hasNext();) {
                String fid = (String) i.next();
                FeatureId featureId = filterFactory.featureId(fid);
               ids.add(featureId);
            }
            // build a single feature id filter
            List filters = Collections.singletonList(filterFactory.id(ids));

            querySet(eObject, "filter", filters);
        } else if (kvp.containsKey("bbox")) {
            //set filter from bbox 
            Envelope bbox = (Envelope) kvp.get("bbox");

            List<Query> queries = GetFeatureRequest.adapt(eObject).getQueries();
            List filters = new ArrayList();

            for (Iterator<Query> it = queries.iterator(); it.hasNext();) {
                Query q = it.next();
                
                List typeName = q.getTypeNames();
                Filter filter = null;

                if (typeName.size() > 1) {
                    //TODO: not sure what to do here, just going to and them up
                    List and = new ArrayList(typeName.size());

                    for (Iterator t = typeName.iterator(); t.hasNext();) {
                        and.add(bboxFilter((QName) t.next(), bbox));
                    }

                    filter = filterFactory.and(and);
                } else {
                    filter = bboxFilter((QName) typeName.get(0), bbox);
                }

                filters.add(filter);
            }

            querySet(eObject, "filter", filters);
        }

        //aliases
        if (kvp.containsKey("aliases")) {
            querySet(eObject, "aliases", (List) kvp.get("aliases"));
        }

        //propertyName
        if (kvp.containsKey("propertyName")) {
            List<String> propertyNames = new ArrayList<String>();
            if( kvp.get("propertyName") != null && kvp.get("propertyName") instanceof List ) 
            {
                propertyNames = (List) kvp.get("propertyName");
            }
            else if( kvp.get("propertyName") != null && kvp.get("propertyName") instanceof String ) 
            {
                propertyNames.addAll(KvpUtils.readFlat((String) kvp.get("propertyName")));
            } 
            querySet(eObject, "propertyName", propertyNames);
        }

        //sortBy
        if (kvp.containsKey("sortBy")) {
            querySet(eObject, "sortBy", (List) kvp.get("sortBy"));
        }

        //srsName
        if (kvp.containsKey("srsName")) {
            querySet(eObject, "srsName",Collections.singletonList((URI)kvp.get("srsName")));
        }

        //featureversion
        if (kvp.containsKey("featureVersion")) {
            querySet(eObject, "featureVersion",
                Collections.singletonList((String) kvp.get("featureVersion")));
        }
        
        GetFeatureRequest req = GetFeatureRequest.adapt(request);
        if(kvp.containsKey("format_options")) {
            req.getFormatOptions().putAll((Map) kvp.get("format_options"));
        }
        
        // sql view params
        if(kvp.containsKey("viewParams")) {
            
            if(req.getViewParams() == null) {
                req.setViewParams(new ArrayList<Map<String,String>>());
            }

            // fan out over all layers if necessary
            List<Map<String, String>> viewParams = (List<Map<String, String>>) kvp.get("viewParams");
            if(viewParams.size() > 0) {                
                int layerCount = req.getQueries().size();

                // if we have just one replicate over all layers
                if(viewParams.size() == 1 && layerCount > 1) {
                    List<Map<String, String>> replacement = new ArrayList<Map<String,String>>();
                    for (int i = 0; i < layerCount; i++) {
                        replacement.add(viewParams.get(0));
                    }
                    viewParams = replacement;
                } else if(viewParams.size() != layerCount) {
                    String msg = layerCount + " feature types requested, but found " + viewParams.size()
                   + " view params specified. ";
                    throw new WFSException(eObject, msg, getClass().getName());
                }
            }

            req.setViewParams(viewParams);
        }

        return request;
    }

    /**
     * Given a set of keys, this method will ensure that no two keys are specified at the same time
     * @param kvp
     * @param keys
     */
    private void ensureMutuallyExclusive(Map kvp, String[] keys, EObject request) {
        for (int i = 0; i < keys.length; i++) {
            if (kvp.containsKey(keys[i])) {
                for (int j = i + 1; j < keys.length; j++) {
                    if (kvp.containsKey(keys[j])) {
                        String msg = keys[i] + " and " + keys[j]
                            + " both specified but are mutually exclusive";
                        throw new WFSException(request, msg);
                    }
                }
            }
        }
    }

    QName checkTypeName(QName qName, NamespaceSupport namespaces, EObject request) {
     // check the type name is known, otherwise complain
        String namespaceURI = qName.getNamespaceURI();
        String localPart = qName.getLocalPart();
        String prefix = qName.getPrefix();
        if (namespaces != null) {
            if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                // request did not specify a namespace prefix for the typeName,
                // let's see if it speficied a default namespace
                String uri = namespaces.getURI(XMLConstants.DEFAULT_NS_PREFIX);
                if (!XMLConstants.NULL_NS_URI.equals(uri)) {
                    // alright, request came with xmlns(http:...) to idicat the typeName's
                    // namespace
                    namespaceURI = uri;
                }
            } else if (namespaces.getURI(prefix) != null) {
                // so request used a custom prefix and declared the prefix:uri mapping?
                namespaceURI = namespaces.getURI(qName.getPrefix());
            }
            NamespaceInfo ns = catalog.getNamespaceByURI(namespaceURI);
            if (ns == null) {
                throw new WFSException("Unknown namespace [" + qName.getPrefix() + "]",
                        "InvalidParameterValue", "namespace");
            }
            prefix = ns.getPrefix();
            qName = new QName(namespaceURI, localPart, prefix);
        }

        if (!XMLConstants.DEFAULT_NS_PREFIX.equals(qName.getPrefix())
                && catalog.getNamespaceByPrefix(qName.getPrefix()) == null) {
            throw new WFSException("Unknown namespace [" + qName.getPrefix() + "]",
                    "InvalidParameterValue", "namespace");
        }

        if (catalog.getFeatureTypeByName(namespaceURI, localPart) == null) {
            String name = qName.getPrefix() + ":" + qName.getLocalPart();
            throw new WFSException("Feature type " + name + " unknown",
                    "InvalidParameterValue", "typeName");
        }
        return qName;
    }
    
    BBOX bboxFilter(QName typeName, Envelope bbox) throws Exception {
        //JD: use "" so that it applies to all geometries
        String name = "";

        if ( bbox instanceof ReferencedEnvelope3D ) {
        	return filterFactory.bbox(name, (ReferencedEnvelope3D) bbox);        
        }         
    
        //get the epsg code
        String epsgCode = null;
        
        if(bbox instanceof SRSEnvelope) {
            SRSEnvelope se = (SRSEnvelope) bbox;
            epsgCode = se.getSrs();
        } else if ( bbox instanceof ReferencedEnvelope ) {
            CoordinateReferenceSystem crs = ((ReferencedEnvelope)bbox).getCoordinateReferenceSystem();
            if ( crs != null ) {
                epsgCode = GML2EncodingUtils.toURI(crs);
            }
        }
            
        return filterFactory.bbox(name, bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY(), epsgCode);
        
    }

    protected void querySet(EObject request, String property, List values)
        throws WFSException {
        //no values specified, do nothing
        if (values == null) {
            return;
        }

        GetFeatureRequest req = GetFeatureRequest.adapt(request);
        
        //handle the name differences in property names between 1.1 and 2.0
        if (req instanceof GetFeatureRequest.WFS20) {
            if ("typeName".equals(property)) {
                property = "typeNames";
            }
            if ("propertyName".equals(property)) {
                property = "abstractProjectionClause";
            }
        }
        
        List query = req.getAdaptedQueries();
        
        int m = values.size();
        int n = query.size();

        if ((m == 1) && (n > 1)) {
            //apply single value to all queries
            EMFUtils.set(query, property, values.get(0));

            return;
        }

        //WfsFactory wfsFactory = (WfsFactory) getFactory();
        //match up sizes
        if (m > n) {
            if (n == 0) {
                //make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    query.add(req.createQuery().getAdaptee());
                }
            } else if (n == 1) {
                //clone single object up to 
                EObject q = (EObject) query.get(0);

                for (int i = 1; i < m; i++) {
                    query.add(EMFUtils.clone(q, req.getFactory()));
                }

                return;
            } else {
                //illegal
                String msg = "Specified " + m + " " + property + " for " + n + " queries.";
                throw new WFSException(request, msg);
            }
        }
        if(m < n) {
            // fill the rest with nulls
            List newValues = new ArrayList<>();
            newValues.addAll(values);
            for (int i = 0; i < n - m; i++) {
                newValues.add(null);
            }
            values = newValues;
        }

        EMFUtils.set(query, property, values);
    }
    
    protected void buildStoredQueries(EObject request, List<URI> storedQueryIds, Map kvp) {
        GetFeatureRequest req = GetFeatureRequest.adapt(request);
        req.getAdaptedQueries();
        
        if (!(req instanceof GetFeatureRequest.WFS20)) {
            throw new WFSException(req, "Stored queries only supported in WFS 2.0+");
        }

        StoredQueryProvider sqp = new StoredQueryProvider(catalog);
        for (URI storedQueryId : storedQueryIds) {
            StoredQuery sq = sqp.getStoredQuery(storedQueryId.toString());
            if (sq == null) {
                throw new WFSException(req, "No such stored query: " + storedQueryId);
            }
    
            //JD: since stored queries are 2.0 only we will create 2.0 model objects directly... once
            // the next version of wfs comes out (and if they keep stored queries around) we will have
            // to abstract stored query away with a request object adapter
            Wfs20Factory factory = (Wfs20Factory) req.getFactory();
            StoredQueryType storedQuery = factory.createStoredQueryType();
            storedQuery.setId(storedQueryId.toString());
            
            //look for parameters in the kvp map
            for (ParameterExpressionType p : sq.getQuery().getParameter()) {
                if (kvp.containsKey(p.getName())) {
                    ParameterType param = factory.createParameterType();
                    param.setName(p.getName());
                    param.setValue(kvp.get(p.getName()).toString());
                    storedQuery.getParameter().add(param);
                }
            }
            
            req.getAdaptedQueries().add(storedQuery);
        }
    }
}
