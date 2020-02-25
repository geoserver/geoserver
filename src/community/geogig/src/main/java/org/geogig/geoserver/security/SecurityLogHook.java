/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import javax.annotation.Nullable;
import org.locationtech.geogig.hooks.CannotRunGeogigOperationException;
import org.locationtech.geogig.hooks.CommandHook;
import org.locationtech.geogig.repository.AbstractGeoGigOp;

/**
 * Classpath {@link CommandHook command hook} that logs remotes related command events to by simply
 * delegating to {@link SecurityLogger}
 *
 * @see SecurityLogger#interestedIn(Class)
 * @see SecurityLogger#logPre(AbstractGeoGigOp)
 * @see SecurityLogger#logPost(AbstractGeoGigOp, Object, RuntimeException)
 */
public class SecurityLogHook implements CommandHook {

    @Override
    public boolean appliesTo(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return SecurityLogger.interestedIn(clazz);
    }

    @Override
    public <C extends AbstractGeoGigOp<?>> C pre(C command)
            throws CannotRunGeogigOperationException {
        SecurityLogger.logPre(command);
        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T post(
            AbstractGeoGigOp<T> command,
            @Nullable Object retVal,
            @Nullable RuntimeException exception)
            throws Exception {
        SecurityLogger.logPost(command, retVal, exception);
        return (T) retVal;
    }
}
