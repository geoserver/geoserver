/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Serializable operation that accepts input with the intension of producing a side-effect rather than a result.
 *
 * @param <T>
 */
public interface SerializableConsumer<T> extends Consumer<T>, Serializable {}
