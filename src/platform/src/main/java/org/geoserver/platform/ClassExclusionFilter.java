/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

/**
 * Black lists a bean by class. By default filters out only the object of the specified class, but
 * it can be configured to match an entire inheritance tree
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class ClassExclusionFilter implements ExtensionFilter {
    private Class beanClass;

    private boolean matchSubclasses;

    public final Class getBeanClass() {
        return beanClass;
    }

    /**
     * Specifies which class to be filtered away
     * @param beanClass
     */
    public void setBeanClass(final Class beanClass) {
        this.beanClass = beanClass;
    }

    public final boolean isMatchSubclasses() {
        return matchSubclasses;
    }

    /**
     * When true enables hierarchy matching. Defaults to false
     * @param matchSubclasses
     */
    public final void setMatchSubclasses(final boolean matchSubclasses) {
        this.matchSubclasses = matchSubclasses;
    }

    @Override
    public boolean exclude(final String beanId, final Object bean) {
        if (this.beanClass != null && bean != null) {
            if(matchSubclasses)
                return this.beanClass.isAssignableFrom(bean.getClass());
            else
                return this.beanClass.equals(bean.getClass());
        }

        return false;
    }
}
