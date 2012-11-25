/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.xml.EMFUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

public class TransactionHandler extends WFSRequestObjectHandler {

    public TransactionHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs.TransactionType", config, catalog);
    }

    @Override
    public void handle(Object request, RequestData data) {
        super.handle(request, data);
        
        //also determine the sub operation
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject)request, "group");
        if (elements == null) {
            return;
        }
        
        ListIterator<Object> i = elements.valueListIterator();
        int flag = 0;
        while(i.hasNext()) {
            Object e = i.next();
            if (e.getClass().getSimpleName().startsWith("Insert")) {
                flag |= 1;
            }
            else if (e.getClass().getSimpleName().startsWith("Update")) {
                flag |= 2;
            }
            else if (e.getClass().getSimpleName().startsWith("Delete")) {
                flag |= 4;
            }
            else {
                flag |= 8;
            }
        }
        
        StringBuffer sb = new StringBuffer();
        if ((flag & 1) == 1) sb.append("I");
        if ((flag & 2) == 2) sb.append("U");
        if ((flag & 4) == 4) sb.append("D");
        if ((flag & 8) == 8) sb.append("O");
        data.setSubOperation(sb.toString());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject)request, "group");
        if (elements == null) {
            return null;
        }
        
        List<String> layers = new ArrayList<String>();
        ListIterator<Object> i = elements.valueListIterator();
        while(i.hasNext()) {
            Object e = i.next();
            if (EMFUtils.has((EObject)e, "typeName")) {
                Object typeName = EMFUtils.get((EObject)e, "typeName");
                if (typeName != null) {
                    layers.add(toString(typeName));
                }
            }
            else {
                //this is most likely an insert, determine layers from feature collection
                if (e.getClass().getSimpleName().startsWith("InsertElementType")) {
                    List<Feature> features = (List<Feature>) EMFUtils.get((EObject)e, "feature");
                    Set<String> set = new LinkedHashSet<String>();
                    for (Feature f : features) {
                        if (f instanceof SimpleFeature) {
                            set.add(((SimpleFeature)f).getType().getTypeName());
                        }
                        else {
                            set.add(f.getType().getName().toString());
                        }
                    }
                    
                    layers.addAll(set);
                }
            }
        }
        
        return layers;
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> getElements(Object request) {
        return (List<Object>) OwsUtils.get(request, "group");
    }
    
    @Override
    protected Object unwrapElement(Object element){
        // For some reason it's wrapped inside an extra EMF object here but not in the other 
        // request types 
        return OwsUtils.get(element, "value");
    }
}
