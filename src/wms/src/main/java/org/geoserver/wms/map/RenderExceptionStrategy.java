/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.geom.NoninvertibleTransformException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 * A {@link RenderListener} that knows which rendering exceptions are ignorable and stops rendering
 * if not.
 *
 * <p>This map producer should register an instance of this listener to the renderer in order to get
 * notified of an unexpected rendering exception through the {@link #getException()} method.
 *
 * <p>The following exception causes are going to be ignored, any other one will stop the rendering:
 *
 * <ul>
 *   <li>{@link IllegalAttributeException}: known to be thrown when a Feature attribute does not
 *       validate against it's schema.
 *   <li>{@link TransformException}: a geometry can't be transformed to the target CRS, usually
 *       because of being outside the target CRS area of validity.
 *   <li>{@link FactoryException}: the transform from source CRS to destination CRS and from there
 *       to the display failed.
 *   <li>{@link NoninvertibleTransformException}: a transformation error for geometry decimation
 * </ul>
 */
public class RenderExceptionStrategy implements RenderListener {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.wms");
    private final GTRenderer renderer;

    private Exception renderException;

    /**
     * Creates a render listener to stop the given {@code renderer} when a non ignorable exception
     * is notified
     *
     * @param renderer the renderer to {@link GTRenderer#stopRendering() stop} if a non ignorable
     *     exception occurs
     */
    public RenderExceptionStrategy(final GTRenderer renderer) {
        this.renderer = renderer;
        this.renderException = null;
    }

    /**
     * Tells whether a non ignorable exception occurred and hence the rendering process was aborted
     *
     * @return {@code true} if rendering aborted due to an exception, {@code false} otherwise
     */
    public boolean exceptionOccurred() {
        return renderException != null;
    }

    /**
     * @return the non ignorable exception occurred on the rendering loop, or {@code null} if the
     *     renderer finished successfully.
     */
    public Exception getException() {
        return renderException;
    }

    /**
     * Upon a render exception check if its cause is one that we actually want to ignore, and if not
     * abort the rendering process so the map producer can fail.
     */
    public void errorOccurred(final Exception renderException) {

        Throwable cause = renderException;

        while (cause != null) {
            if (cause instanceof TransformException
                    || cause instanceof IllegalAttributeException
                    || cause instanceof FactoryException
                    || cause instanceof NoninvertibleTransformException) {

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Ignoring renderer error", renderException);
                }

                return;
            }
            cause = cause.getCause();
        }

        // not an ignorable cause... stop rendering
        LOGGER.log(Level.FINE, "Got an unexpected render exception.", renderException);
        this.renderException = renderException;
        renderer.stopRendering();
    }

    /**
     * Not used, we're only checking exceptions here
     *
     * @see RenderListener#featureRenderer(SimpleFeature)
     */
    public void featureRenderer(SimpleFeature feature) {
        // intentionally left blank
    }
}
