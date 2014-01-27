package org.geoserver.catalog;

import java.io.Serializable;

public class DimensionDefaultValueSetting implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 7964431537211881227L;

    public enum Strategy {MINIMUM, MAXIMUM, NEAREST, FIXED}
    
    Strategy strategy;
    
    String referenceValue;

    /**
     * @return the strategyType
     */
    public Strategy getStrategyType() {
        return strategy;
    }

    /**
     * @param strategyType the strategyType to set
     */
    public void setStrategyType(Strategy strategyType) {
        this.strategy = strategyType;
    }

    /**
     * @return the referenceValue
     */
    public String getReferenceValue() {
        return referenceValue;
    }

    /**
     * @param referenceValue the referenceValue to set
     */
    public void setReferenceValue(String referenceValue) {
        this.referenceValue = referenceValue;
    }
 
    
    
}
