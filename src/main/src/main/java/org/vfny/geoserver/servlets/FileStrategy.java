/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;

/**
 * A safe ServiceConfig strategy that uses a temporary file until writeTo completes.
 *
 * @author $author$
 * @version $Revision: 1.23 $
 */
public class FileStrategy implements ServiceStrategy {
    public String getId() {
        return "FILE";
    }

    /** Buffer size used to copy safe to response.getOutputStream() */
    private static int BUFF_SIZE = 4096;

    /** Temporary file number */
    static int sequence = 0;

    /** Class logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** OutputStream provided to writeTo method */
    private OutputStream safe;

    /** Temporary file used by safe */
    private File temp;

    /**
     * Provides a outputs stream on a temporary file.
     *
     * <p>I have changed this to use a BufferedWriter to agree with SpeedStrategy.
     *
     * @param response Response being handled
     * @return Outputstream for a temporary file
     * @throws IOException If temporary file could not be created.
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        // REVISIT: Should do more than sequence here
        // (In case we are running two GeoServers at once)
        // - Could we use response.getHandle() in the filename?
        // - ProcessID is traditional, I don't know how to find that in Java
        sequence++;

        // lets check for file permissions first so we can throw a clear error
        try {
            temp = File.createTempFile("wfs" + sequence, "tmp");

            if (!temp.canRead() || !temp.canWrite()) {
                String errorMsg =
                        "Temporary-file permission problem for location: " + temp.getPath();
                throw new IOException(errorMsg);
            }
        } catch (IOException e) {
            String errorMsg = "Possible file permission problem. Root cause: \n" + e.toString();
            IOException newE = new IOException(errorMsg);
            throw newE;
        }

        safe = new BufferedOutputStream(new FileOutputStream(temp));

        return new DispatcherOutputStream(safe);
    }

    /**
     * Closes safe output stream, copies resulting file to response.
     *
     * @throws IOException If temporay file or response is unavailable
     * @throws IllegalStateException if flush is called before getDestination
     */
    public void flush(HttpServletResponse response) throws IOException {
        if ((temp == null) || (response == null) || (safe == null) || !temp.exists()) {
            LOGGER.fine(
                    "temp is "
                            + temp
                            + ", response is "
                            + response
                            + " safe is "
                            + safe
                            + ", temp exists "
                            + (temp == null ? "false" : temp.exists()));
            throw new IllegalStateException("flush should only be called after getDestination");
        }

        InputStream copy = null;

        try {
            safe.flush();
            safe.close();
            safe = null;

            // service succeeded in producing a response!
            // copy result to the real output stream
            copy = new BufferedInputStream(new FileInputStream(temp));

            OutputStream out = response.getOutputStream();
            out = new BufferedOutputStream(out, 1024 * 1024);

            byte[] buffer = new byte[BUFF_SIZE];
            int b;

            while ((b = copy.read(buffer, 0, BUFF_SIZE)) > 0) {
                out.write(buffer, 0, b);
            }

            // Speed Writer closes output Stream
            // I would prefer to leave that up to doService...
            out.flush();

            // out.close();
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (copy != null) {
                try {
                    copy.close();
                } catch (Exception ex) {
                }
            }

            copy = null;

            if ((temp != null) && temp.exists()) {
                temp.delete();
            }

            temp = null;
            response = null;
            safe = null;
        }
    }

    /**
     * Clean up after writeTo fails.
     *
     * @see org.geoserver.ows.ServiceStrategy#abort()
     */
    public void abort() {
        if (safe != null) {
            try {
                safe.close();
            } catch (IOException ioException) {
            }

            safe = null;
        }

        if ((temp != null) && temp.exists()) {
            temp.delete();
        }

        temp = null;
    }

    public Object clone() throws CloneNotSupportedException {
        return new FileStrategy();
    }
}
