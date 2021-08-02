/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.util.Converters;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Helper class to apply partial updates to object of type T, setting values coming from an XML or
 * JSON representation. Support settings null values by using <elementName xsi:nil="true"/> for XML
 * or "attribute"=null for JSON.
 *
 * @param <T> the type of the object to which apply changes.
 */
class PatchMergeHandler<T> {

    private Class<T> patchType;

    PatchMergeHandler(Class<T> patchType) {
        this.patchType = patchType;
    }

    <T> T applyPatch(String patch, T toPatch, String contentType) {
        try {
            if (isJSON(contentType)) patchJSON((JSONObject) JSONSerializer.toJSON(patch), toPatch);
            else patchXML(toXMLDocument(patch), toPatch);
            return toPatch;
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PropertyDescriptor[] getDescriptors() throws IntrospectionException {
        return Introspector.getBeanInfo(patchType).getPropertyDescriptors();
    }

    private Document toXMLDocument(String xmlString) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            // Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            // Parse the content to Document object
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            return doc;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private <T> T patchJSON(JSONObject patch, T toPatch)
            throws InvocationTargetException, IllegalAccessException, IntrospectionException {
        Set keys = patch.keySet();
        PropertyDescriptor[] descriptors = getDescriptors();
        for (Object k : keys) {
            Object o = patch.get(k);
            if (o instanceof JSONObject) {
                patchJSON((JSONObject) o, toPatch);
            } else if (o instanceof JSONArray) {
                patchJSON((JSONArray) o, toPatch);
            } else {
                Optional<PropertyDescriptor> op = beanFieldFromJSON(descriptors, k);
                if (op.isPresent()) {
                    PropertyDescriptor pd = op.get();
                    Object val = patch.get(pd.getName());
                    setNewValue(toPatch, val, pd);
                }
            }
        }
        return toPatch;
    }

    private <T> T patchJSON(JSONArray patch, T toPatch)
            throws InvocationTargetException, IllegalAccessException, IntrospectionException {
        for (int i = 0; i < patch.size(); i++) {
            patchJSON(patch.getJSONObject(i), toPatch);
        }
        return toPatch;
    }

    private Optional<PropertyDescriptor> beanFieldFromJSON(
            PropertyDescriptor[] descriptors, Object fieldName) {
        return Stream.of(descriptors).filter(d -> d.getName().equals(fieldName)).findFirst();
    }

    private <T> T patchXML(Document patch, T toPatch)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        for (PropertyDescriptor pd : getDescriptors()) {
            String fieldName = pd.getName();
            NodeList list = patch.getElementsByTagName(fieldName);
            if (list.getLength() > 0) {
                Node node = list.item(0);
                Object newValue;
                Node nil = node.getAttributes().getNamedItem("xs:nil");
                if (nil != null && nil.getTextContent().equals("true")) newValue = null;
                else newValue = node.getTextContent();
                setNewValue(toPatch, newValue, pd);
            }
        }
        return toPatch;
    }

    private void setNewValue(Object toPatch, Object newValue, PropertyDescriptor pd)
            throws InvocationTargetException, IllegalAccessException {
        newValue = convertNullIfNeeded(newValue);
        if (newValue != null) {
            Class<?> type = pd.getPropertyType();
            newValue = Converters.convert(newValue, type);
        }
        pd.getWriteMethod().invoke(toPatch, newValue);
    }

    private Object convertNullIfNeeded(Object value) {
        if (value == null || value.equals("null") || value.equals("")) value = null;
        return value;
    }

    private boolean isJSON(String contentType) {
        if (contentType.equals(MediaType.APPLICATION_JSON_VALUE)
                || contentType.equals(MediaTypeExtensions.TEXT_JSON_VALUE)) return true;
        return false;
    }
}
