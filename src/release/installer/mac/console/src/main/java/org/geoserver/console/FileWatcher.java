/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Polls a file watching for modifications.
 * 
 * @author Justin Deoliveira, OpenGEO
 * 
 */
public class FileWatcher implements Runnable {

    /**
     * The file being watched.
     */
    protected File file;

    /**
     * last time the file being watched was modified
     */
    protected long lastModified;

    /**
     * last size of the file.
     */
    protected long lastSize;

    /**
     * poll / sleep interval
     */
    long interval = 500;

    /**
     * stop flag
     */
    boolean stopped = false;

    /**
     * buffer for reading content
     */
    byte[] buffer = new byte[1024];

    public FileWatcher() {
        
    }
    
    public void setFile(File file) throws IOException {
        this.file = file;
        lastModified = file.lastModified();
        lastSize = size();
    } 
    
    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void run() {
        while (!stopped) {
            // check the file
            long modified = file.lastModified();
            if (modified != lastModified) {
                // calculate its size
                long size;
                try {
                    size = size();

                    // fire the modified hook
                    modified(modified, size);

                    // update state
                    this.lastModified = modified;
                    this.lastSize = size;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                // sleep for polling interval
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        stopped = true;
    }

    long size() throws IOException {
        FileInputStream in = new FileInputStream(file);
        long size = in.getChannel().size();
        in.close();
        return size;
    }

    void modified(long modified, long size) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        f.skipBytes((int) lastSize);

        int n = (int) (size - lastSize);
        while (n > 0) {
            int r = f.read(buffer, 0, 1024);
            try {
                handleContent(buffer, r);
            } catch (Exception e) {
                // TODO: log this
            }

            n -= r;
        }

        f.close();
    }

    protected void handleContent(byte[] buffer, int n) throws Exception {

    }
}
