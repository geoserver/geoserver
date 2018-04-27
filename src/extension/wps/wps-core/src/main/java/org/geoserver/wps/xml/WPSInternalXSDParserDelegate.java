package org.geoserver.wps.xml;

import java.lang.reflect.Field;

import org.geotools.xml.Configuration;
import org.geotools.xml.XSDParserDelegate;
import org.geotools.xml.impl.ParserHandler;
import org.springframework.util.ReflectionUtils;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Extends XSDParserDelegate to make package level ParserHandler accesible via protected getter getHandler()
 * using ReflectionUtils
 * Adds a NamespaceSupport to default handler
 * @author GeoSolutions
 */
public class WPSInternalXSDParserDelegate extends XSDParserDelegate {

    public WPSInternalXSDParserDelegate(Configuration configuration, NamespaceSupport nsSupport) {
        super(configuration);
        getHandler().getNamespaceSupport().add(nsSupport);
    }
    
    protected ParserHandler getHandler(){
        Field field = ReflectionUtils.findField(XSDParserDelegate.class, "handler");
        ReflectionUtils.makeAccessible(field);
        return (ParserHandler)ReflectionUtils.getField(field, this);
    }
    
}
