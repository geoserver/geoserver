/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

/**
 * Policy for deciding whether a property should be copied from one bean to another. This is used by
 * {@link OwsUtils#copy(Object, Object, Class, PropertyCopyPolicy)} to decide whether to copy over a certain property or
 * not. The main use case is to allow controlling how to deal with null values (ignore or copy them over), but it can be
 * used for any other custom logic as well.
 */
@FunctionalInterface
public interface PropertyCopyPolicy {

    class DefaultCopyPolicy implements PropertyCopyPolicy {
        @Override
        public boolean shouldCopy(String propertyName, Object source, Object target, Object newValue) {
            return newValue != null;
        }
    }

    /** Default policy that copies all non-null values, and ignores null values (i.e. does not copy them over). */
    PropertyCopyPolicy DEFAULT_POLICY = new DefaultCopyPolicy();

    /**
     * Decide whether the property should be copied.
     *
     * @param propertyName the property name
     * @param source the source bean
     * @param target the target bean
     * @param newValue the value read from the source
     * @return true if the setter should be invoked
     */
    boolean shouldCopy(String propertyName, Object source, Object target, Object newValue);

    /**
     * Optional method to allow mapping the value before copying it. By default it returns the newValue as is.
     *
     * @param propertyName the property name
     * @param source the source bean
     * @param target the target bean
     * @param newValue the value read from the source
     * @return the value to be passed to the setter if shouldCopy returns true
     */
    default Object mapValue(String propertyName, Object source, Object target, Object newValue) {
        return newValue;
    }
}
