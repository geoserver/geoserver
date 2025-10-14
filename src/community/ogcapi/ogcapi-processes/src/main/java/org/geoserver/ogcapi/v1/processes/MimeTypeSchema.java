/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Objects;

/** Workaround for Swagger API not supporting the contentMediaType property of the OpenAPI 3.0.0 spec. */
public class MimeTypeSchema extends StringSchema {
    /** The content media type of the schema. */
    String contentMediaType;

    /** Creates a new schema for a binary type. */
    public static MimeTypeSchema binary(String contentMediaType) {
        MimeTypeSchema schema = new MimeTypeSchema(contentMediaType);
        schema.setFormat("binary");
        return schema;
    }

    /** Creates a new schema for a complex text type (e.g., JSON, XML) */
    public static MimeTypeSchema text(String contentMediaType) {
        return new MimeTypeSchema(contentMediaType);
    }

    private MimeTypeSchema(String contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    public String getContentMediaType() {
        return contentMediaType;
    }

    public void setContentMediaType(String contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MimeTypeSchema that = (MimeTypeSchema) o;
        return Objects.equals(contentMediaType, that.contentMediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contentMediaType);
    }
}
