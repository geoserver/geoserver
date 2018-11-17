/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.Filter;

/**
 * Base class for WFS request object adpaters.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class RequestObject {

    /** underlying request object */
    protected EObject adaptee;

    protected RequestObject(EObject adaptee) {
        this.adaptee = adaptee;
    }

    /** The underlying object being adapted. */
    public EObject getAdaptee() {
        return adaptee;
    }

    /** Factory that creates the underlying request model objects. */
    public EFactory getFactory() {
        return adaptee.eClass().getEPackage().getEFactoryInstance();
    }

    //
    // Some common properties that many request objects share
    //

    public String getBaseURL() {
        return getBaseUrl();
    }

    public String getBaseUrl() {
        return eGet(adaptee, "baseUrl", String.class);
    }

    public void setBaseUrl(String baseUrl) {
        eSet(adaptee, "baseUrl", baseUrl);
    }

    public String getVersion() {
        return eGet(adaptee, "version", String.class);
    }

    public boolean isSetService() {
        return eIsSet(adaptee, "service");
    }

    public Map getMetadata() {
        return eGet(adaptee, "metadata", Map.class);
    }

    public void setMetadata(Map metadata) {
        eSet(adaptee, "metadata", metadata);
    }

    public Map getExtendedProperties() {
        return eGet(adaptee, "extendedProperties", Map.class);
    }

    public Map getFormatOptions() {
        return eGet(adaptee, "formatOptions", Map.class);
    }

    public String getHandle() {
        return eGet(adaptee, "handle", String.class);
    }

    public void setHandle(String handle) {
        eSet(adaptee, "handle", handle);
    }

    public QName getTypeName() {
        return eGet(adaptee, "typeName", QName.class);
    }

    public void setTypeName(QName typeName) {
        eSet(adaptee, "typeName", typeName);
    }

    public List<QName> getTypeNames() {
        return eGet(adaptee, "typeName", List.class);
    }

    public void setTypeNames(List<QName> typeNames) {
        List l = eGet(adaptee, "typeName", List.class);
        l.clear();
        l.addAll(typeNames);
    }

    public Filter getFilter() {
        return eGet(adaptee, "filter", Filter.class);
    }

    public void setFilter(Filter filter) {
        eSet(adaptee, "filter", filter);
    }

    public boolean isSetOutputFormat() {
        return eIsSet(adaptee, "outputFormat");
    }

    public String getOutputFormat() {
        return eGet(adaptee, "outputFormat", String.class);
    }

    public void setOutputFormat(String outputFormat) {
        eSet(adaptee, "outputFormat", outputFormat);
    }

    //
    // helpers
    //
    protected <T> T eGet(Object obj, String property, Class<T> type) {
        String[] props = property.split("\\.");
        for (String prop : props) {
            if (obj == null) {
                return null;
            }
            if (!EMFUtils.has((EObject) obj, prop)) {
                return null;
            }
            obj = EMFUtils.get((EObject) obj, prop);
        }
        return (T) obj;
    }

    protected void eSet(Object obj, String property, Object value) {
        String[] props = property.split("\\.");
        for (int i = 0; i < props.length - 1; i++) {
            obj = eGet(obj, props[i], Object.class);
        }

        EMFUtils.set((EObject) obj, props[props.length - 1], value);
    }

    protected void eAdd(Object obj, String property, Object value) {
        EMFUtils.add((EObject) obj, property, value);
    }

    protected boolean eIsSet(Object obj, String property) {
        return EMFUtils.isSet((EObject) obj, property);
    }
}
