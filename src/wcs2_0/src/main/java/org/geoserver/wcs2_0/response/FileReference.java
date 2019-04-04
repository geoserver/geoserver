/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

class FileReference {

    String reference;

    String mimeType;

    String conformanceClass;

    public FileReference(String reference, String mimeType, String conformanceClass) {
        this.reference = reference;
        this.mimeType = mimeType;
        this.conformanceClass = conformanceClass;
    }

    public String getReference() {
        return reference;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getConformanceClass() {
        return conformanceClass;
    }

    @Override
    public String toString() {
        return "FileReference [reference="
                + reference
                + ", mimeType="
                + mimeType
                + ", conformanceClass="
                + conformanceClass
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((conformanceClass == null) ? 0 : conformanceClass.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + ((reference == null) ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FileReference other = (FileReference) obj;
        if (conformanceClass == null) {
            if (other.conformanceClass != null) return false;
        } else if (!conformanceClass.equals(other.conformanceClass)) return false;
        if (mimeType == null) {
            if (other.mimeType != null) return false;
        } else if (!mimeType.equals(other.mimeType)) return false;
        if (reference == null) {
            if (other.reference != null) return false;
        } else if (!reference.equals(other.reference)) return false;
        return true;
    }
}
