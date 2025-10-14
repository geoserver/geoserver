/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.Serial;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.convert.IConverter;

/**
 * A data view listing files in a certain directory, subject to a file filter
 *
 * @author Andrea Aime - OpenGeo
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public abstract class FileDataView extends Panel {
    private static final IConverter<File> FILE_NAME_CONVERTER = new StringConverter() {

        @Override
        public String convertToString(File file, Locale locale) {
            if (file.isDirectory()) {
                return file.getName() + "/";
            } else {
                return file.getName();
            }
        }
    };

    private static final IConverter<File> FILE_LASTMODIFIED_CONVERTER = new StringConverter() {

        @Override
        public String convertToString(File file, Locale locale) {
            long lastModified = file.lastModified();
            if (lastModified == 0L) return null;
            else {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(new Date(file.lastModified()));
            }
        }
    };

    private static final IConverter<File> FILE_SIZE_CONVERTER = new StringConverter() {
        private static final double KBYTE = 1024;
        private static final double MBYTE = KBYTE * 1024;
        private static final double GBYTE = MBYTE * 1024;

        @Override
        public String convertToString(File value, Locale locale) {
            File file = value;

            if (!file.isFile()) return "";

            long size = file.length();
            if (size == 0L) return null;

            if (size < KBYTE) {
                return size + "";
            } else if (size < MBYTE) {
                return new DecimalFormat("#.#").format(size / KBYTE) + "K";
            } else if (size < GBYTE) {
                return new DecimalFormat("#.#").format(size / MBYTE) + "M";
            } else {
                return new DecimalFormat("#.#").format(size / GBYTE) + "G";
            }
        }
    };

    FileProvider provider;

    WebMarkupContainer fileContent;

    String tableHeight = "25em";

    public FileDataView(String id, FileProvider fileProvider) {
        super(id);

        this.provider = fileProvider;
        //        provider.setDirectory(currentPosition);
        //        provider.setSort(new SortParam(NAME, true));

        final WebMarkupContainer table = new WebMarkupContainer("fileTable");
        table.setOutputMarkupId(true);
        add(table);

        DataView<File> fileTable = new DataView<>("files", fileProvider) {

            @Override
            protected void populateItem(final Item<File> item) {

                // odd/even alternate style
                item.add(AttributeModifier.replace("class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                // navigation/selection links
                AjaxFallbackLink<?> link = new IndicatingAjaxFallbackLink<Void>("nameLink") {

                    @Override
                    public void onClick(Optional<AjaxRequestTarget> target) {
                        linkNameClicked(item.getModelObject(), target);
                    }
                };
                link.add(new Label("name", item.getModel()) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) FILE_NAME_CONVERTER;
                    }
                });
                item.add(link);

                // last modified and size labels
                item.add(new Label("lastModified", item.getModel()) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) FILE_LASTMODIFIED_CONVERTER;
                    }
                });
                item.add(new Label("size", item.getModel()) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) FILE_SIZE_CONVERTER;
                    }
                });
            }
        };

        fileContent = new WebMarkupContainer("fileContent");
        if (tableHeight != null) {
            fileContent.setOutputMarkupId(true);
            fileContent.add(AttributeModifier.replace("class", "overflowAuto"));
            fileContent.add(new Behavior() {
                @Serial
                private static final long serialVersionUID = 4820788798632906484L;

                @Override
                public void renderHead(Component component, IHeaderResponse response) {
                    String script = "document.getElementById('"
                            + fileContent.getMarkupId()
                            + "').style.height = '"
                            + StringEscapeUtils.escapeEcmaScript(tableHeight)
                            + "';";
                    response.render(OnLoadHeaderItem.forScript(script));
                }
            });
        }

        fileContent.add(fileTable);

        table.add(fileContent);
        table.add(new OrderByBorder<>("nameHeader", FileProvider.NAME, fileProvider));
        table.add(new OrderByBorder<>("lastModifiedHeader", FileProvider.LAST_MODIFIED, fileProvider));
        table.add(new OrderByBorder<>("sizeHeader", FileProvider.SIZE, fileProvider));
    }

    protected abstract void linkNameClicked(File file, Optional<AjaxRequestTarget> target);

    private abstract static class StringConverter implements IConverter<File> {

        @Override
        public File convertToObject(String value, Locale locale) {
            throw new UnsupportedOperationException("This converter works only for strings");
        }
    }

    public FileProvider getProvider() {
        return provider;
    }

    public void setTableHeight(String tableHeight) {
        this.tableHeight = tableHeight;
    }
}
