/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.logging.Logger;
import org.geotools.util.Version;

/**
 * Parses a key-value pair into a key-object pair.
 *
 * <p>This class is intended to be subclassed. Subclasses need declare the key in which they parse,
 * and the type of object they parse into.
 *
 * <p>Instances need to be declared in a spring context like the following:
 *
 * <pre>
 *         <code>
 *  &lt;bean id="myKvpParser" class="org.xzy.MyKvpParser"/&gt;
 *         </code>
 * </pre>
 *
 * Where <code>com.xzy.MyKvpParser</code> could be something like:
 *
 * <pre>
 *         <code>
 *  public class MyKvpParser extends KvpParser {
 *
 *     public MyKvpParser() {
 *        super( "MyKvp", MyObject.class )l
 *     }
 *
 *     public Object parse( String value ) {
 *        return new MyObject( value );
 *     }
 *  }
 *         </code>
 * </pre>
 *
 * <p><b>Operation Binding</b>
 *
 * <p>In the normal case, a kvp parser is engaged when a request specifies a name which matches the
 * name declared by the kvp parser. It is also possible to attach a kvp parser so that it only
 * engages on a particular operation. This is done by declaring the one or more of the following:
 *
 * <ul>
 *   <li>service
 *   <li>version
 *   <li>request
 * </ul>
 *
 * <p>When a kvp parser declares one or more of these properties, it will only be engaged if an
 * incoming request specicfies matching values of the properties.
 *
 * <p>The following bean declaration would create the above kvp parser so that it only engages when
 * the service is "MyService", and the request is "MyRequest".
 *
 * <pre>
 *         <code>
 *  &lt;bean id="myKvpParser" class="org.xzy.MyKvpParser"&gt;
 *    &lt;property name="service"&gt;MyService&lt;/property&gt;
 *    &lt;property name="request"&gt;MyRequest&lt;/property&gt;
 *  &lt;bean&gt;
 *         </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class KvpParser {
    /** logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.ows");

    /** The key. */
    String key;

    /** The class of parsed objects. */
    Class binding;

    /** The service to bind to */
    String service;

    /** The version of the service to bind to */
    Version version;

    /** The request to bind to */
    String request;

    public KvpParser(String key, Class binding) {
        this.key = key;
        this.binding = binding;
    }

    /** @return The name of the key the parser binds to. */
    public String getKey() {
        return key;
    }

    /** @return The type of parsed objects. */
    protected Class getBinding() {
        return binding;
    }

    /** @return The service to bind to, may be <code>null</code>. */
    public final String getService() {
        return service;
    }

    /** Sets the service to bind to. */
    public final void setService(String service) {
        this.service = service;
    }

    /** @return The version to bind to, or <code>null</code>. */
    public final Version getVersion() {
        return version;
    }

    /** Sets the version to bind to. */
    public final void setVersion(Version version) {
        this.version = version;
    }

    /** Sets the request to bind to. */
    public final void setRequest(String request) {
        this.request = request;
    }

    /** @return The request to bind to, or <code>null</code>. */
    public String getRequest() {
        return request;
    }

    /**
     * Parses the string representation into the object representation.
     *
     * @param value The string value.
     * @return The parsed object, or null if it could not be parsed.
     * @throws Exception In the event of an unsuccesful parse.
     */
    public abstract Object parse(String value) throws Exception;
}
