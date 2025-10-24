package org.geoserver.backuprestore.reader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
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
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.xml.stax.FragmentEventReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * StAX-based reader that supports both old chunked backups (<items> wrapper) and one-object-per-file backups. It can
 * auto-populate acceptable element names by type if none are explicitly configured.
 */
public class CatalogFileReader<T> extends CatalogReader<T> {

    private static final Logger logger = Logging.getLogger(CatalogFileReader.class);

    private FragmentEventReader fragmentReader;
    private XMLEventReader eventReader;
    private Resource resource;
    private InputStream inputStream;

    private List<QName> fragmentRootElementNames;

    private boolean noInput;
    /** Keep default strict=true for backwards compatibility (can be set from step config). */
    private boolean strict = true;

    public CatalogFileReader(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
        this.clazz = clazz;
    }

    protected final Class<T> clazz;

    protected final Class<T> getClazz() {
        return clazz;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
        // If no element names were provided by step wiring, infer sensible defaults
        if (fragmentRootElementNames == null || fragmentRootElementNames.isEmpty()) {
            fragmentRootElementNames = defaultElementNamesFor(getClazz());
        }
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /** Accept a single element name */
    public void setFragmentRootElementName(String fragmentRootElementName) {
        setFragmentRootElementNames(new String[] {fragmentRootElementName});
    }

    /** Accept multiple element names */
    public void setFragmentRootElementNames(String[] names) {
        this.fragmentRootElementNames = new ArrayList<>();
        for (String n : names) {
            this.fragmentRootElementNames.add(parseFragmentRootElementName(n));
        }
    }

    /** Move cursor to next start element matching one of the configured names (inside or outside <items>). */
    protected boolean moveCursorToNextFragment(XMLEventReader reader) {
        try {
            while (true) {
                while (reader.peek() != null && !reader.peek().isStartElement()) {
                    reader.nextEvent();
                }
                if (reader.peek() == null) return false;

                StartElement se = (StartElement) reader.peek();
                QName name = se.getName();

                // Skip <items> wrappers transparently
                if ("items".equals(name.getLocalPart())) {
                    reader.nextEvent(); // consume <items>
                    continue;
                }

                if (isFragmentRootElementName(name)) return true;
                reader.nextEvent();
            }
        } catch (XMLStreamException e) {
            return logValidationExceptions(
                    (T) null, new NonTransientResourceException("Error while reading from event reader", e));
        }
    }

    @Override
    protected void doClose() throws Exception {
        try {
            if (fragmentReader != null) fragmentReader.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException | XMLStreamException e) {
            logValidationExceptions((T) null, e);
        } finally {
            fragmentReader = null;
            inputStream = null;
        }
    }

    // NEW: helper to set default fragments when none were configured
    private void inferFragmentsFromFilenameIfMissing() {
        if (fragmentRootElementNames != null && !fragmentRootElementNames.isEmpty()) return;
        if (resource == null || resource.getFilename() == null) return;
        String fn = resource.getFilename().toLowerCase();

        List<String> names = new java.util.ArrayList<>();

        if (fn.startsWith("namespace.dat")) {
            names = java.util.List.of("namespace");
        } else if (fn.startsWith("workspace.dat")) {
            names = java.util.List.of("workspace");
        } else if (fn.startsWith("store.dat")) {
            names = java.util.List.of("dataStore", "coverageStore", "wmsStore", "wmtsStore");
        } else if (fn.startsWith("resource.dat")) {
            names = java.util.List.of("featureType", "coverage", "wmsLayer", "wmtsLayer");
        } else if (fn.startsWith("layer.dat")) {
            names = java.util.List.of("layer");
        } else if (fn.startsWith("layergroup.dat")) {
            names = java.util.List.of("layerGroup");
        } else if (fn.startsWith("style.dat")) {
            names = java.util.List.of("style");
        } else {
            // leave empty: delegate configuration may still set them explicitly
            names = java.util.List.of();
        }

        if (!names.isEmpty()) {
            this.fragmentRootElementNames = new java.util.ArrayList<>();
            for (String n : names) {
                this.fragmentRootElementNames.add(new javax.xml.namespace.QName(n));
            }
        }
    }

    @Override
    protected void doOpen() throws Exception {
        Assert.notNull(resource, "The Resource must not be null.");

        try {
            noInput = true;
            if (!resource.exists()) {
                if (strict) {
                    throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode)");
                }
                logger.warning("Input resource does not exist " + resource.getDescription());
                return;
            }
            if (!resource.isReadable()) {
                if (strict) {
                    throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode)");
                }
                logger.warning("Input resource is not readable " + resource.getDescription());
                return;
            }

            // NEW: pick defaults for old *.dat.* bundles, if nothing was configured
            inferFragmentsFromFilenameIfMissing();

            inputStream = resource.getInputStream();
            eventReader = javax.xml.stream.XMLInputFactory.newInstance().createXMLEventReader(inputStream);
            fragmentReader = new org.springframework.batch.item.xml.stax.DefaultFragmentEventReader(eventReader);
            noInput = false;
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    // So afterPropertiesSet() doesn’t fail when we auto-infer later
    @Override
    public void afterPropertiesSet() throws Exception {
        // If nothing set, we'll infer in doOpen(). Keep validation only when explicitly configured.
        if (fragmentRootElementNames != null && !fragmentRootElementNames.isEmpty()) {
            for (QName qn : fragmentRootElementNames) {
                Assert.hasText(qn.getLocalPart(), "Fragment root names must not contain empty elements");
            }
        }
    }

    @Override
    protected T doRead() throws Exception {
        T item = null;
        try {
            if (noInput) return null;

            boolean success;
            try {
                success = moveCursorToNextFragment(fragmentReader);
            } catch (NonTransientResourceException e) {
                noInput = true; // fatal
                throw e;
            }
            if (success) {
                fragmentReader.markStartFragment();
                try {
                    @SuppressWarnings("unchecked")
                    T mapped = (T) unmarshal(StaxUtils.createStaxSource(fragmentReader));
                    item = mapped;
                    try {
                        firePostRead(item, resource);
                    } catch (IOException e) {
                        logValidationExceptions(
                                (ValidationResult) null,
                                new UnexpectedInputException("Could not write data. The file may be corrupt.", e));
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

    /** Unmarshal a fragment via XStreamPersister. */
    private Object unmarshal(Source source)
            throws TransformerException, XMLStreamException, UnsupportedEncodingException {
        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        Transformer t = tf.newTransformer();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Result result = new StreamResult(os);
        t.transform(source, result);
        return this.getXp().fromXML(new ByteArrayInputStream(os.toByteArray()));
    }

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {
        for (int i = 0; i < itemIndex; i++) {
            try {
                QName fragmentName = readToStartFragment();
                readToEndFragment(fragmentName);
            } catch (NoSuchElementException e) {
                if (itemIndex == (i + 1)) {
                    return; // EOF at last item -> fine
                } else {
                    logValidationExceptions((T) null, e);
                }
            }
        }
    }

    private QName readToStartFragment() throws XMLStreamException {
        while (true) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()) {
                QName name = ((StartElement) nextEvent).getName();
                if ("items".equals(name.getLocalPart())) continue; // skip wrapper
                if (isFragmentRootElementName(name)) return name;
            }
        }
    }

    private void readToEndFragment(QName fragmentRootElementName) throws XMLStreamException {
        while (true) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isEndElement() && fragmentRootElementName.equals(((EndElement) nextEvent).getName())) {
                return;
            }
        }
    }

    private boolean isFragmentRootElementName(QName name) {
        for (QName qn : fragmentRootElementNames) {
            if (qn.getLocalPart().equals(name.getLocalPart())) {
                if (!StringUtils.hasText(qn.getNamespaceURI())
                        || qn.getNamespaceURI().equals(name.getNamespaceURI())) {
                    return true;
                }
            }
        }
        return false;
    }

    private QName parseFragmentRootElementName(String s) {
        String name = s;
        String ns = null;
        if (s.contains("{")) {
            ns = s.replaceAll("\\{(.*)\\}.*", "$1");
            name = s.replaceAll("\\{.*\\}(.*)", "$1");
        }
        return new QName(ns, name, "");
    }

    /** Auto element-name inference for common catalog types. */
    private static List<QName> defaultElementNamesFor(Class<?> clazz) {
        List<QName> out = new ArrayList<>();
        if (NamespaceInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "namespace", ""));
        } else if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "workspace", ""));
        } else if (StoreInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "dataStore", ""));
            out.add(new QName(null, "coverageStore", ""));
            out.add(new QName(null, "wmsStore", ""));
            out.add(new QName(null, "wmtsStore", ""));
        } else if (ResourceInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "featureType", ""));
            out.add(new QName(null, "coverage", ""));
            out.add(new QName(null, "wmsLayer", ""));
            out.add(new QName(null, "wmtsLayer", ""));
        } else if (StyleInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "style", ""));
        } else if (LayerInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "layer", ""));
        } else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "layerGroup", ""));
        } else if (DataStoreInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "dataStore", ""));
        } else if (CoverageStoreInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "coverageStore", ""));
        } else if (WMSStoreInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "wmsStore", ""));
        } else if (WMTSStoreInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "wmtsStore", ""));
        } else if (CoverageInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "coverage", ""));
        } else if (WMSLayerInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "wmsLayer", ""));
        } else if (WMTSLayerInfo.class.isAssignableFrom(clazz)) {
            out.add(new QName(null, "wmtsLayer", ""));
        }
        return out;
    }
}
