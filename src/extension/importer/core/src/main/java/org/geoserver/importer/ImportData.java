/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.IOException;
import java.io.Serializable;
import org.geoserver.importer.job.ProgressMonitor;

/**
 * Base class for all types of data sources that can be imported.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class ImportData implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the format for this data */
    DataFormat format;

    Object parent; // either the task or inputcontext - that owns this data
    String charsetEncoding;

    /** message associated with the data, usually for error reporting. */
    String message;

    public ImportData() {}

    public ImportData(ImportData data) {
        this.format = data.getFormat();
        this.charsetEncoding = data.getCharsetEncoding();
    }

    public String getCharsetEncoding() {
        return charsetEncoding;
    }

    public void setCharsetEncoding(String charsetEncoding) {
        this.charsetEncoding = charsetEncoding;
    }

    public DataFormat getFormat() {
        return format;
    }

    public void setFormat(DataFormat format) {
        this.format = format;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /** Generates a name for this data. */
    public abstract String getName();

    /** Runs any initial checks against the data preparing for import. */
    public final void prepare() throws IOException {
        prepare(new ProgressMonitor());
    }

    /** Runs any initial checks against the data preparing for import. */
    public void prepare(ProgressMonitor monitor) throws IOException {}

    public void cleanup() throws IOException {
        // do nothing
    }

    /**
     * For data types that contain multiple other types, like directories, databases, etc... this
     * method should return the sub part, or simply return itself for data types (liek files) that
     * can't be broken up.
     */
    public ImportData part(String name) {
        return this;
    }

    /** A dummy transfer object to hold properties but has no functionality. */
    public static class TransferObject extends ImportData {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void cleanup() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void prepare(ProgressMonitor m) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    public void reattach() {}

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((charsetEncoding == null) ? 0 : charsetEncoding.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImportData other = (ImportData) obj;
        if (charsetEncoding == null) {
            if (other.charsetEncoding != null) return false;
        } else if (!charsetEncoding.equals(other.charsetEncoding)) return false;
        if (format == null) {
            if (other.format != null) return false;
        } else if (!format.equals(other.format)) return false;
        if (message == null) {
            if (other.message != null) return false;
        } else if (!message.equals(other.message)) return false;
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ImportData [format="
                + format
                + ", charsetEncoding="
                + charsetEncoding
                + ", message="
                + message
                + "]";
    }

    public void setParent(ImportTask task) {
        parent = task;
    }

    public void setParent(ImportContext context) {
        parent = context;
    }

    /** @return the parent */
    public Object getParent() {
        return parent;
    }
}
