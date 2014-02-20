/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Juha Hyvärinen / Cyberlightning Ltd
 */

package org.geoserver.w3ds.xml3d;

public class XML3DAttribute {
private String attribute;

private String value;

public XML3DAttribute(String name, String value) {
    this.attribute = name;
    this.value = value;
}

public String getAttribute() {
    return attribute;
}

public String getValue() {
    return value;
}

@Override
public String toString() {
    return attribute + "=\"" + value + "\"";
}
}
