/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geoserver.backuprestore.reader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.config.util.XStreamPersister;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxUtils;
import org.springframework.batch.item.xml.stax.DefaultFragmentEventReader;
import org.springframework.batch.item.xml.stax.FragmentEventReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Item reader for reading XML input based on StAX.
 *
 * <p>It extracts fragments from the input XML document which correspond to records for processing.
 * The fragments are wrapped with StartDocument and EndDocument events so that the fragments can be
 * further processed like standalone XML documents.
 *
 * <p>The implementation is <b>not</b> thread-safe.
 *
 * <p>Code based on original {@link StaxEventItemReader} by Robert Kasanicky.
 *
 * @author Robert Kasanicky
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogFileReader<T> extends CatalogReader<T> {

    private static final Log logger = LogFactory.getLog(CatalogFileReader.class);

    private FragmentEventReader fragmentReader;

    private XMLEventReader eventReader;

    private Resource resource;

    private InputStream inputStream;

    private List<QName> fragmentRootElementNames;

    private boolean noInput;

    private boolean strict = true;

    public CatalogFileReader(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }

    /**
     * In strict mode the reader will throw an exception on {@link
     * #open(org.springframework.batch.item.ExecutionContext)} if the input resource does not exist.
     *
     * @param strict false by default
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /** @param fragmentRootElementName name of the root element of the fragment */
    public void setFragmentRootElementName(String fragmentRootElementName) {
        setFragmentRootElementNames(new String[] {fragmentRootElementName});
    }

    /** @param fragmentRootElementNames list of the names of the root element of the fragment */
    public void setFragmentRootElementNames(String[] fragmentRootElementNames) {
        this.fragmentRootElementNames = new ArrayList<QName>();
        for (String fragmentRootElementName : fragmentRootElementNames) {
            this.fragmentRootElementNames.add(
                    parseFragmentRootElementName(fragmentRootElementName));
        }
    }

    /**
     * Ensure that all required dependencies for the ItemReader to run are provided after all
     * properties have been set.
     *
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     * @throws IllegalArgumentException if the Resource, FragmentDeserializer or
     *     FragmentRootElementName is null, or if the root element is empty.
     * @throws IllegalStateException if the Resource does not exist.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(fragmentRootElementNames, "The FragmentRootElementNames must not be empty");
        for (QName fragmentRootElementName : fragmentRootElementNames) {
            Assert.hasText(
                    fragmentRootElementName.getLocalPart(),
                    "The FragmentRootElementNames must not contain empty elements");
        }
    }

    /**
     * Responsible for moving the cursor before the StartElement of the fragment root.
     *
     * <p>This implementation simply looks for the next corresponding element, it does not care
     * about element nesting. You will need to override this method to correctly handle composite
     * fragments.
     *
     * @return <code>true</code> if next fragment was found, <code>false</code> otherwise.
     * @throws NonTransientResourceException if the cursor could not be moved. This will be treated
     *     as fatal and subsequent calls to read will return null.
     */
    protected boolean moveCursorToNextFragment(XMLEventReader reader) {
        try {
            while (true) {
                while (reader.peek() != null && !reader.peek().isStartElement()) {
                    reader.nextEvent();
                }
                if (reader.peek() == null) {
                    return false;
                }
                QName startElementName = ((StartElement) reader.peek()).getName();
                if (isFragmentRootElementName(startElementName)) {
                    return true;
                }
                reader.nextEvent();
            }
        } catch (XMLStreamException e) {
            return logValidationExceptions(
                    (T) null,
                    new NonTransientResourceException("Error while reading from event reader", e));
        }
    }

    @Override
    protected void doClose() throws Exception {
        try {
            if (fragmentReader != null) {
                fragmentReader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException | XMLStreamException e) {
            logValidationExceptions((T) null, e);
        } finally {
            fragmentReader = null;
            inputStream = null;
        }
    }

    @Override
    protected void doOpen() throws Exception {
        Assert.notNull(resource, "The Resource must not be null.");

        try {
            noInput = true;
            if (!resource.exists()) {
                if (strict) {
                    throw new IllegalStateException(
                            "Input resource must exist (reader is in 'strict' mode)");
                }
                logger.warn("Input resource does not exist " + resource.getDescription());
                return;
            }
            if (!resource.isReadable()) {
                if (strict) {
                    throw new IllegalStateException(
                            "Input resource must be readable (reader is in 'strict' mode)");
                }
                logger.warn("Input resource is not readable " + resource.getDescription());
                return;
            }

            inputStream = resource.getInputStream();
            eventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
            fragmentReader = new DefaultFragmentEventReader(eventReader);
            noInput = false;
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    /** Move to next fragment and map it to item. */
    @Override
    protected T doRead() throws Exception {
        T item = null;
        try {
            if (noInput) {
                return null;
            }

            boolean success = false;
            try {
                success = moveCursorToNextFragment(fragmentReader);
            } catch (NonTransientResourceException e) {
                // Prevent caller from retrying indefinitely since this is fatal
                noInput = true;
                throw e;
            }
            if (success) {
                fragmentReader.markStartFragment();

                try {
                    @SuppressWarnings("unchecked")
                    T mappedFragment = (T) unmarshal(StaxUtils.getSource(fragmentReader));
                    item = mappedFragment;
                    try {
                        firePostRead(item, resource);
                    } catch (IOException e) {
                        logValidationExceptions(
                                (ValidationResult) null,
                                new UnexpectedInputException(
                                        "Could not write data.  The file may be corrupt.", e));
                    }
                } finally {
                    fragmentReader.markFragmentProcessed();
                }
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }

        return item;
    }

    /**
     * This is the core of the Class. The XML fragment will be unmarshalled via the GeoServer {@link
     * XStreamPersister}.
     */
    private Object unmarshal(Source source)
            throws TransformerException, XMLStreamException, UnsupportedEncodingException {
        TransformerFactory tf =
                new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();
        Transformer t = tf.newTransformer();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Result result = new StreamResult(os);
        t.transform(source, result);
        return this.getXp().fromXML(new ByteArrayInputStream(os.toByteArray()));
    }

    /*
     * jumpToItem is overridden because reading in and attempting to bind an entire fragment is unacceptable in a restart scenario, and may cause
     * exceptions to be thrown that were already skipped in previous runs.
     */
    @Override
    protected void jumpToItem(int itemIndex) throws Exception {
        for (int i = 0; i < itemIndex; i++) {
            try {
                QName fragmentName = readToStartFragment();
                readToEndFragment(fragmentName);
            } catch (NoSuchElementException e) {
                if (itemIndex == (i + 1)) {
                    // we can presume a NoSuchElementException on the last item means the EOF was
                    // reached on the last run
                    return;
                } else {
                    // if NoSuchElementException occurs on an item other than the last one, this
                    // indicates a problem
                    logValidationExceptions((T) null, e);
                }
            }
        }
    }

    /*
     * Read until the first StartElement tag that matches any of the provided fragmentRootElementNames. Because there may be any number of tags in
     * between where the reader is now and the fragment start, this is done in a loop until the element type and name match.
     */
    private QName readToStartFragment() throws XMLStreamException {
        while (true) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()
                    && isFragmentRootElementName(((StartElement) nextEvent).getName())) {
                return ((StartElement) nextEvent).getName();
            }
        }
    }

    /*
     * Read until the first EndElement tag that matches the provided fragmentRootElementName. Because there may be any number of tags in between where
     * the reader is now and the fragment end tag, this is done in a loop until the element type and name match
     */
    private void readToEndFragment(QName fragmentRootElementName) throws XMLStreamException {
        while (true) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isEndElement()
                    && fragmentRootElementName.equals(((EndElement) nextEvent).getName())) {
                return;
            }
        }
    }

    private boolean isFragmentRootElementName(QName name) {
        for (QName fragmentRootElementName : fragmentRootElementNames) {
            if (fragmentRootElementName.getLocalPart().equals(name.getLocalPart())) {
                if (!StringUtils.hasText(fragmentRootElementName.getNamespaceURI())
                        || fragmentRootElementName
                                .getNamespaceURI()
                                .equals(name.getNamespaceURI())) {
                    return true;
                }
            }
        }
        return false;
    }

    private QName parseFragmentRootElementName(String fragmentRootElementName) {
        String name = fragmentRootElementName;
        String nameSpace = null;
        if (fragmentRootElementName.contains("{")) {
            nameSpace = fragmentRootElementName.replaceAll("\\{(.*)\\}.*", "$1");
            name = fragmentRootElementName.replaceAll("\\{.*\\}(.*)", "$1");
        }
        return new QName(nameSpace, name, "");
    }
}
