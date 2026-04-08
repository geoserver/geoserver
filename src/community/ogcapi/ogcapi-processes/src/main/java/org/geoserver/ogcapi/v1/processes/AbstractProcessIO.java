/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.RawDataPPIO;
import org.geoserver.wps.process.AbstractRawData;
import org.geotools.api.data.Parameter;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/** A class representing the common parts of process inputs and outputs in the OGC API Processes specification. */
public class AbstractProcessIO {

    private static final Logger LOGGER = Logging.getLogger(AbstractProcessIO.class);
    private static final Schema BBOX_SCHEMA;

    /** Declares the object to have the OGC bbox format, as per OGC API Processes spec */
    public static final String FORMAT_OGC_BBOX = "ogc-bbox";

    static {
        ArraySchema fourCoords = new ArraySchema().items(new NumberSchema());
        fourCoords.minItems(4);
        fourCoords.maxItems(4);

        ArraySchema sixCoords = new ArraySchema().items(new NumberSchema());
        sixCoords.minItems(6);
        sixCoords.maxItems(6);

        ComposedSchema bboxSchema = new ComposedSchema().oneOf(List.of(fourCoords, sixCoords));

        Schema<?> crsSchema = new StringSchema().format("uri");
        crsSchema.setDefault("http://www.opengis.net/def/crs/OGC/1.3/CRS84");

        @SuppressWarnings("unchecked")
        Schema explicitSchema = new ObjectSchema()
                .addProperties("bbox", bboxSchema)
                .addProperties("crs", crsSchema)
                .required(List.of("bbox"));
        Schema<?> bboxFormat = new Schema<>().format(FORMAT_OGC_BBOX);
        BBOX_SCHEMA = new ComposedSchema().allOf(List.of(bboxFormat, explicitSchema));
    }

    String title;
    String description;
    Schema<?> schema;

    public AbstractProcessIO(Parameter<?> p, ApplicationContext context) {
        this.title = p.getTitle().toString();
        this.description = p.getDescription().toString();

        // look up the PPIOs, this should always work
        // TODO: favour JSON like PPIOs over XML ones (re-sort list)... shoud be done in Process
        List<ProcessParameterIO> ppios = ProcessParameterIO.findDecoder(p, context);
        if (ppios.isEmpty()) {
            throw new IllegalArgumentException("Could not find process parameter for type " + p.key + "," + p.type);
        }

        // handle the literal case
        if (ppios.get(0) instanceof LiteralPPIO) {
            this.schema = QueryablesBuilder.getSchema(p.getType());
        } else if (ppios.get(0) instanceof BoundingBoxPPIO) {
            this.schema = BBOX_SCHEMA;
        } else {
            List<Schema> schemas = new ArrayList<>();
            for (ProcessParameterIO ppio : ppios) {
                ComplexPPIO cppio = (ComplexPPIO) ppio;
                try {
                    if (ppio instanceof RawDataPPIO) {
                        String[] mimeTypes = AbstractRawData.getMimeTypes(p);

                        for (String mimeType : mimeTypes) {
                            if (isTextBased(mimeType)) {
                                schemas.add(MimeTypeSchema.text(mimeType));
                            } else {
                                schemas.add(MimeTypeSchema.binary(mimeType));
                            }
                        }
                    } else {
                        String mimeType = cppio.getMimeType();
                        if (cppio instanceof CDataPPIO || isTextBased(mimeType)) {
                            schemas.add(MimeTypeSchema.text(mimeType));
                        } else {
                            schemas.add(MimeTypeSchema.binary(mimeType));
                        }
                    }
                } catch (InvalidMediaTypeException e) {
                    LOGGER.log(Level.FINER, "Skipping invalid mime type for input " + p.getName(), e);
                }
            }
            this.schema = new ComposedSchema().oneOf(schemas);
            this.schema.setTitle(p.getType().getSimpleName());
        }
    }

    /**
     * Heuristically checks if the given mime type is text based. This is used to determine if the schema should be a
     * String or a BinarySchema.
     *
     * @param mime
     * @return
     */
    private static boolean isTextBased(String mime) {
        MediaType mediaType = MediaType.parseMediaType(mime);

        // Check for "text/*"
        if (mediaType.getType().equalsIgnoreCase("text")) {
            return true;
        }

        // Check for known text-based "application/*"
        if (mediaType.getType().equalsIgnoreCase("application")) {
            String subtype = mediaType.getSubtype().toLowerCase();
            return subtype.contains("json")
                    || subtype.contains("gml")
                    || subtype.contains("xml")
                    || subtype.contains("javascript")
                    || subtype.contains("x-www-form-urlencoded")
                    || subtype.contains("yaml")
                    || subtype.contains("csv");
        }

        return false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Schema<?> getSchema() {
        return schema;
    }

    public void setSchema(Schema<?> schema) {
        this.schema = schema;
    }

    @JsonIgnore
    public List<String> getEncodings() {
        if (schema instanceof ComposedSchema composedSchema && composedSchema.getOneOf() != null) {
            return composedSchema.getOneOf().stream()
                    .filter(s -> s instanceof MimeTypeSchema)
                    .map(s -> ((MimeTypeSchema) s).getContentMediaType())
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
