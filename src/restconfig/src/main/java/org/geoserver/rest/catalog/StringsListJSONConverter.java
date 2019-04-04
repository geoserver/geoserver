/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert a named {@link StringsList} to JSON. */
@Component
public class StringsListJSONConverter extends BaseMessageConverter<StringsList> {

    public StringsListJSONConverter() {
        super(MediaType.APPLICATION_JSON, MediaTypeExtensions.TEXT_JSON);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return StringsList.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(StringsList stringsList, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Map<String, Object> values =
                Collections.singletonMap(
                        "list", Collections.singletonMap("string", stringsList.getValues()));
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
        JSONObject.fromObject(values).write(outWriter);
        outWriter.flush();
        outWriter.close();
    }
}
