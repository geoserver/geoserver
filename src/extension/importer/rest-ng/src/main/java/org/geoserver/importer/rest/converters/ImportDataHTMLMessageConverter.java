package org.geoserver.importer.rest.converters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.Table;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * {@link BaseMessageConverter} implementation for writing {@link ImportTask} objects to JSON.
 */
@Component
public class ImportDataHTMLMessageConverter extends BaseMessageConverter<ImportData> {

    Importer importer;

    @Autowired
    public ImportDataHTMLMessageConverter(Importer importer) {
        super(MediaType.TEXT_HTML);
        this.importer = importer;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return ImportData.class.isAssignableFrom(clazz);
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
    protected void writeInternal(ImportData data, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        try (OutputStream out = outputMessage.getBody()) {
            OutputStreamWriter outputStream = new OutputStreamWriter(out);
            outputStream.write("<html><body><pre>");

            FlushableJSONBuilder json = new FlushableJSONBuilder(outputStream);
            ImportJSONWriter writer = new ImportJSONWriter(
                    importer);

            Object parent = data.getParent();
            int expand = writer.expand(1);
            
            writer.data( json, data, parent, expand);
            
            if (data instanceof FileData) {
                if (data instanceof Directory) {
                    if (data instanceof Mosaic) {
                        writer.mosaic(json,(Mosaic) data, parent, expand);
                    } else {
                        writer.directory(json, (Directory) data, parent, expand);
                    }
                } else {
                    writer.file(json, (FileData) data, parent, expand, false);
                }
            } else if (data instanceof Database) {
                writer.database(json, (Database) data, parent, expand);
            } else if (data instanceof Table) {
                writer.table(json,(Table) data, parent, expand);
            } else if (data instanceof RemoteData) {
                writer.remote(json, (RemoteData) data, parent, expand);
            } else {
                throw new RestException("Trying to write an unknown object " + data,
                        HttpStatus.I_AM_A_TEAPOT);
            }
            outputStream.write("</pre></body></html>");
            outputStream.flush();
        }
    }
}
