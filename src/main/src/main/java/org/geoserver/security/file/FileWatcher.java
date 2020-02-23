/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.file;

import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;

/**
 * This class is based on the concept from the FileWatchDog class in log4j.
 *
 * <p>Objects of this class watch for modifications of files periodically. If a file has changed an
 * action is triggered, this action has to be implemented in a concrete subclass.
 *
 * @author christian
 */
public abstract class FileWatcher implements ResourceListener {

    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    /** The default delay between every file modification check, set to 10 seconds. */
    // static final public long DEFAULT_DELAY = 10000;

    /** The name of the file to observe for changes. */
    protected String path;

    /** The delay to observe between every check. By default set {@link #DEFAULT_DELAY}. */
    // protected long delay = DEFAULT_DELAY;

    Resource resource;

    long lastModified = 0;

    boolean warnedAlready = false;
    boolean terminate = false;
    Object terminateLock = new Object();
    Object lastModifiedLock = new Object();

    /**
     * Check if FileWatcher has been terminated.
     *
     * <p>A terminated FileWatcher no longer listens for resource notification, and will ignore any
     * last minuet notifications that sneak in.
     *
     * @return true if file watcher has been terminated
     */
    public boolean isTerminated() {
        synchronized (terminateLock) {
            return terminate;
        }
    }

    /** Use this method for stopping the thread */
    public void setTerminate(boolean terminated) {
        resource.removeListener(this);
        synchronized (terminateLock) {
            this.terminate = terminated; // will ignore any last minuet events
        }
    }

    protected FileWatcher(Resource resource) {
        this.resource = resource;
        this.path = resource.path();
    }

    /** Used to register FileWatcher as a resource notification listener. */
    public void start() {
        resource.addListener(this);
    }

    @Override
    public void changed(ResourceNotification notify) {
        if (isTerminated()) {
            return; // ignore this event
        }
        doOnChange();
        // checkAndConfigure();
    }

    /** Subclasses must override */
    protected abstract void doOnChange();

    /** Test constellation and call {@link #doOnChange()} if necessary */
    protected void checkAndConfigure() {
        boolean fileExists;
        try {
            fileExists = resource.getType() == Type.RESOURCE;
        } catch (SecurityException e) {
            LOGGER.warning("Was not allowed to read check file existance, file:[" + path + "].");
            setTerminate(true); // there is no point in continuing
            return;
        }

        if (fileExists) {
            long l = resource.lastmodified(); // this can also throw a SecurityException
            if (testAndSetLastModified(l)) { // however, if we reached this point this
                doOnChange(); // is very unlikely.
                warnedAlready = false;
            }
        } else {
            if (!warnedAlready) {
                LOGGER.warning("[" + path + "] does not exist.");
                warnedAlready = true;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    //    public void run() {
    //      while(!isTerminated()) {
    //        try {
    //        Thread.sleep(delay);
    //        } catch(InterruptedException e) {
    //          // no interruption expected
    //        }
    //        checkAndConfigure();
    //      }
    //    }

    /** @return info about the watched file */
    public String getFileInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        StringBuffer buff = new StringBuffer(path);
        buff.append(" last modified: ");
        buff.append(sdf.format(resource.lastmodified()));
        return buff.toString();
    }

    @Override
    public String toString() {
        return "FileWatcher: " + getFileInfo();
    }

    /**
     * Test if l > last modified
     *
     * <p>This extra check is used in conjunction with {@link #setLastModified(long)} to allow the
     * FileWatcher to ignore an event that has been caused by a file update.
     *
     * @return true if file was modified
     */
    public boolean testAndSetLastModified(long l) {
        synchronized (lastModifiedLock) {
            if (l > lastModified) {
                lastModified = l;
                return true;
            }
            return false;
        }
    }

    /**
     * Method intended to set last modified from a client which is up to date. This avoids
     * unnecessary reloads
     */
    public void setLastModified(long lastModified) {
        synchronized (lastModifiedLock) {
            this.lastModified = lastModified;
        }
    }
}
