/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.Reader;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.geotools.util.Version;

/**
 * Creates a request bean from xml.
 *
 * <p>A request bean is an object which captures the parameters of an operation being requested to a
 * service.
 *
 * <p>An xml request reader must declare the root element of xml documents that it is capable of
 * reading. This is accomplished with {@link #getElement()} and {@link QName#getNamespaceURI()}.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class XmlRequestReader {
    /** the qualified name of the element this reader can read. */
    final QName element;

    /** Appliction specific version number. */
    final Version version;

    /** Service identifier */
    final String serviceId;

    /**
     * Creates the xml reader for the specified element.
     *
     * @param element The qualified name of the element the reader reads.
     */
    public XmlRequestReader(QName element) {
        this(element, null, null);
    }

    /**
     * Creates the xml reader for the specified element.
     *
     * @param namespace The namespace of the element
     * @param local The local name of the element
     */
    public XmlRequestReader(String namespace, String local) {
        this(new QName(namespace, local));
    }

    /**
     * Creates the xml reader for the specified element of a particular version.
     *
     * @param element The qualified name of the element the reader reads.
     * @param version The version of the element in which the reader supports, may be <code>null
     *     </code>.
     */
    public XmlRequestReader(QName element, Version version, String serviceId) {
        this.element = element;
        this.version = version;
        this.serviceId = serviceId;

        if (element == null) {
            throw new NullPointerException("element");
        }
    }

    /** @return The qualified name of the element that this reader reads. */
    public QName getElement() {
        return element;
    }

    /** @return The version of the element that this reader reads. */
    public Version getVersion() {
        return version;
    }

    /**
     * Reads the xml and initializes the request object.
     *
     * <p>The <tt>request</tt> parameter may be <code>null</code>, so in this case the request
     * reader would be responsible for creating the request object, or throwing an exception if this
     * is not supported.
     *
     * <p>In the case of the <tt>request</tt> being non <code>null</code>, the request reader may
     * chose to modify and return <tt>request</tt>, or create a new request object and return it.
     *
     * <p>The <tt>kvp</tt> is used to support mixed style reading of the request object from xml and
     * from a set of key value pairs. This map is often empty.
     */
    public abstract Object read(Object request, Reader reader, Map kvp) throws Exception;

    /**
     * Two XmlReaders considered equal if namespace,element, and version properties are the same.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof XmlRequestReader)) {
            return false;
        }

        XmlRequestReader other = (XmlRequestReader) obj;

        return new EqualsBuilder()
                .append(element, other.element)
                .append(version, other.version)
                .append(serviceId, other.serviceId)
                .isEquals();
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + element + ", " + serviceId + ", " + version + ")";
    }

    /** Implementation of hashcode. */
    public int hashCode() {
        return new HashCodeBuilder().append(element).append(version).append(serviceId).toHashCode();
    }

    public String getServiceId() {
        return serviceId;
    }
}
