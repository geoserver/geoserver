/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
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
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;

public class WFSWorkspaceQualifier extends WorkspaceQualifyingCallback {

    public WFSWorkspaceQualifier(Catalog catalog) {
        super(catalog);
    }

    @Override
    protected void qualifyRequest(WorkspaceInfo workspace, LayerInfo layer, Service service, Request request) {
    }
    
    @Override
    protected void qualifyRequest(WorkspaceInfo workspace, LayerInfo layer, Operation operation, Request request) {
        NamespaceInfo ns = catalog.getNamespaceByPrefix(workspace.getName());
        
        GetCapabilitiesRequest caps = GetCapabilitiesRequest.adapt(
            OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (caps != null) {
            caps.setNamespace(workspace.getName());
            return;
        }
        
        DescribeFeatureTypeRequest dft = DescribeFeatureTypeRequest.adapt(
                OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (dft != null) {
            qualifyTypeNames(dft.getTypeNames(), workspace, ns);
            return;
        }
        
        GetFeatureRequest gf = GetFeatureRequest.adapt(
            OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (gf != null) {
            for (Query q : gf.getQueries()) {
                qualifyTypeNames(q.getTypeNames(), workspace, ns);
            }
            return;
        }
        
        LockFeatureRequest lf = LockFeatureRequest.adapt(
            OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (lf != null) {
            for (Lock lock : lf.getLocks()) {
                lock.setTypeName(qualifyTypeName(lock.getTypeName(), workspace, ns));
            }
            return;
        }
        
        TransactionRequest t = TransactionRequest.adapt(
            OwsUtils.parameter(operation.getParameters(), EObject.class));
        if (t != null) {
            for (TransactionElement el : t.getElements()) {
                if (el instanceof Insert) {
                    Insert in = (Insert) el;
                    //in the insert case the objects are gt feature types which are not mutable
                    // so we just check them and throw an exception if a name does not match
                    for (Iterator j = in.getFeatures().iterator(); j.hasNext(); ) {
                        Feature f = (Feature) j.next();
                        Name n = f.getType().getName();
                        if (n.getNamespaceURI() != null && !ns.getURI().equals(n.getNamespaceURI())) {
                            throw new WFSException(t, "No such feature type " + n);
                        }
                    }
                }
                else {
                    el.setTypeName(qualifyTypeName(el.getTypeName(), workspace, ns));
                }
            }
        }
    }
    
    void qualifyTypeNames(List names, WorkspaceInfo ws, NamespaceInfo ns) {
        for (int i = 0; i < names.size(); i++) {
            QName name = (QName) names.get(i);
            names.set(i, qualifyTypeName(name, ws, ns));
        }
    }
    
    QName qualifyTypeName(QName name, WorkspaceInfo ws, NamespaceInfo ns) {
        return new QName(ns.getURI(), name.getLocalPart(), ws.getName());
    }

}
