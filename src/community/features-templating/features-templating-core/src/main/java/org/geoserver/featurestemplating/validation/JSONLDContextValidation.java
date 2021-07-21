/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.validation;

import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;

/** This class provides methods to validate json-ld features against the @context. */
public class JSONLDContextValidation {

    File tmpFile;

    /**
     * Method to init the validator, produces a tmp file in the data directory.
     *
     * @return the file tml file produced in the data dir as a File object
     * @throws IOException
     */
    public File init() throws IOException {
        GeoServerResourceLoader directory = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        tmpFile = new File(directory.getBaseDirectory(), "json-ld-validation.tmp");
        tmpFile.createNewFile();
        return tmpFile;
    }

    Set<String> failedFields = new HashSet<>();

    /**
     * Parse the json file where the features have been wrote and perform a validation against
     * the @context. The validation process execute the expansion algorithm against the features
     * followed by a compaction algorithm. The expansion algorithm expand each field name of a
     * feature against the iri declared in the @contex, removing those for which is not found any
     * reference. The compaction algorithm perform the original operation. The the result of the
     * compaction process and the original json-ld document are compared. If any field name is
     * missed a RuntimeException is thrown.
     *
     * @throws ServiceException
     */
    public void validate() {
        try {
            if (tmpFile != null) {
                try (InputStream is = new FileInputStream(tmpFile)) {
                    Object json = JsonUtils.fromInputStream(is);
                    validate(json);
                }
            }
        } catch (JsonLdError jsonLdError) {
            throw new ServiceException(
                    "Error while validating "
                            + "the json-ld output. Message is: "
                            + jsonLdError.getMessage());
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            tmpFile.delete();
        }
    }

    public void validate(Object json) {
        Map<String, Object> jsonMap = (Map<String, Object>) json;
        JsonLdOptions options = new JsonLdOptions();
        Object context = jsonMap.get("@context");
        options.setExpandContext(context);
        options.setAllowContainerSetOnType(true);
        // run the expansion algorithm
        List<Object> expanded = JsonLdProcessor.expand(json, options);
        if (expanded.size() == 0) {
            // list is void it means that there is no reference for the
            // feature fields name inside the context.
            throw new RuntimeException(
                    "Validation failed. Unable to resolve the field features "
                            + " against the @context");
        }
        // run the compaction
        Object compacted = JsonLdProcessor.compact(expanded, context, options);
        // compares the original json-ld to the result of the compaction
        checkJsonLdByKeys(json, compacted);
        if (failedFields.size() > 0) {
            Iterator<String> it = failedFields.iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) sb.append(",");
            }
            sb.append(". ");
            throw new ServiceException(
                    "Validation failed. Unable to resolve the following fields"
                            + " against the @context: "
                            + sb.toString());
        }
    }

    private void checkJsonLdByKeys(Object original, Object compacted) {
        Map<String, Object> originalJsonLd = (Map<String, Object>) original;
        Map<String, Object> compactJsonLd = (Map<String, Object>) compacted;
        checkJsonLdLists(originalJsonLd.get("features"), compactJsonLd.get("features"));
    }

    private void checkJsonLdMaps(Object orig, Object compacted) {
        Map<String, Object> mapOrig = (Map<String, Object>) orig;
        Map<String, Object> mapCompact = (Map<String, Object>) compacted;
        Set<String> keys = mapOrig.keySet();
        for (String key : keys) {
            Object origEl = mapOrig.get(key);
            Object compactEl = mapCompact.get(key);
            if (compactEl == null) {
                failedFields.add(key);
            } else {
                // this is a fix for a strange behaviour of the
                // json-ld api: sometimes the compaction algorithm
                // produce json object not as map but as a single element List
                // which holds the map.
                if (!(origEl instanceof List)) {
                    origEl = Arrays.asList(origEl);
                }
                if (!(compactEl instanceof List)) {
                    compactEl = Arrays.asList(compactEl);
                }
                checkJsonLdLists(origEl, compactEl);
            }
        }
    }

    private void checkJsonLdLists(Object orig, Object compacted) {
        List<Object> featuresOrig = (List<Object>) orig;
        List<Object> featuresCompact = (List<Object>) compacted;

        for (int i = 0; i < featuresOrig.size(); i++) {
            Object fOrig = featuresOrig.get(i);
            Object fCompact = featuresCompact.get(i);
            if (fOrig instanceof Map) {
                checkJsonLdMaps(fOrig, fCompact);
            } else if (fOrig instanceof List) {
                checkJsonLdLists(orig, compacted);
            }
        }
    }

    public Set<String> getFailedFields() {
        return failedFields;
    }
}
