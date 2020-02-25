/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.impl.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A simple class to support file based stores. Simulates a write lock by creating/removing a
 * physical file on the file system
 *
 * @author Christian
 */
public class LockFile {

    protected long lockFileLastModified;
    protected Resource lockFileTarget, lockFile;

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");

    public LockFile(Resource file) throws IOException {
        lockFileTarget = file;
        if (!Resources.exists(file)) {
            throw new IOException("Cannot lock a not existing file: " + file.path());
        }
        lockFile = file.parent().get(lockFileTarget.name() + ".lock");
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                new Runnable() { // remove on shutdown

                                    @Override
                                    public void run() {
                                        lockFile.delete();
                                    }
                                }));
    }

    /** return true if a write lock is hold by this file watcher */
    public boolean hasWriteLock() throws IOException {
        return Resources.exists(lockFile) && lockFile.lastmodified() == lockFileLastModified;
    }

    /** return true if a write lock is hold by another file watcher */
    public boolean hasForeignWriteLock() throws IOException {
        return Resources.exists(lockFile) && lockFile.lastmodified() != lockFileLastModified;
    }

    /** remove the lockfile */
    public void writeUnLock() {
        if (Resources.exists(lockFile)) {
            if (lockFile.lastmodified() == lockFileLastModified) {
                lockFileLastModified = 0;
                lockFile.delete();
            } else {
                LOGGER.warning("Tried to unlock foreign lock: " + lockFile.path());
            }
        } else {
            LOGGER.warning("Tried to unlock not exisiting lock: " + lockFile.path());
        }
    }

    /** Try to get a lock */
    public void writeLock() throws IOException {

        if (hasWriteLock()) return; // already locked

        if (Resources.exists(lockFile)) {
            LOGGER.warning("Cannot obtain  lock: " + lockFile.path());
            Properties props = new Properties();

            try (InputStream in = lockFile.in()) {
                props.load(in);
            }

            throw new IOException(Util.convertPropsToString(props, "Already locked"));
        } else { // success
            writeLockFileContent(lockFile);
            lockFileLastModified = lockFile.lastmodified();
            LOGGER.info("Successful lock: " + lockFile.path());
        }
    }

    /** Write some info into the lock file hostname, ip, user and lock file path */
    protected void writeLockFileContent(Resource lockFile) throws IOException {

        Properties props = new Properties();
        try (OutputStream out = lockFile.out()) {
            props.store(out, "Locking info");

            String hostname = "UNKNOWN";
            String ip = "UNKNOWN";

            // find some network info
            try {
                hostname = InetAddress.getLocalHost().getHostName();
                InetAddress addrs[] = InetAddress.getAllByName(hostname);
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress())
                        ip = addr.getHostAddress();
                }
            } catch (UnknownHostException ex) {
            }

            props.put("hostname", hostname);
            props.put("ip", ip);
            props.put("location", lockFile.path());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            props.put("principal", auth == null ? "UNKNOWN" : auth.getName());

            props.store(out, "Locking info");
        }
    }
}
