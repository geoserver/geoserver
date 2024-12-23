/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.xml.sax.SAXException;

/**
 * Creates a request bean from xml.
 *
 * <p>A request bean is an object which captures the parameters of an operation being requested to a service.
 *
 * <p>An xml request reader must declare the root element of xml documents that it is capable of reading. This is
 * accomplished with {@link #getElement()} and {@link QName#getNamespaceURI()}.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class XmlRequestReader {
    /** Logger for XmlRequestReader subclass established in constructor for logging of parse errors. */
    final Logger LOGGER;

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

        LOGGER = Logging.getLogger(this.getClass());

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
     * <p>The <tt>request</tt> parameter may be <code>null</code>, so in this case the request reader would be
     * responsible for creating the request object, or throwing an exception if this is not supported.
     *
     * <p>In the case of the <tt>request</tt> being non <code>null</code>, the request reader may chose to modify and
     * return <tt>request</tt>, or create a new request object and return it.
     *
     * <p>The <tt>kvp</tt> is used to support mixed style reading of the request object from xml and from a set of key
     * value pairs. This map is often empty.
     */
    public abstract Object read(Object request, Reader reader, Map kvp) throws Exception;

    /** Two XmlReaders considered equal if namespace,element, and version properties are the same. */
    @Override
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
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(element)
                .append(version)
                .append(serviceId)
                .toHashCode();
    }

    public String getServiceId() {
        return serviceId;
    }

    /**
     * Wrap a request parsing failure in a SAXException to prevent generic FileNotFound and ConnectionRefused messages.
     *
     * <p>This communicates that there is a problem with the XML Document while avoiding providing internal details that
     * are not useful to web service users.
     *
     * <p>The full stack trace is preserved to support development and debugging; so the line number can be used by
     * developers.
     *
     * @param t Failure during parsing (such as SAXException or IOException)
     * @return Clean SAXException for common parsing failures, or initial throwable
     */
    protected Exception cleanException(Exception t) {
        if (t instanceof IOException) {
            return createParseException(t);
        }
        if (t instanceof SAXException) {
            // Double check SAXException does not echo caused by message
            return cleanSaxException((SAXException) t);
        }
        return t;
    }

    /**
     * Clean the localized message, in the case where it is reproduced by SAXException default constructor.
     *
     * @param saxException
     * @return saxException with nested localized message removed.
     */
    protected SAXException cleanSaxException(SAXException saxException) {
        Throwable cause = saxException.getCause();
        // We only wish to check SAXException which echos internal caused by message
        // Subclasses such as SAXParserException provide a useful message
        if (saxException != null && saxException.getCause() != null && saxException.getClass() == SAXException.class) {
            String saxMessage = saxException.getMessage();
            String causeMessage = cause.getLocalizedMessage();
            if (causeMessage != null && saxMessage.contains(causeMessage)) {
                return createParseException(saxException);
            }
        }
        return saxException;
    }

    /**
     * Log the cause, and return a SAXException indicaitng a parse failure.
     *
     * <p>This is a replacement for the provided cause, and includes the same stack trace to assist with troubleshooting
     * and debugging.
     *
     * @param cause
     * @return SAXException indicating parse failure
     */
    protected SAXException createParseException(Throwable cause) {
        // Log actual failure for debugging and troubleshooting
        String requestFailure = "XML " + getElement().getLocalPart() + " Parsing Failure: ";
        LOGGER.info(requestFailure + cause.toString());

        // Provide clean SAXException message, keep stacktrace history (for verbose service
        // exception document)
        String cleanMessage = "Parsing failed, the xml request is most probably not compliant to "
                + getElement().getLocalPart()
                + " element";
        SAXException saxException = new SAXException(cleanMessage);
        saxException.setStackTrace(cause.getStackTrace());
        return saxException;
    }
}
