package org.geoserver.mapml.xml;

/**
 * Common interface for MapML XML elements that can be used in polymorphic collections. This interface helps resolve
 * JAXB compatibility issues with List&lt;Object&gt; in Jakarta JAXB.
 */
public interface MapMLElement {
    // Marker interface - no methods needed
    // This allows JAXB to properly handle type information for collections
}
