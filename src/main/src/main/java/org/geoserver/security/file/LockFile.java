/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import org.geoserver.security.impl.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * A simple class to support file based stores.
 * Simulates a write lock by creating/removing a 
 * physical file on the file system
 *
 * @author Christian
 *
 */
public class LockFile  {
    
    protected long lockFileLastModified;
    protected File lockFileTarget,lockFile;
    
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    
    public LockFile(File file) throws IOException{
        lockFileTarget=file;
        if (file.exists()==false) {
            throw new IOException("Cannot lock a not existing file: "+file.getCanonicalPath());
        }              
        lockFile = new File (lockFileTarget.getCanonicalPath()+".lock");
        lockFile.deleteOnExit(); 
    }

    
    /**
     * return true if a write lock is hold by this file watcher 
     * 
     * @return
     * @throws IOException
     */
    public boolean hasWriteLock() throws IOException{        
        return lockFile.exists() && lockFile.lastModified()==lockFileLastModified;
    }

    /**
     * return true if a write lock is hold by another file watcher 
     * 
     * @return
     * @throws IOException
     */
    public boolean hasForeignWriteLock() throws IOException{        
        return lockFile.exists() && lockFile.lastModified()!=lockFileLastModified;
    }
    
    /**
     * remove the lockfile
     * 
     */
    public void writeUnLock() {        
        if (lockFile.exists()) {
            if (lockFile.lastModified()==lockFileLastModified) {
                lockFileLastModified=0;
                lockFile.delete();
            } else {
                LOGGER.warning("Tried to unlock foreign lock: " + lockFile.getAbsolutePath());
            }
        } else {
            LOGGER.warning("Tried to unlock not exisiting lock: " + lockFile.getAbsolutePath());
        }
    }
    
    
    /**
     * Try to get  a lock  
     * 
     * @throws IOException
     */
    public void writeLock() throws IOException{
        
        if (hasWriteLock()) return; // already locked
                        
        if (lockFile.exists()) {             
            LOGGER.warning("Cannot obtain  lock: " + lockFile.getCanonicalPath());
            FileInputStream in = new FileInputStream(lockFile);
            Properties props = new Properties();

            try {
                props.load(in);
            } finally {
                IOUtils.closeQuietly(in);
            }

            throw new IOException(Util.convertPropsToString(props,"Already locked" ));
        } else { // success             
            writeLockFileContent(lockFile);
            lockFileLastModified =lockFile.lastModified();
            lockFile.deleteOnExit(); // remove on shutdown
            LOGGER.info("Successful lock: " + lockFile.getCanonicalPath());
        }
    }
    
    
    /**
     * Write some info into the lock file 
     * hostname, ip, user and lock file path
     * 
     * @param lockFile
     * @throws IOException
     */
    protected void writeLockFileContent(File lockFile) throws IOException {
        
        FileOutputStream out = new FileOutputStream(lockFile); 
        Properties props = new Properties();

        try {
            props.store(out, "Locking info");

            String hostname="UNKNOWN";
            String ip ="UNKNOWN"; 

            // find some network info
            try {
                hostname = InetAddress.getLocalHost().getHostName();
                InetAddress addrs[] = InetAddress.getAllByName(hostname);
                for (InetAddress addr: addrs) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress())
                        ip = addr.getHostAddress();
                }
            } catch (UnknownHostException ex) {
            }

            props.put("hostname", hostname);
            props.put("ip", ip);
            props.put("location", lockFile.getCanonicalPath());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            props.put("principal", auth==null ? "UNKNOWN" :auth.getName());

            props.store(out, "Locking info");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
        
    /**
     * remove the lock file if the garbage
     * collector removes this object
     */
    @Override
    protected void finalize() throws Throwable {
        // check for left locks
        if (lockFile!=null && hasWriteLock()) {
            writeUnLock();
            LOGGER.warning("Unlocking due to garbage collection for "+lockFile.getCanonicalPath());
        }
        try {
            super.finalize();
        } catch (Throwable ex) {
            LOGGER.severe(ex.getMessage());
        }
    }

}
