/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import org.junit.experimental.categories.Category;

/**
 * Category marker for system test.
 * <p>
 * System test classes should be marked with this interface as an {@link Category}. For example.
 * <pre>
 * <code>
 * {@literal @}Category(SystemTest.class)
 * public class MySystemTest extends GeoServerSystemTest {
 *   ...
 * }
 * </code>
 * </pre>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface SystemTest {

}
