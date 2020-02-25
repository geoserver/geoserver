/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

/** Parses elements common to GetFeature/GetFeatureWithLock/LockFeature (2.0 version) */
public abstract class BaseFeatureKvpRequestReader extends WFSKvpRequestReader {

    /** Catalog used in qname parsing */
    protected Catalog catalog;

    /** Factory used in filter parsing */
    protected FilterFactory filterFactory;

    protected GeoServer geoServer;

    protected final QNameKvpParser qNameParser;

    public BaseFeatureKvpRequestReader(
            Class requestBean, GeoServer geoServer, FilterFactory filterFactory) {
        this(requestBean, WfsFactory.eINSTANCE, geoServer, filterFactory);
    }

    public BaseFeatureKvpRequestReader(
            Class requestBean, EFactory factory, GeoServer geoServer, FilterFactory filterFactory) {
        super(requestBean, factory);
        this.catalog = geoServer.getCatalog();
        this.geoServer = geoServer;
        this.filterFactory = filterFactory;
        qNameParser = new QNameKvpParser("typeName", catalog);
    }

    protected WFSInfo getWFS() {
        return geoServer.getService(WFSInfo.class);
    }

    /**
     * Reads the commons elements to GetFeature, GetFeatureWithLock, LockFeature (typenames,
     * filters, namespaces)
     */
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        // get feature has some additional parsing requirements
        EObject eObject = (EObject) request;

        // make sure the filter is specified in just one way
        ensureMutuallyExclusive(
                kvp,
                new String[] {"featureId", "resourceId", "filter", "bbox", "cql_filter"},
                eObject);

        // did the user supply alternate namespace prefixes?
        NamespaceSupport namespaces = null;
        if (kvp.containsKey("namespace") || kvp.containsKey("namespaces")) {
            if (kvp.get("namespace") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespace");
            } else if (kvp.get("namespaces") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespaces");
            } else {
                LOGGER.warning(
                        "There's a namespace parameter but it seems it wasn't parsed to a "
                                + NamespaceSupport.class.getName()
                                + ": "
                                + kvp.get("namespace"));
            }
        }

        // typeName (in WFS 2.0 it is typeNames, not typeName)
        List<List<QName>> typeNames = null;
        if ((kvp.containsKey("typeName") || kvp.containsKey("typeNames"))
                && !kvp.containsKey("STOREDQUERY_ID")) {
            // HACK, the kvp reader gives us a list of QName, need to wrap in
            // another
            typeNames = (List) kvp.get("typeName");
            if (typeNames == null) {
                typeNames = (List) kvp.get("typeNames");
            }
            List<List<QName>> list = new ArrayList<>();

            for (Iterator itr = typeNames.iterator(); itr.hasNext(); ) {
                Object obj = itr.next();

                // we might get a list of qname, or a list of list of qname
                if (obj instanceof QName) {
                    QName qName = (QName) obj;
                    qName = checkTypeName(qName, namespaces, eObject);

                    List l = new ArrayList();
                    l.add(qName);
                    list.add(l);
                } else {
                    List<QName> qNames = (List<QName>) obj;
                    for (int i = 0; i < qNames.size(); i++) {
                        qNames.set(i, checkTypeName(qNames.get(i), namespaces, eObject));
                    }

                    list.add(qNames);
                }
            }

            kvp.put("typeName", list);
            querySet(eObject, "typeName", list);
            typeNames = list;
        } else {
            // check for featureId and infer typeName
            // in WFS 2.0 it is resourceId
            if (kvp.containsKey("featureId") || kvp.containsKey("resourceId")) {
                // use featureId to infer type Names
                List featureId = (List) kvp.get("featureId");
                featureId = featureId != null ? featureId : (List) kvp.get("resourceId");

                Set<List> hTypeNames = new HashSet<>();
                for (int i = 0; i < featureId.size(); i++) {
                    QName typeName = getTypeNameFromFeatureId((String) featureId.get(i));
                    if (typeName != null) {
                        hTypeNames.add(Arrays.asList(typeName));
                    }
                }

                // remove duplicate typeNames from the list
                List derivedTypeNames = new ArrayList<>(hTypeNames);
                querySet(eObject, "typeName", derivedTypeNames);
            } else {
                // check for stored query id, i have seen both storedQueryId and storedQuery_Id used
                // so support both
                List<URI> storedQueryId = null;
                if (kvp.containsKey("storedQuery_Id")) {
                    storedQueryId = (List<URI>) kvp.get("storedQuery_Id");
                }
                if (storedQueryId == null && kvp.containsKey("storedQueryId")) {
                    storedQueryId = (List<URI>) kvp.get("storedQueryId");
                }
                if (storedQueryId != null) {
                    buildStoredQueries(eObject, storedQueryId, rawKvp);
                } else {
                    throw new WFSException(
                            eObject,
                            "The query should specify either typeName, featureId filter"
                                    + ", or a stored query id",
                            "MissingParameterValue");
                }
            }
        }

        // filter
        if (kvp.containsKey("filter")) {
            querySet(eObject, "filter", (List) kvp.get("filter"));
        } else if (kvp.containsKey("cql_filter")) {
            querySet(eObject, "filter", (List) kvp.get("cql_filter"));
        } else if (kvp.containsKey("featureId") || kvp.containsKey("resourceId")) {
            // set filter from featureId
            List featureIdList = (List) kvp.get("featureId");
            boolean isFeatureId = featureIdList != null;
            featureIdList = isFeatureId ? featureIdList : (List) kvp.get("resourceId");
            Set ids = new HashSet();

            for (Iterator i = featureIdList.iterator(); i.hasNext(); ) {
                String fid = (String) i.next();
                // check consistency between resourceId and typeName (per WFS 2.0 CITE tests)
                if (getWFS().isCiteCompliant() && typeNames != null && !typeNames.isEmpty()) {
                    QName qName = getTypeNameFromFeatureId(fid);
                    if (qName != null) {
                        if (!typeNames
                                .stream()
                                .flatMap(List::stream)
                                .anyMatch(q -> typeNameMatch(qName, q))) {
                            String locator = isFeatureId ? "FEATUREID" : "RESOURCEID";
                            WFSException exception =
                                    new WFSException(
                                            eObject,
                                            "ResourceId is incosistent with " + "typenames");
                            exception.setCode(ServiceException.INVALID_PARAMETER_VALUE);
                            exception.setLocator(locator);
                            throw exception;
                        }
                    }
                }
                FeatureId featureId = filterFactory.featureId(fid);
                ids.add(featureId);
            }
            // build a single feature id filter
            List filters = Collections.singletonList(filterFactory.id(ids));

            querySet(eObject, "filter", filters);
        } else if (kvp.containsKey("bbox")) {
            handleBBOX(kvp, eObject);
        }

        return request;
    }

    protected void handleBBOX(Map kvp, EObject eObject) throws Exception {
        // set filter from bbox
        Envelope bbox = (Envelope) kvp.get("bbox");

        List<Query> queries = getQueries(eObject);
        List filters = new ArrayList();

        for (Iterator<Query> it = queries.iterator(); it.hasNext(); ) {
            Query q = it.next();

            List typeName = q.getTypeNames();
            Filter filter;
            if (typeName.size() > 1) {
                // TODO: not sure what to do here, just going to and them up
                List and = new ArrayList(typeName.size());

                for (Iterator t = typeName.iterator(); t.hasNext(); ) {
                    and.add(bboxFilter(bbox));
                }

                filter = filterFactory.and(and);
            } else {
                filter = bboxFilter(bbox);
            }

            filters.add(filter);
        }

        querySet(eObject, "filter", filters);
    }

    protected List<Query> getQueries(EObject eObject) {
        return GetFeatureRequest.adapt(eObject).getQueries();
    }

    public boolean typeNameMatch(QName maybeUnqualified, QName qualified) {
        return maybeUnqualified.equals(qualified)
                || ((maybeUnqualified.getNamespaceURI() == null
                                || maybeUnqualified.getNamespaceURI().isEmpty())
                        && qualified.getLocalPart().equals(maybeUnqualified.getLocalPart()));
    }

    QName getTypeNameFromFeatureId(String fid) throws Exception {
        int pos = fid.indexOf(".");

        if (pos != -1) {
            String typeName = fid.substring(0, fid.lastIndexOf("."));

            // add to a list to set on the query
            List<QName> parsed = (List) qNameParser.parse(typeName);
            return parsed.get(0);
        } else {
            return null;
        }
    }

    /**
     * Given a set of keys, this method will ensure that no two keys are specified at the same time
     */
    protected void ensureMutuallyExclusive(Map kvp, String[] keys, EObject request) {
        for (int i = 0; i < keys.length; i++) {
            if (kvp.containsKey(keys[i])) {
                for (int j = i + 1; j < keys.length; j++) {
                    if (kvp.containsKey(keys[j])) {
                        String msg =
                                keys[i]
                                        + " and "
                                        + keys[j]
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
                throw new WFSException(
                        "Unknown namespace [" + qName.getPrefix() + "]",
                        "InvalidParameterValue",
                        "namespace");
            }
            prefix = ns.getPrefix();
            qName = new QName(namespaceURI, localPart, prefix);
        }

        if (!XMLConstants.DEFAULT_NS_PREFIX.equals(qName.getPrefix())
                && catalog.getNamespaceByPrefix(qName.getPrefix()) == null) {
            throw new WFSException(
                    "Unknown namespace [" + qName.getPrefix() + "]",
                    "InvalidParameterValue",
                    "namespace");
        }

        if (catalog.getFeatureTypeByName(namespaceURI, localPart) == null) {
            String name = qName.getPrefix() + ":" + qName.getLocalPart();
            throw new WFSException(
                    "Feature type " + name + " unknown", "InvalidParameterValue", "typeName");
        }
        return qName;
    }

    protected BBOX bboxFilter(Envelope bbox) {
        // JD: use "" so that it applies to all geometries
        String name = "";

        if (bbox instanceof ReferencedEnvelope3D) {
            return filterFactory.bbox(name, (ReferencedEnvelope3D) bbox);
        }

        // get the epsg code
        String epsgCode = null;

        if (bbox instanceof SRSEnvelope) {
            SRSEnvelope se = (SRSEnvelope) bbox;
            epsgCode = se.getSrs();
        } else if (bbox instanceof ReferencedEnvelope) {
            CoordinateReferenceSystem crs =
                    ((ReferencedEnvelope) bbox).getCoordinateReferenceSystem();
            if (crs != null) {
                epsgCode = GML2EncodingUtils.toURI(crs);
            }
        }

        return filterFactory.bbox(
                name, bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY(), epsgCode);
    }

    protected abstract void querySet(EObject eObject, String filter, List filters);

    protected abstract void buildStoredQueries(
            EObject eObject, List<URI> storedQueryId, Map rawKvp);
}
