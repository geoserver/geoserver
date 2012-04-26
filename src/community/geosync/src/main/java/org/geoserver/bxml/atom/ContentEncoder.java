package org.geoserver.bxml.atom;

import static org.geoserver.gss.internal.atom.Atom.content;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.impl.DeleteElementTypeImpl;
import net.opengis.wfs.impl.InsertElementTypeImpl;
import net.opengis.wfs.impl.UpdateElementTypeImpl;

import org.geoserver.bxml.Encoder;
import org.geoserver.bxml.wfs_1_1.DeleteElementTypeEncoder;
import org.geoserver.bxml.wfs_1_1.InsertElementTypeEncoder;
import org.geoserver.bxml.wfs_1_1.UpdateElementTypeEncoder;
import org.geoserver.gss.internal.atom.Atom;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.gvsig.bxml.stream.BxmlStreamWriter;

public class ContentEncoder extends AbstractAtomEncoder<ContentImpl> {

    private static final QName src = new QName(Atom.NAMESPACE, "src");

    private static final QName type = new QName(Atom.NAMESPACE, "type");

    private static final Encoder<Object> NULL_VALUE_ENCODER = new Encoder<Object>() {
        @Override
        public void encode(Object value, BxmlStreamWriter w) {
            // do nothing
        }
    };

    private static Map<Class<?>, Encoder<?>> encoders = new HashMap<Class<?>, Encoder<?>>();
    static {
        InsertElementTypeEncoder insertEncoder = new InsertElementTypeEncoder();
        UpdateElementTypeEncoder updateEncoder = new UpdateElementTypeEncoder();
        DeleteElementTypeEncoder deleteEncoder = new DeleteElementTypeEncoder();
        Encoder<String> stringEncoder = new Encoder<String>() {
            @Override
            public void encode(String obj, BxmlStreamWriter w) throws IOException {
                w.writeValue(obj);
            }
        };
        encoders.put(InsertElementType.class, insertEncoder);
        encoders.put(InsertElementTypeImpl.class, insertEncoder);
        encoders.put(UpdateElementType.class, updateEncoder);
        encoders.put(UpdateElementTypeImpl.class, updateEncoder);
        encoders.put(DeleteElementType.class, deleteEncoder);
        encoders.put(DeleteElementTypeImpl.class, deleteEncoder);
        encoders.put(String.class, stringEncoder);
    }

    /**
     * @param w
     * @param value
     * @throws XMLStreamException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Override
    public void encode(final ContentImpl c, final BxmlStreamWriter w) throws IOException {
        Encoder<Object> contentEncoder;
        contentEncoder = (Encoder<Object>) ContentEncoder.findEncoderFor(c.getValue());

        startElement(w, content);
        writeAttributes(c, w);
        {
            Object obj = c.getValue();
            contentEncoder.encode(obj, w);
        }
        endElement(w);

    }

    private void writeAttributes(final ContentImpl c, final BxmlStreamWriter w) throws IOException {
        if (c.getSrc() == null && c.getType() == null) {
            return;
        }
        if (c.getSrc() != null) {
            w.writeStartAttribute(src);
            w.writeValue(c.getSrc());
        }
        if (c.getType() != null) {
            w.writeStartAttribute(type);
            if (w.supportsStringTableValues()) {
                long stringTableReference = w.getStringTableReference(c.getType());
                w.writeStringTableValue(stringTableReference);
            } else {
                w.writeValue(c.getType());
            }
        }
        w.writeEndAttributes();
    }

    static Encoder<? extends Object> findEncoderFor(Object value) throws IllegalArgumentException {
        if (null == value) {
            return NULL_VALUE_ENCODER;
        }
        Encoder<? extends Object> encoder = encoders.get(value.getClass());
        if (encoder == null) {
            throw new IllegalArgumentException("No content encoder found for value object of type "
                    + value.getClass().getName() + ". Known encoders: " + encoders.keySet());
        }
        return encoder;
    }

}
