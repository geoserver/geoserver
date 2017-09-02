package org.geoserver.status.monitoring.collector;

public class SystemPropertyValue {

    private String countName;

    private String count;

    private String valueName;

    private String value;

    public SystemPropertyValue(String countName, String count, String valueName, String value) {
        super();
        this.countName = countName;
        this.count = count;
        this.valueName = valueName;
        this.value = value;
    }

    public String getCountName() {
        return countName;
    }

    public void setCountName(String countName) {
        this.countName = countName;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getValueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
