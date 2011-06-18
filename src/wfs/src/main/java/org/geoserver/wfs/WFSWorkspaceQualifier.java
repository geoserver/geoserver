package org.geoserver.wfs;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.ows10.GetCapabilitiesType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.WorkspaceQualifyingCallback;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
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
        
        GetCapabilitiesType caps = 
            (GetCapabilitiesType) OwsUtils.parameter(operation.getParameters(), GetCapabilitiesType.class);
        if (caps != null) {
            caps.setNamespace(workspace.getName());
            return;
        }
        
        DescribeFeatureTypeType dft = 
            (DescribeFeatureTypeType) OwsUtils.parameter(operation.getParameters(), DescribeFeatureTypeType.class);
        if (dft != null) {
            qualifyTypeNames(dft.getTypeName(), workspace, ns);
            return;
        }
        
        GetFeatureType gf = 
            (GetFeatureType) OwsUtils.parameter(operation.getParameters(), GetFeatureType.class);
        if (gf != null) {
            for (QueryType q : (List<QueryType>)gf.getQuery()) {
                qualifyTypeNames(q.getTypeName(), workspace, ns);
            }
            return;
        }
        
        GetFeatureWithLockType gfwl = 
            (GetFeatureWithLockType) OwsUtils.parameter(operation.getParameters(), GetFeatureWithLockType.class);
        if (gfwl != null) {
            for (QueryType q : (List<QueryType>)gfwl.getQuery()) {
                qualifyTypeNames(q.getTypeName(), workspace, ns);
            }
            return;
        }
        
        LockFeatureType lf = 
            (LockFeatureType) OwsUtils.parameter(operation.getParameters(), LockFeatureType.class);
        if (lf != null) {
            for (LockType lock : (List<LockType>)lf.getLock()) {
                lock.setTypeName(qualifyTypeName(lock.getTypeName(), workspace, ns));
            }
            return;
        }
        
        TransactionType t = 
            (TransactionType) OwsUtils.parameter(operation.getParameters(), TransactionType.class);
        if (t != null) {
            for (Iterator i = t.getUpdate().iterator(); i.hasNext(); ) {
                UpdateElementType up = (UpdateElementType) i.next();
                up.setTypeName(qualifyTypeName(up.getTypeName(), workspace, ns));
            }
            for (Iterator i = t.getDelete().iterator(); i.hasNext(); ) {
                DeleteElementType del = (DeleteElementType) i.next();
                del.setTypeName(qualifyTypeName(del.getTypeName(), workspace, ns));
            }
            for (Iterator i = t.getInsert().iterator(); i.hasNext();) {
                InsertElementType in = (InsertElementType) i.next();
                
                //in the insert case the objects are gt feature types which are not mutable
                // so we just check them and throw an exception if a name does not match
                for (Iterator j = in.getFeature().iterator(); j.hasNext(); ) {
                    Feature f = (Feature) j.next();
                    Name n = f.getType().getName();
                    if (n.getNamespaceURI() != null && !ns.getURI().equals(n.getNamespaceURI())) {
                        throw new WFSException("No such feature type " + n);
                    }
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
