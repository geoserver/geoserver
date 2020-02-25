/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

public abstract class AbstractRawData implements RawData {

    static final Logger LOGGER = Logging.getLogger(AbstractRawData.class);

    protected String mimeType;

    protected String extension = AbstractRawData.DEFAULT_EXTENSION;

    /** The default mime type */
    public static final String BINARY_MIME = "application/octet-stream";

    /** The {@link Parameter} metadata entry listing the mime type for raw data */
    public static final String MIME_TYPES = "mimeTypes";

    /** The default file extension */
    public static final String DEFAULT_EXTENSION = "bin";

    /**
     * The metadata entry pointing at the input attribute that will be filled with the user chosen
     * selection attribute
     */
    public static final String SELECTION_ATTRIBUTE = "chosenMimeType";

    public AbstractRawData(String mimeType) {
        this.mimeType = mimeType;
    }

    public AbstractRawData(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AbstractRawData other = (AbstractRawData) obj;
        if (extension == null) {
            if (other.extension != null) return false;
        } else if (!extension.equals(other.extension)) return false;
        if (mimeType == null) {
            if (other.mimeType != null) return false;
        } else if (!mimeType.equals(other.mimeType)) return false;
        return true;
    }

    /** Extracts the list of mime types from the metadata entry */
    public static String[] getMimeTypes(Parameter<?> p) {
        if (p.metadata != null && p.metadata.get(MIME_TYPES) != null) {
            String mimeTypes = (String) p.metadata.get(MIME_TYPES);
            if (!mimeTypes.trim().isEmpty()) {
                return mimeTypes.split("\\s*,\\s*");
            }
        }
        return new String[] {BINARY_MIME};
    }

    /**
     * Extracts the a map of process input parameters that should be filled with the chosen output
     * mime type for RawData outputs, the map goes from the output result name to the input that
     * will receive the user chosen mime type
     */
    public static Map<String, String> getOutputMimeParameters(Name processName, ProcessFactory pf) {
        Map<String, Parameter<?>> resultInfo = pf.getResultInfo(processName, null);
        Map<String, String> result = new HashMap<String, String>();
        for (Parameter p : resultInfo.values()) {
            if (RawData.class.isAssignableFrom(p.getType())) {
                String attribute = (String) p.metadata.get(SELECTION_ATTRIBUTE);
                if (attribute != null) {
                    if (result.containsValue(attribute)) {
                        LOGGER.warning(
                                "In process "
                                        + processName
                                        + " two raw results parameter are using the same input attribute "
                                        + attribute
                                        + " to notify the process of the user chosen mime type");
                    } else {
                        result.put(p.key, attribute);
                    }
                }
            }
        }

        return result;
    }

    /** Returns the default mime type for the given raw result */
    public static String getDefaultMime(Name processName, ProcessFactory pf, String resultName) {
        Map<String, Parameter<?>> resultInfo = pf.getResultInfo(processName, null);
        Parameter<?> parameter = resultInfo.get(resultName);
        if (parameter == null) {
            LOGGER.warning(
                    "Looked up raw result "
                            + resultName
                            + " in process "
                            + processName
                            + " but found none, returned default mime type");
            return BINARY_MIME;
        }

        return getMimeTypes(parameter)[0];
    }
}
