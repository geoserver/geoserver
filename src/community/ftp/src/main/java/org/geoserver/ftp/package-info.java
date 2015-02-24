/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/**
 * This package contains an FTP Server implementation that's embedded into GeoServer and that allows
 * authenticated users to upload data files (shapefiles, geotiffs) directly to the GeoServer data
 * directory.
 * <p>
 * <H3>Configuration</H3>
 * So far the only configurable properties are whether to enable the FTP server (defaulting
 * to <b>{@code true}</b>) and on what port to run it (defaults to <b>{@code 8021}</b>).
 * This can be done through an {@code ftp.xml} XML file inside the GeoServer data directory,
 * which is automatically created if it does not already exist, and has the following structure:
 * <pre>
 * <code>
 * &lt;ftp&gt;
 *  &lt;enabled&gt;true&lt;/enabled&gt;
 *  &lt;ftpPort&gt;8021&lt;/ftpPort&gt;
 *  &lt;idleTimeout&gt;10&lt;/idleTimeout&gt;
 *  &lt;serverAddress&gt;10.0.1.5&lt;/serverAddress&gt;
 *  &lt;passivePorts&gt;2300:2400&lt;/passivePorts&gt;
 * &lt;/ftp&gt;
 * </code>
 * </pre>
 * Configration parameters:
 * <ul>
 * <li>enabled: &lt;true|false&gt; whether the embedded FTP service is enabled or not
 * <li>ftpPort: &lt;integer {@code > 1023} &gt;, port where to listen for FTP connections
 * <li>idleTimeout: integer, how many seconds to hold an idle connection before automatically close it
 * <li>serverAddress: &lt;all|ip address&gt;, which server ip address to bind the FTP service to. Defaults 
 * to the {@code all} literal meaning to bind the service to all the attached server interfaces.
 * <li>passivePorts: the passive ports to be used for data connections. Ports can be defined as single
 * ports, closed or open ranges.
 * <p>
 * Multiple definitions can be separated by commas, for example:
 * <ul>
 * <li>2300 : only use port 2300 as the passive port</li>
 * <li>2300-2399 : use all ports in the range</li>
 * <li>2300- : use all ports larger than 2300</li>
 * <li>2300, 2305, 2400- : use 2300 or 2305 or any port larger than 2400</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * <p>
 * <H3>Usage</H3>
 * This module doesn't impose any action to be automatically taken upon the user uploaded files.
 * Instead, it provides an extension point in the form of a {@link org.geoserver.ftp.FTPCallback
 * callback interface} to notify interested parties of any file related activity happening through
 * the FTP server.
 * </p>
 * <p>
 * To gather the list of extension point implementations, the normal GeoServer extension point
 * mechanism is used, meaning that a Spring bean implementing the
 * {@link org.geoserver.ftp.FTPCallback} interface must be declared in the application context, like
 * in the following XML snippet:
 * 
 * <pre>
 * <code>
 *   &lt;bean id="ftpLogger" class="org.geoserver.ftp.LoggingFTPCallback"/&gt;
 * </code>
 * </pre>
 * 
 * </p>
 * <p>
 * The {@link org.geoserver.ftp.DefaultFTPCallback} class is an empty implementation of this
 * interface serving as a base class where subclasses can override the methods for the events of
 * interest, like in:
 * 
 * <pre>
 * <code>
 * public class LoggingFTPCallback extends DefaultFTPCallback{
 *  {@code @}Override
 *  public CallbackAction onDeleteEnd(UserDetails user, File workingDir, String fileName) {
 *    LOGGER.fine("User " + user.getName() + " just deleted file " + fileName + " in directory " + workingDir.getAbsolutePath());
 *    return CallbackAction.CONTINUE;
 *  }
 * }
 * </code>
 * </pre>
 */
package org.geoserver.ftp;

