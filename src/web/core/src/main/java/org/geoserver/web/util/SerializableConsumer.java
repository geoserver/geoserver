/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.io.Serializable;
import java.util.function.Consumer;

public interface SerializableConsumer<T extends Object> extends Consumer<T>, Serializable {}
