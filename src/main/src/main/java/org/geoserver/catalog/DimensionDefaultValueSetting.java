/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;

/**
 * Setting for dimension default value selection.
 *
 * @author Ilkka Rinne / Spatineo Inc. for Finnish Meteorological Institute
 */
public class DimensionDefaultValueSetting implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 7964431537211881227L;

    public static String TIME_CURRENT = "current";

    public enum Strategy {
        MINIMUM,
        MAXIMUM,
        NEAREST,
        FIXED,
        BUILTIN
    }

    Strategy strategy;

    String referenceValue;

    /** @return the strategyType */
    public Strategy getStrategyType() {
        return strategy;
    }

    /** @param strategyType the strategyType to set */
    public void setStrategyType(Strategy strategyType) {
        this.strategy = strategyType;
    }

    /** @return the referenceValue */
    public String getReferenceValue() {
        return referenceValue;
    }

    /** @param referenceValue the referenceValue to set */
    public void setReferenceValue(String referenceValue) {
        this.referenceValue = referenceValue;
    }
}
