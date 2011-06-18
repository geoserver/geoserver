package org.geoserver.monitor.ows.wfs;

import javax.xml.namespace.QName;

import org.geoserver.monitor.ows.RequestObjectHandler;

public abstract class WFSRequestObjectHandler extends RequestObjectHandler {

    protected WFSRequestObjectHandler(String reqObjClassName) {
        super(reqObjClassName);
    }

    protected String toString(Object name) {
        if (name instanceof QName) {
            QName qName = (QName) name;
            String prefix = qName.getPrefix();
            if (prefix == null || "".equals(prefix)) {
                prefix = qName.getNamespaceURI();
            }
            if (prefix == null || "".equals(prefix)) {
                prefix = null;
            }

            return prefix != null ? prefix + ":" + qName.getLocalPart() : qName.getLocalPart();
        }
        else {
            return name.toString();
        }
         
    }

}
