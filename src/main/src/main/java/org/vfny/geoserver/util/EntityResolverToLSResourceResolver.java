/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** A wrapper allowing a EntityResolver to participate in schema validation */
class EntityResolverToLSResourceResolver implements LSResourceResolver {

    static class InputSourceToLSResource implements LSInput {

        private InputSource delegate;

        public InputSourceToLSResource(InputSource is) {
            this.delegate = is;
        }

        @Override
        public void setPublicId(String publicId) {
            delegate.setPublicId(publicId);
        }

        @Override
        public String getPublicId() {
            return delegate.getPublicId();
        }

        @Override
        public void setSystemId(String systemId) {
            delegate.setSystemId(systemId);
        }

        @Override
        public String getSystemId() {
            return delegate.getSystemId();
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            delegate.setByteStream(byteStream);
        }

        @Override
        public InputStream getByteStream() {
            return delegate.getByteStream();
        }

        @Override
        public void setEncoding(String encoding) {
            delegate.setEncoding(encoding);
        }

        @Override
        public String getEncoding() {
            return delegate.getEncoding();
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
            delegate.setCharacterStream(characterStream);
        }

        @Override
        public Reader getCharacterStream() {
            return delegate.getCharacterStream();
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
            // nothing to do

        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
            //
        }

        @Override
        public boolean getCertifiedText() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
            // TODO Auto-generated method stub

        }
    }

    EntityResolver entityResolver;
    LSResourceResolver delegate;

    public EntityResolverToLSResourceResolver(
            LSResourceResolver delegate, EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        this.delegate = delegate;
    }

    @Override
    public LSInput resolveResource(
            String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        // give the entity resolver an opportunity (mostly to throw an exception)
        try {
            InputSource is = entityResolver.resolveEntity(publicId, systemId);
            if (is != null) {
                return new InputSourceToLSResource(is);
            }
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        // otherwise fall back on the default resolution path
        return delegate.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
    }
}
