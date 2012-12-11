/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
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
    String beanId;

    public String getBeanId() {
        return beanId;
    }

    /**
     * Sets the bean to be filtered out
     * @param beanId
     */
    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    public boolean exclude(String beanId, Object bean) {
        if (this.beanId != null) {
            if (beanId == null)
                return false;
            else
                return this.beanId.equals(beanId);
        }

        return false;
    }

}
