package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.Objects;

public class SmartOverrideEntry implements Serializable {

    private String key;
    private String expression;

    SmartOverrideEntry(String key, String expression) {
        this.key = key;
        this.expression = expression;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmartOverrideEntry)) return false;
        SmartOverrideEntry that = (SmartOverrideEntry) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "SmartOverrideEntry{" + "key='" + key + '\'' + ", expression='" + expression + '\'' + '}';
    }
}
