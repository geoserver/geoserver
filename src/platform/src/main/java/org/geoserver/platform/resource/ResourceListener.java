/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

/**
 * Listener is notified of changes to configuration resources.
 *
 * <p>These changes are the result of:
 *
 * <ul>
 *   <li>Direct manipulation using {@link Resource#out()}
 *   <li>Indirect editing detected through synchronization (with the local file system or geoserver
 *       cluster)
 * </ul>
 *
 * Listeners are used to register interest in a change to a individual resource or a directory of
 * resources. Receiving a notification indicates that a change has taken place, and is available
 * using {@link Resource#in()}. If you require local file access please use {@link Resource#file()}
 * (or {@link Resource#dir()}) in response to this notification to unpack the change locally.
 *
 * <p>Watch directory contents:
 *
 * <pre>
 * <code>
 * resourceStore.addListener( "styles", new ResourceListener(){
 *    public void changed( ResourceNotification notify ){
 *       boolean resetFonts = false;
 *       for( String path : notify.delta() {
 *          if( path.endswith(".ttf")){
 *             Resource font = resourceStore.get(path);
 *             font.file(); // refresh file locally (if needed)
 *             resetFonts = true;
 *          }
 *       }
 *       if( resetFonts ){
 *         FontCache.getDefaultInstance().resetCache();
 *       }
 *    }
 * });
 * </code>
 * </pre>
 *
 * <p>As shown above, simply watching a directory for changes does not automatically retrieve the
 * changed file. In this case the FontCache needs any TTF fonts unpacked locally before being reset.
 *
 * <p>Example reload on resource change:
 *
 * <pre>
 * <code>
 * resource.addListener( new ResourceListener(){
 *    public void changed( ResourceNotification notify ){
 *       File file = notify.resource().file(); // unpack locally
 *       reload( file );
 *    }
 * });
 * </code>
 * </pre>
 */
public interface ResourceListener {
    public void changed(ResourceNotification notify);
}
