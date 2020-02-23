/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStoreFactorySpi;

/**
 * Performs post processing directly after a {@link DataStoreFactorySpi} is instantiated.
 *
 * <p>Instances of this class are be declared in a spring context. Example:
 *
 * <pre>
 * ShapefileDataStoreFactoryInitializer.java:
 *
 * public class ShapefleDataStoreFactoryInitializer
 *      extends DataStoreFactoryInitializer<ShapefileDataStoreFactory> {
 *
 *      public ShapefleDataStoreFactoryInitializer() {
 *         super( ShapefileDataStoreFactory.class );
 *      }
 *
 *      ...
 *
 *      public void initialize( ShapefileDataStoreFactory factory ) {
 *         //do something here
 *      }
 * }
 *
 * applicationContext.xml:
 *
 * &lt;beans>
 *   &lt;bean id="shapefileDataStoreFactoryInitializer" class="org.geoserver.data.ShapefileDataStoreFactoryInitializer"/>
 * &lt;/beans>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class DataStoreFactoryInitializer<T extends DataAccessFactory> {

    /** the class of the factory instances to initialie */
    protected final Class<T> factoryClass;

    /** Constructs a new initializer. */
    protected DataStoreFactoryInitializer(Class<T> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public final Class<T> getFactoryClass() {
        return factoryClass;
    }

    public abstract void initialize(T factory);
}
