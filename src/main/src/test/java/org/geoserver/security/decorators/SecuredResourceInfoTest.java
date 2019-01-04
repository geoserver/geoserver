/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public abstract class SecuredResourceInfoTest<D extends ResourceInfo, S extends ResourceInfo>
        extends GeoServerSystemTestSupport {

    protected final WrapperPolicy policy =
            WrapperPolicy.readOnlyHide(new AccessLimits(CatalogMode.HIDE));

    /**
     * Creates an instance of a non-secure wrapped ResourceInfo.
     *
     * @return An instance of a non-secure wrapped ResourceInfo implementation.
     */
    abstract D createDelegate();

    /**
     * Retrieves the Class of the non-secure wrapped ResourceInfo type.
     *
     * @return the Class of the non-secure wrapped ResourceInfo type.
     */
    abstract Class getDelegateClass();

    /**
     * Wraps a non-Secured ResourceInfo with an appropriate security {@link Wrapper}.
     *
     * @param delegate An instance of the associated non-secure wrapped ResourceInfo type.
     * @return A secured instance wrapping the supplied non-secure ResourceInfo instance.
     */
    abstract S createSecuredDecorator(D delegate);

    /**
     * Retrieves the Class of the secure wrapped ResourceInfo type.
     *
     * @return the Class of the secure wrapped ResourceInfo type.
     */
    abstract Class getSecuredDecoratorClass();

    /**
     * Retrieves the Class of the secure wrapped StoreInfo type associated with the secure wrapped
     * ResourceInfo type.
     *
     * @return the Class of the secure wrapped StoreInfo type associated with the secure wrapped
     *     ResourceInfo type.
     */
    abstract Class getSecuredStoreInfoClass();

    /**
     * Retrieves the minimum number of times a secure {@link Wrapper} needs to re-wrap an object to
     * cause a {@link java.lang.StackOverflowError} when setting a StoreInfo on a ResourceInfo, or
     * when unwrapping nested {@link Wrapper}s.
     *
     * @return the number of nestings required to cause a {@link java.lang.StackOverflowError}.
     */
    abstract int getStackOverflowCount();

    /**
     * Creates a Thread that will repeatedly set the StoreInfo on the target ResourceInfo with the
     * StoreInfo retrieved from the source ResourceInfo. When the source ResourceInfo is a
     * secure-wrapped instance, this process should not continually nest secure {@link Wrapper}s
     * around the StoreInfo instance. If it does, a {@link java.lang.StackOverflowError} could
     * result.
     *
     * @param source A secure wrapped ResourceInfo instance with a non-null StoreInfo attribute.
     * @param target A secure wrapped ResourceInfo instance in which to store the StoreInfo
     *     retrieved from the source.
     * @return A Thread instance that will repeated call target.setStore(source.getStore());
     */
    private Thread getRoundTripThread(final S source, final S target) {
        // This is just a simple thread that will loop a bunch of times copying the info onto
        // itself.
        // If the info is secured, and the copy causes nested Secure wrappings of the data or its
        // attributes, this will
        // eventually throw a StackOverflowError.
        final Runnable runnable =
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < getStackOverflowCount(); ++i) {
                            target.setStore(source.getStore());
                        }
                    }
                };
        // use a very small stack size so the stack overflow happens quickly if it's going to
        // happen.
        // this may not fail on all platforms if set/get is broken however, as some platforms may
        // ignore the stack size in the Thread constructor.
        return new Thread(Thread.currentThread().getThreadGroup(), runnable, "RoundTripThread", 5);
    }

    @Test
    public void testCanSecure() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // secure it
        Object secure = SecuredObjects.secure(delegate, policy);
        assertTrue(
                "Unable to secure ResourceInfo",
                getSecuredDecoratorClass().isAssignableFrom(secure.getClass()));
    }

    @Test
    public void testCanSecureProxied() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // wrap the delegate in a ModificationProxy
        ResourceInfo proxy = ModificationProxy.create(delegate, getDelegateClass());
        // secure it
        Object secure = SecuredObjects.secure(proxy, policy);
        assertTrue(
                "Unable to secure proxied Resourceinfo",
                getSecuredDecoratorClass().isAssignableFrom(secure.getClass()));
    }

    @Test
    public void testSecureWrapping() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // assert the delegate is not secured
        assertFalse(
                "ResourceInfo delegate should not be Secured",
                getSecuredDecoratorClass().isAssignableFrom(delegate.getClass()));
        // create a Secure wrapped instance
        S secured = createSecuredDecorator(delegate);
        assertTrue(
                "ResourceInfo delegate should be Secured",
                getSecuredDecoratorClass().isAssignableFrom(secured.getClass()));
        // get the StoreInfo
        final StoreInfo securedStore = secured.getStore();
        assertTrue(
                "Secured ResourceInfo should return a Secured StoreInfo",
                getSecuredStoreInfoClass().isAssignableFrom(securedStore.getClass()));
        // copy non secured into secured
        Thread roundTripThread = getRoundTripThread(secured, secured);
        // catch Errors
        final StringWriter sw = new StringWriter();
        roundTripThread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        // print the stack to the StringWriter
                        e.printStackTrace(new PrintWriter(sw, true));
                    }
                });
        // start the thread and wait for it to finish
        roundTripThread.start();
        roundTripThread.join();
        // If there was an Error in the thread, the StringWriter will have it
        StringBuffer buffer = sw.getBuffer();
        if (buffer.length() > 0) {
            fail(buffer.toString());
        }
        // just in case, unwrap the StoreInfo and ensure it doesn't throw a StackOverflow
        try {
            SecureCatalogImpl.unwrap(secured.getStore());
        } catch (Throwable t) {
            t.printStackTrace(new PrintWriter(sw, true));
        }
        buffer = sw.getBuffer();
        if (buffer.length() > 0) {
            fail(buffer.toString());
        }
    }
}
