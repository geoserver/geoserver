/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import com.sun.media.jai.util.PropertyUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.media.jai.JAI;
import javax.media.jai.OperationNode;
import javax.media.jai.OperationRegistry;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.PropertySource;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.util.ImagingException;
import javax.media.jai.util.ImagingListener;

/**
 * A thread safe implementation of OperationRegistry using Java 5 Concurrent {@link ReadWriteLock}
 *
 * @author Andrea Aime - GeoSolutions
 */
public final class ConcurrentOperationRegistry extends OperationRegistry {

    static String JAI_REGISTRY_FILE = "META-INF/javax.media.jai.registryFile.jai";

    static String USR_REGISTRY_FILE = "META-INF/registryFile.jai";

    public static OperationRegistry initializeRegistry() {
        try {
            InputStream url = PropertyUtil.getFileFromClasspath(JAI_REGISTRY_FILE);

            if (url == null) {
                throw new RuntimeException("Could not find the main registry file");
            }

            OperationRegistry registry = new ConcurrentOperationRegistry();

            if (url != null) {
                registry.updateFromStream(url);
            }

            registry.registerServices(null);

            return registry;

        } catch (IOException ioe) {
            ImagingListener listener = JAI.getDefaultInstance().getImagingListener();
            String message = "Error occurred while initializing JAI";
            listener.errorOccurred(
                    message, new ImagingException(message, ioe), OperationRegistry.class, false);

            return null;
        }
    }

    /** The reader/writer lock for this class. */
    private ReadWriteLock lock;

    public ConcurrentOperationRegistry() {
        super();

        // Create a concurrent RW lock.
        lock = new ReentrantReadWriteLock();
    }

    public String toString() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.toString();
        } finally {
            readLock.unlock();
        }
    }

    public void writeToStream(OutputStream out) throws IOException {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            super.writeToStream(out);
        } finally {
            readLock.unlock();
        }
    }

    public void initializeFromStream(InputStream in) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.initializeFromStream(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void updateFromStream(InputStream in) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.updateFromStream(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.readExternal(in);
        } finally {
            writeLock.unlock();
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            super.writeExternal(out);
        } finally {
            readLock.unlock();
        }
    }

    public void removeRegistryMode(String modeName) {

        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.removeRegistryMode(modeName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[] getRegistryModes() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getRegistryModes();
        } finally {
            readLock.unlock();
        }
    }

    public void registerDescriptor(RegistryElementDescriptor descriptor) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.registerDescriptor(descriptor);
        } finally {
            writeLock.unlock();
        }
    }

    public void unregisterDescriptor(RegistryElementDescriptor descriptor) {

        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unregisterDescriptor(descriptor);
        } finally {
            writeLock.unlock();
        }
    }

    public RegistryElementDescriptor getDescriptor(Class descriptorClass, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptor(descriptorClass, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public List getDescriptors(Class descriptorClass) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptors(descriptorClass);
        } finally {
            readLock.unlock();
        }
    }

    public String[] getDescriptorNames(Class descriptorClass) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptorNames(descriptorClass);
        } finally {
            readLock.unlock();
        }
    }

    public RegistryElementDescriptor getDescriptor(String modeName, String descriptorName) {

        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptor(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public List getDescriptors(String modeName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptors(modeName);
        } finally {
            readLock.unlock();
        }
    }

    public String[] getDescriptorNames(String modeName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getDescriptorNames(modeName);
        } finally {
            readLock.unlock();
        }
    }

    public void setProductPreference(
            String modeName,
            String descriptorName,
            String preferredProductName,
            String otherProductName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.setProductPreference(
                    modeName, descriptorName, preferredProductName, otherProductName);
        } finally {
            writeLock.unlock();
        }
    }

    public void unsetProductPreference(
            String modeName,
            String descriptorName,
            String preferredProductName,
            String otherProductName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unsetProductPreference(
                    modeName, descriptorName, preferredProductName, otherProductName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearProductPreferences(String modeName, String descriptorName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearProductPreferences(modeName, descriptorName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[][] getProductPreferences(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getProductPreferences(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Vector getOrderedProductList(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getOrderedProductList(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public void registerFactory(
            String modeName, String descriptorName, String productName, Object factory) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.registerFactory(modeName, descriptorName, productName, factory);
        } finally {
            writeLock.unlock();
        }
    }

    public void unregisterFactory(
            String modeName, String descriptorName, String productName, Object factory) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unregisterFactory(modeName, descriptorName, productName, factory);
        } finally {
            writeLock.unlock();
        }
    }

    public void setFactoryPreference(
            String modeName,
            String descriptorName,
            String productName,
            Object preferredOp,
            Object otherOp) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.setFactoryPreference(modeName, descriptorName, productName, preferredOp, otherOp);
        } finally {
            writeLock.unlock();
        }
    }

    public void unsetFactoryPreference(
            String modeName,
            String descriptorName,
            String productName,
            Object preferredOp,
            Object otherOp) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unsetFactoryPreference(
                    modeName, descriptorName, productName, preferredOp, otherOp);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearFactoryPreferences(
            String modeName, String descriptorName, String productName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearFactoryPreferences(modeName, descriptorName, productName);
        } finally {
            writeLock.unlock();
        }
    }

    public Object[][] getFactoryPreferences(
            String modeName, String descriptorName, String productName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactoryPreferences(modeName, descriptorName, productName);
        } finally {
            readLock.unlock();
        }
    }

    public List getOrderedFactoryList(String modeName, String descriptorName, String productName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getOrderedFactoryList(modeName, descriptorName, productName);
        } finally {
            readLock.unlock();
        }
    }

    public Iterator getFactoryIterator(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactoryIterator(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Object getFactory(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getFactory(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public Object invokeFactory(String modeName, String descriptorName, Object[] args) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.invokeFactory(modeName, descriptorName, args);
        } finally {
            readLock.unlock();
        }
    }

    public void addPropertyGenerator(
            String modeName, String descriptorName, PropertyGenerator generator) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.addPropertyGenerator(modeName, descriptorName, generator);
        } finally {
            writeLock.unlock();
        }
    }

    public void removePropertyGenerator(
            String modeName, String descriptorName, PropertyGenerator generator) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.removePropertyGenerator(modeName, descriptorName, generator);
        } finally {
            writeLock.unlock();
        }
    }

    public void copyPropertyFromSource(
            String modeName, String descriptorName, String propertyName, int sourceIndex) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.copyPropertyFromSource(modeName, descriptorName, propertyName, sourceIndex);
        } finally {
            writeLock.unlock();
        }
    }

    public void suppressProperty(String modeName, String descriptorName, String propertyName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.suppressProperty(modeName, descriptorName, propertyName);
        } finally {
            writeLock.unlock();
        }
    }

    public void suppressAllProperties(String modeName, String descriptorName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.suppressAllProperties(modeName, descriptorName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearPropertyState(String modeName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearPropertyState(modeName);
        } finally {
            writeLock.unlock();
        }
    }

    public String[] getGeneratedPropertyNames(String modeName, String descriptorName) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getGeneratedPropertyNames(modeName, descriptorName);
        } finally {
            readLock.unlock();
        }
    }

    public PropertySource getPropertySource(
            String modeName, String descriptorName, Object op, Vector sources) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getPropertySource(modeName, descriptorName, op, sources);
        } finally {
            readLock.unlock();
        }
    }

    public PropertySource getPropertySource(OperationNode op) {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();

            return super.getPropertySource(op);
        } finally {
            readLock.unlock();
        }
    }

    public void registerServices(ClassLoader cl) throws IOException {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.registerServices(cl);
        } finally {
            writeLock.unlock();
        }
    }

    public void unregisterOperationDescriptor(String operationName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.unregisterOperationDescriptor(operationName);
        } finally {
            writeLock.unlock();
        }
    }

    public void clearOperationPreferences(String operationName, String productName) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            super.clearOperationPreferences(operationName, productName);
        } finally {
            writeLock.unlock();
        }
    }
}
