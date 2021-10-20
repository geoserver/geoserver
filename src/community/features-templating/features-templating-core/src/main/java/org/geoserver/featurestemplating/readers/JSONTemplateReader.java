/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.readers.JSONTemplateReaderUtil.CONTEXTKEY;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.FileWatcher;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JSONTemplateReader implements TemplateReader {

    private JsonNode template;

    private TemplateReaderConfiguration configuration;

    private List<FileWatcher<Object>> watchers;

    private JSONTemplateReaderUtil jsonTemplateReaderUtil;

    public JSONTemplateReader(
            JsonNode template,
            TemplateReaderConfiguration configuration,
            List<FileWatcher<Object>> watchers) {
        this.template = template;
        this.configuration = configuration;
        this.watchers = watchers;
        this.jsonTemplateReaderUtil = new JSONTemplateReaderUtil();
    }

    /**
     * Get a builder tree as a ${@link RootBuilder} mapping it from a Json template
     *
     * @return
     */
    @Override
    public RootBuilder getRootBuilder() {
        TemplateBuilderMaker builderMaker = configuration.getBuilderMaker();
        if (template.has(CONTEXTKEY))
            builderMaker.encodingOption(CONTEXTKEY, template.get(CONTEXTKEY));
        builderMaker.rootBuilder(true);
        RootBuilder root = (RootBuilder) builderMaker.build();
        builderMaker.namespaces(configuration.getNamespaces());
        jsonTemplateReaderUtil.getBuilderFromJson(null, template, root, builderMaker);
        root.setWatchers(watchers);
        return root;
    }
}
