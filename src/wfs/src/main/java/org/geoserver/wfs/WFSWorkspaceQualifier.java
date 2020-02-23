/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Insert;
import org.geoserver.wfs.request.Lock;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.request.Replace;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;

public class WFSWorkspaceQualifier extends WorkspaceQualifyingCallback {

    public WFSWorkspaceQualifier(Catalog catalog) {
        super(catalog);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void qualifyRequest(
            WorkspaceInfo localWorkspace,
            PublishedInfo localLayer,
            Service service,
            Request request) {
        Objects.requireNonNull(localWorkspace);
        Objects.requireNonNull(service);
        Objects.requireNonNull(request);

        if (request.getContext() == null) {
            return;
        }

        final NamespaceInfo ns = catalog.getNamespaceByPrefix(localWorkspace.getName());
        if (ns == null) {
            return;
        }

        final Map<String, Object> kvp = request.getKvp();
        // parameter name differs between WFS 1.x and 2.0
        if (kvp.containsKey("TYPENAME")) { // WFS 1.x
            Collection<QName> typeNames = (Collection<QName>) kvp.get("TYPENAME");
            // if a qualifying workspace exist, try to qualify the request typename/s parameter,
            // if present
            List<QName> qualifiedNames = qualifyTypeNames(request, ns, typeNames);
            kvp.put("TYPENAME", qualifiedNames);
        } else if (kvp.containsKey("TYPENAMES")) { // WFS 2.0
            Collection<Collection<QName>> nestedTypeNames =
                    (Collection<Collection<QName>>) kvp.get("TYPENAMES");

            List<List<QName>> qualifiedNames =
                    nestedTypeNames
                            .stream()
                            .map(l -> qualifyTypeNames(request, ns, l))
                            .collect(Collectors.toList());

            kvp.put("TYPENAMES", qualifiedNames);
        }
    }

    private List<QName> qualifyTypeNames(
            Request request, NamespaceInfo ns, Collection<QName> typeNames) {
        return typeNames
                .stream()
                .map(name -> qualifyTypeName(request, ns, name))
                .collect(Collectors.toList());
    }

    private QName qualifyTypeName(Request request, NamespaceInfo ns, QName typeName) {

        // no namespace specified, we can qualify
        if (Strings.isNullOrEmpty(typeName.getNamespaceURI())) {
            return new QName(ns.getURI(), typeName.getLocalPart());
        }
        if (!typeName.getNamespaceURI().equals(ns.getURI())) {
            // more complex case, if we have the default
            // namespace, we have to check if it's been
            // specified on the request, or assigned by parser
            return checkOriginallyUnqualified(request, ns, typeName);
        }
        return typeName;
    }

    /**
     * Checks if the typeName default namespace is present in the original request, or it has been
     * overridden by parser. If it's been overridden we can qualify with the given namespace.
     *
     * @param request the request context information
     * @param ns the current LocalWorkspace namespace
     * @param typeName the typeName created by a KVP parser
     */
    private QName checkOriginallyUnqualified(Request request, NamespaceInfo ns, QName typeName) {
        @SuppressWarnings("unchecked")
        Map<String, ?> rawKvp = request.getRawKvp(); // keys are case insensitive
        String rawNames = (String) rawKvp.get("TYPENAME");
        if (rawNames == null) {
            rawNames = (String) rawKvp.get("TYPENAMES");
        }
        @SuppressWarnings("unchecked")
        List<String> rawTypeNames = KvpUtils.readFlat(rawNames);
        for (String rawTypeName : rawTypeNames) {
            if (rawTypeName.equals(typeName.getLocalPart())) {
                // the original typeName was not qualified, we can qualify it
                typeName = new QName(ns.getURI(), typeName.getLocalPart());
                break;
            }
        }
        return typeName;
    }

    @Override
    protected void qualifyRequest(
            WorkspaceInfo workspace, PublishedInfo layer, Operation operation, Request request) {
        NamespaceInfo ns = catalog.getNamespaceByPrefix(workspace.getName());

        GetCapabilitiesRequest caps =
                GetCapabilitiesRequest.adapt(
                        OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (caps != null) {
            caps.setNamespace(workspace.getName());
            return;
        }

        DescribeFeatureTypeRequest dft =
                DescribeFeatureTypeRequest.adapt(
                        OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (dft != null) {
            qualifyTypeNames(dft.getTypeNames(), workspace, ns);
            return;
        }

        GetFeatureRequest gf =
                GetFeatureRequest.adapt(
                        OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (gf != null) {
            for (Query q : gf.getQueries()) {
                // in case of stored query usage the typenames might be null
                if (q.getTypeNames() != null) {
                    qualifyTypeNames(q.getTypeNames(), workspace, ns);
                }
            }
            return;
        }

        LockFeatureRequest lf =
                LockFeatureRequest.adapt(
                        OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (lf != null) {
            for (Lock lock : lf.getLocks()) {
                lock.setTypeName(qualifyTypeName(lock.getTypeName(), workspace, ns));
            }
            return;
        }

        TransactionRequest t =
                TransactionRequest.adapt(
                        OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (t != null) {
            for (TransactionElement el : t.getElements()) {
                if (el instanceof Insert) {
                    Insert in = (Insert) el;
                    // in the insert case the objects are gt feature types which are not mutable
                    // so we just check them and throw an exception if a name does not match
                    List features = in.getFeatures();
                    ensureFeatureNamespaceUriMatches(features, ns, t);
                } else if (el instanceof Replace) {
                    Replace rep = (Replace) el;
                    // in the replace case the objects are gt feature types which are not mutable
                    // so we just check them and throw an exception if a name does not match
                    List features = rep.getFeatures();
                    ensureFeatureNamespaceUriMatches(features, ns, t);
                } else {
                    el.setTypeName(qualifyTypeName(el.getTypeName(), workspace, ns));
                }
            }
        }
    }

    /** Iterates the given features and ensures their namespaceURI matches the given namespace */
    private void ensureFeatureNamespaceUriMatches(
            List features, NamespaceInfo ns, TransactionRequest t) {
        for (Iterator j = features.iterator(); j.hasNext(); ) {
            Object next = j.next();
            if (next instanceof Feature) {
                Feature f = (Feature) next;
                Name n = f.getType().getName();
                if (n.getNamespaceURI() != null && !ns.getURI().equals(n.getNamespaceURI())) {
                    throw new WFSException(t, "No such feature type " + n);
                }
            }
        }
    }

    void qualifyTypeNames(List names, WorkspaceInfo ws, NamespaceInfo ns) {
        if (names != null) {
            for (int i = 0; i < names.size(); i++) {
                QName name = (QName) names.get(i);
                names.set(i, qualifyTypeName(name, ws, ns));
            }
        }
    }

    QName qualifyTypeName(QName name, WorkspaceInfo ws, NamespaceInfo ns) {
        if (name != null) {
            return new QName(ns.getURI(), name.getLocalPart(), ws.getName());
        }
        return null;
    }
}
