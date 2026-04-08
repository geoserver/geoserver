/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** An interface to represent all possible input values for a process in the OGC API Processes */
public interface InputValue {

    public class LiteralInputValue implements InputValue {
        public Object value;

        public Object getValue() {
            return value;
        }

        public String getString() {
            return value == null ? null : String.valueOf(value);
        }
    }

    public class ArrayInputValue implements InputValue {
        public List<InputValue> values = new ArrayList<>();

        public List<InputValue> getValues() {
            return values;
        }
    }

    public class InlineFileInputValue implements InputValue {
        public String value; // base64 string
        public String mediaType; // MIME type

        public String getValue() {
            return value;
        }

        public String getMediaType() {
            return mediaType;
        }
    }

    public class ComplexJSONInputValue implements InputValue {
        public JsonNode value;

        public JsonNode getValue() {
            return value;
        }
    }

    public class ReferenceInputValue implements InputValue {
        public String href;
        public String type;

        public String getHref() {
            return href;
        }

        public String getType() {
            return type;
        }
    }

    public class BoundingBoxInputValue implements InputValue {
        public List<Double> lowerCorner;
        public List<Double> upperCorner;
        public String crs;

        public List<Double> getLowerCorner() {
            return lowerCorner;
        }

        public List<Double> getUpperCorner() {
            return upperCorner;
        }

        public String getCrs() {
            return crs;
        }
    }
}
