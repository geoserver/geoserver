/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Classes to integrate GWC service requests with the GeoServer {@link org.geoserver.ows.Dispatcher}
 * framework.
 *
 * <p>How the GWC service request processing is modified to engage into the {@link
 * org.geoserver.ows.Dispatcher} life cycle:
 *
 * <ul>
 *   <li>The mapping of incoming requests to {@code /gwc/service/*} to the GWC dispatcher is
 *       commented out in {@code geowebcache-servlet.xml}.
 *   <li>An instance of {@link org.geoserver.ows.OWSHandlerMapping} in {@code
 *       geowebcache-geoserver-context.xml} redirects all requests to {@code /gwc/service/*} to the
 *       GeoSever {@link org.geoserver.ows.Dispatcher} instead of the regular GWC dispatcher.
 *   <li>A GeoServer OWS Service descriptor is configured in {@code
 *       geowebcache-geoserver-context.xml} as a {@link org.geoserver.platform.Service} called
 *       {@code "gwc"}, that defines a {@code "dispatch"} operation and that references the {@link
 *       org.geoserver.gwc.dispatch.GwcServiceProxy} as the actual service bean that perfors that
 *       operation.
 *   <li>A {@link org.geoserver.ows.DispatcherCallback} of type {@link
 *       org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback} intercepts OWS service requests
 *       and if they're directed to one of the GWC provided services, initializes the {@link
 *       org.geoserver.ows.Request} object with "gwc" as the request service and "dispatch" as the
 *       request operation; with this information, the {@link org.geoserver.ows.Dispatcher} will
 *       find out the "mediator" {@link org.geoserver.gwc.dispatch.GwcServiceProxy} and call it's
 *       {@code dispatch} method.
 *   <li>The {@link org.geoserver.gwc.dispatch.GwcServiceProxy#dispatch dispatch} method in turn
 *       redirects the request to the {@link org.geowebcache.GeoWebCacheDispatcher} and returns a
 *       payload {@link org.geoserver.gwc.dispatch.GwcOperationProxy} object; the {@code Dispatcher}
 *       finds out it's to be handled by the configured {@link org.geoserver.ows.Response} object of
 *       type {@link org.geoserver.gwc.dispatch.GwcResponseProxy}.
 * </ul>
 */
package org.geoserver.gwc.dispatch;
