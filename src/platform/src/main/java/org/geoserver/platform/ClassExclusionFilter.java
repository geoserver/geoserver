/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

/**
 * Black lists a bean by class. By default filters out only the object of the specified class, but
 * it can be configured to match an entire inheritance tree
 *
 * @author Andrea Aime - OpenGeo
 */
public class ClassExclusionFilter implements ExtensionFilter {
    Class beanClass;

    boolean matchSubclasses;

    public Class getBeanClass() {
        return beanClass;
    }

    /**
     * Specifies which class to be filtered away
     *
     * @param beanClass bean to be filtered
     */
    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    public boolean isMatchSubclasses() {
        return matchSubclasses;
    }

    /** When true enables hierarchy matching. Defaults to false */
    public void setMatchSubclasses(boolean matchSubclasses) {
        this.matchSubclasses = matchSubclasses;
    }

    public boolean exclude(String beanId, Object bean) {
        if (this.beanClass != null && bean != null) {
            if (matchSubclasses) return this.beanClass.isAssignableFrom(bean.getClass());
            else return this.beanClass.equals(bean.getClass());
        }

        return false;
    }
}
