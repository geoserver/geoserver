/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.Properties;
import org.geotools.util.logging.Logging;

/**
 * An handler is an extension class for the JMS platform which define a set of basic operations:
 *
 * <ul>
 *   <li><b>serialize:</b> {@link JMSEventHandler#serialize(Object)}
 *   <li><b>deserialize:</b> {@link JMSEventHandler#deserialize(Serializable)}
 *   <li><b>synchronize:</b> {@link JMSEventHandler#synchronize(Object)}
 * </ul>
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @param <S> type implementing Serializable
 * @param <O> the type of the object this handler is able to handle
 */
public abstract class JMSEventHandler<S extends Serializable, O> {
    private static final long serialVersionUID = 8208466391619901813L;

    protected static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSEventHandler.class);

    private final Class<JMSEventHandlerSPI<S, O>> generatorClass;

    private Properties properties;

    protected final XStream xstream;
    /**
     * @param xstream an already initialized xstream
     * @param clazz the SPI class which generate this kind of handler
     */
    public JMSEventHandler(final XStream xstream, Class<JMSEventHandlerSPI<S, O>> clazz) {
        this.generatorClass = clazz;
        this.xstream = xstream;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /** @return the generatorClass */
    public final Class<JMSEventHandlerSPI<S, O>> getGeneratorClass() {
        return generatorClass;
    }

    /**
     * Its scope is to serialize from an object of type <O> to instance of a Serializable object.
     * <br>
     * That instance will be used by the {@link JMSPublisher} to send the object over a JMS topic.
     * <br>
     *
     * <p>This method is used exclusively on the Server side.
     *
     * @param o the object of type <O> to serialize
     * @return a serializable object
     */
    public abstract S serialize(O o) throws Exception;

    /**
     * Its scope is to create a new instance of type <O> de-serializing the object of type <S>.<br>
     * That instance will be used by the {@link JMSSynchronizer} to obtain (from the JMS topic) an
     * instance to pass to the synchronize method ( {@link #synchronize(Object)}).<br>
     *
     * <p>This method is used exclusively on the Client side
     *
     * @param o the object of type <O> to serialize
     * @return a serializable object
     */
    public abstract O deserialize(S o) throws Exception;

    /**
     * Its scope is to do something with the deserialized {@link #deserialize(Serializable)} object.
     *
     * <p>This method is used exclusively on the Client side
     *
     * @param deserialized the deserialized object
     * @return a boolean true if the operation ends successfully false otherwise
     * @throws Exception if something goes wrong
     */
    public abstract boolean synchronize(O deserialized) throws Exception;
}
