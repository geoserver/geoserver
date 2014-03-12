/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

/**
 * Black lists a bean by bean id.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class NameExclusionFilter implements ExtensionFilter {
    private String beanId;

    public final String getBeanId() {
        return beanId;
    }

    /**
     * Sets the bean to be filtered out
     * @param beanId
     */
    public final void setBeanId(final String beanId) {
        this.beanId = beanId;
    }

    @Override
    public final boolean exclude(final String beanId, final Object bean) {
        if (this.beanId != null) {
            if (beanId == null) {
                return false;
            } else {
                return this.beanId.equals(beanId);
            }
        }

        return false;
    }

}
