package org.geoserver.wps.gs.download;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The key and value pair
 */
@XmlRootElement(name="Parameter")
public class Parameter {
    @XmlAttribute
    public String key;
    @XmlValue
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String value;

    private Parameter() {
        // private empty constructor required by JAXB
    }

    public Parameter(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
