/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Juha Hyvärinen / Cyberlightning Ltd
 */

package org.geoserver.w3ds.xml3d;

import java.util.ArrayList;

public class XML3DNode {
private String name = null;

private ArrayList<XML3DAttribute> attributes = null;

private ArrayList<XML3DNode> nodes = null;

private String nodeValues = null;

public XML3DNode(String name) {
    this.name = name;
    attributes = new ArrayList<XML3DAttribute>();
    nodes = new ArrayList<XML3DNode>();
}

public XML3DNode() {
    attributes = new ArrayList<XML3DAttribute>();
    nodes = new ArrayList<XML3DNode>();
}

public void addXML3DNode(XML3DNode node) {
    nodes.add(node);
}

public void addXML3DAttribute(XML3DAttribute attribute) {
    attributes.add(attribute);
}

public void addNodeValues(String values) {
    if (nodeValues == null) {
        nodeValues = new String();
    }
    nodeValues += values;
}

public void setName(String name) {
    this.name = name;
}

public String toHtml() {
    // TODO: implement this!

    StringBuilder strb = new StringBuilder();
    strb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
    strb.append("Implementation is not yet ready!");
    strb.append("</html>");
    return strb.toString();
}

@Override
public String toString() {
    StringBuilder strb = new StringBuilder();
    if (name != null) {
        strb.append("<" + name);
        for (XML3DAttribute a : attributes) {
            strb.append(" " + a.toString());
        }
        strb.append(">");

        if (nodeValues != null) {
            strb.append(nodeValues);
        }
        if (!nodes.isEmpty()) {
            for (XML3DNode n : nodes) {
                strb.append(n.toString());
            }
        }
        strb.append("</" + name + ">");
        return strb.toString();
    } else {
        for (XML3DNode n : nodes) {
            strb.append(n.toString());
        }
        return strb.toString();
    }
}
}
