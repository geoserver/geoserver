/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Graphics;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.geoserver.wms.WebMap;
import org.geotools.renderer.GTRenderer;

/**
 * An utility class that can be used to set a strict timeout on rendering operations: if the timeout elapses, the
 * renderer will be asked to stop rendering and the graphics will be disposed of to make extra sure the renderer cannot
 * keep going on.
 *
 * @author Andrea Aime - OpenGeo
 */
public class RenderingTimeoutEnforcer {

    ScheduledThreadPoolExecutor executor;
    long timeout;
    GTRenderer renderer;
    Graphics graphics;
    ScheduledFuture<?> timeoutFuture;
    boolean timedOut = false;
    boolean saveMap;
    WebMap map = null;

    public RenderingTimeoutEnforcer(
            long timeout, GTRenderer renderer, Graphics graphics, ScheduledThreadPoolExecutor executor) {
        this(timeout, renderer, graphics, false, executor);
    }

    public RenderingTimeoutEnforcer(
            long timeout,
            GTRenderer renderer,
            Graphics graphics,
            boolean saveMap,
            ScheduledThreadPoolExecutor executor) {
        this.timeout = timeout;
        this.renderer = renderer;
        this.graphics = graphics;
        this.saveMap = saveMap;
        this.executor = executor;
    }

    public void saveMap() {}

    public WebMap getMap() {
        return map;
    }

    /** Starts checking the rendering timeout (if timeout is positive, does nothing otherwise) */
    public void start() {
        if (timeoutFuture != null) {
            throw new IllegalStateException("The timeout enforcer has already been started");
        }

        if (timeout > 0) {
            timedOut = false;
            timeoutFuture = executor.schedule(this::stopRendering, timeout, TimeUnit.MILLISECONDS);
        }
    }

    /** Stops the timeout check */
    public void stop() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    /** Returns true if the renderer has been stopped mid-way due to the timeout occurring */
    public boolean isTimedOut() {
        return timedOut;
    }

    private void stopRendering() {
        timedOut = true;

        if (saveMap) {
            saveMap();
        }

        renderer.stopRendering();
        graphics.dispose();
    }
}
