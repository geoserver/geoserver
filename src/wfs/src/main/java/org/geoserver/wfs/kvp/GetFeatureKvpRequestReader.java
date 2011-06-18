/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

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

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.WFSException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.xml.EMFUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.Envelope;


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
        super(requestBean);
        this.catalog = catalog;
        this.filterFactory = filterFactory;
    }
    
    /**
     * Performs additinon GetFeature kvp parsing requirements
     */
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);
        
        // make sure the filter is specified in just one way
        ensureMutuallyExclusive(kvp, new String[] { "featureId", "filter", "bbox", "cql_filter" });

        //get feature has some additional parsing requirements
        EObject eObject = (EObject) request;

        //outputFormat
        if (!EMFUtils.isSet(eObject, "outputFormat")) {
            //set the default
            String version = (String) EMFUtils.get(eObject, "version");

            if ((version != null) && version.startsWith("1.0")) {
                EMFUtils.set(eObject, "outputFormat", "GML2");
            } else {
                EMFUtils.set(eObject, "outputFormat", "text/xml; subtype=gml/3.1.1");
            }
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
        
        //typeName
        if (kvp.containsKey("typeName")) {
            //HACK, the kvp reader gives us a list of QName, need to wrap in 
            // another
            List typeName = (List) kvp.get("typeName");
            List list = new ArrayList();

            for (Iterator itr = typeName.iterator(); itr.hasNext();) {
                QName qName = (QName) itr.next();

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

                List l = new ArrayList();
                l.add(qName);
                list.add(l);
            }

            kvp.put("typeName", list);
            querySet(eObject, "typeName", list);
        } else {
            //check for featureId and infer typeName
            if (kvp.containsKey("featureId")) {
                //use featureId to infer type Names
                List featureId = (List) kvp.get("featureId");

                ArrayList typeNames = new ArrayList();

                QNameKvpParser parser = new QNameKvpParser("typeName", catalog);

                for (int i = 0; i < featureId.size(); i++) {
                    String fid = (String) featureId.get(i);
                    int pos = fid.indexOf(".");

                    if (pos != -1) {
                        String typeName = fid.substring(0, fid.lastIndexOf("."));

                        //add to a list to set on the query
                        List parsed = (List) parser.parse(typeName);
                        typeNames.add(parsed);
                    }
                }

                querySet(eObject, "typeName", typeNames);
            } else {
                throw new WFSException("The query should specify either typeName or a featureId filter", "MissingParameterValue");
            }
        }

        //filter
        if (kvp.containsKey("filter")) {
            querySet(eObject, "filter", (List) kvp.get("filter"));
        } else if (kvp.containsKey("cql_filter")) {
            querySet(eObject, "filter", (List) kvp.get("cql_filter"));
        } else if (kvp.containsKey("featureId")) {
            //set filter from featureId
            List featureIdList = (List) kvp.get("featureId");
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

            List queries = (List) EMFUtils.get(eObject, "query");
            List filters = new ArrayList();

            for (Iterator q = queries.iterator(); q.hasNext();) {
                QueryType query = (QueryType) q.next();
                List typeName = query.getTypeName();
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

        //propertyName
        if (kvp.containsKey("propertyName")) {
            querySet(eObject, "propertyName", (List) kvp.get("propertyName"));
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
        
        if(kvp.containsKey("format_options")) {
            GetFeatureType gft = (GetFeatureType) eObject;
            gft.getFormatOptions().putAll((Map) kvp.get("format_options"));
        }
        
        // sql view params
        if(kvp.containsKey("viewParams")) {
            GetFeatureType gft = (GetFeatureType) eObject;
            if(gft.getMetadata() == null) {
                gft.setMetadata(new HashMap());
            } 
            
            // fan out over all layers if necessary
            List<Map<String, String>> viewParams = (List<Map<String, String>>) kvp.get("viewParams");
            if(viewParams.size() > 0) {
                int layerCount = gft.getQuery().size();
                
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
                    throw new ServiceException(msg, getClass().getName());
                }
            }
            
            gft.getMetadata().put(GetFeature.SQL_VIEW_PARAMS, viewParams);
        }

        return request;
    }

    /**
     * Given a set of keys, this method will ensure that no two keys are specified at the same time
     * @param kvp
     * @param keys
     */
    private void ensureMutuallyExclusive(Map kvp, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            if (kvp.containsKey(keys[i])) {
                for (int j = i + 1; j < keys.length; j++) {
                    if (kvp.containsKey(keys[j])) {
                        String msg = keys[i] + " and " + keys[j]
                            + " both specified but are mutually exclusive";
                        throw new WFSException(msg);
                    }
                }
            }
        }
    }

    BBOX bboxFilter(QName typeName, Envelope bbox) throws Exception {
        FeatureTypeInfo featureTypeInfo = 
            catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        //JD: should this be applied to all geometries?
        //String name = featureType.getDefaultGeometry().getLocalName();
        //JD: changing to "" so it is
        String name = "";
        
        //get the epsg code
        String epsgCode = null;
        if ( bbox instanceof ReferencedEnvelope ) {
            CoordinateReferenceSystem crs = ((ReferencedEnvelope)bbox).getCoordinateReferenceSystem();
            if ( crs != null ) {
                epsgCode = GML2EncodingUtils.crs(crs);
            }
        }
        
        return filterFactory.bbox(name, bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(),
            bbox.getMaxY(), epsgCode);
    }

    protected void querySet(EObject request, String property, List values)
        throws WFSException {
        //no values specified, do nothing
        if (values == null) {
            return;
        }

        List query = (List) EMFUtils.get(request, "query");

        int m = values.size();
        int n = query.size();

        if ((m == 1) && (n > 1)) {
            //apply single value to all queries
            EMFUtils.set(query, property, values.get(0));

            return;
        }

        //match up sizes
        if (m > n) {
            if (n == 0) {
                //make same size, with empty objects
                for (int i = 0; i < m; i++) {
                    query.add(getWfsFactory().createQueryType());
                }
            } else if (n == 1) {
                //clone single object up to 
                EObject q = (EObject) query.get(0);

                for (int i = 1; i < m; i++) {
                    query.add(EMFUtils.clone(q, getWfsFactory()));
                }

                return;
            } else {
                //illegal
                String msg = "Specified " + m + " " + property + " for " + n + " queries.";
                throw new WFSException(msg);
            }
        }

        EMFUtils.set(query, property, values);
    }
}
