/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.data.complex.util.XPathUtil;

public class PropertyPath {

    private static final Pattern PATTERN_ATT_WITH_INDEX = Pattern.compile("([^\\[]*)\\[(.*)\\]");

    protected static class Element {
        private String name;

        private Integer index;

        public String getName() {
            return name;
        }

        public Integer getIndex() {
            return index;
        }

        public Element(String name) {
            this(name, null);
        }

        public Element(String name, Integer index) {
            this.name = name;
            this.index = index;
        }

        public boolean matches(Element pattern) {
            return name.equalsIgnoreCase(pattern.name)
                    && (pattern.index == null || index != null && index.equals(pattern.index));
        }
    }

    protected List<Element> elements = new ArrayList<>();

    protected PropertyPath() {}

    public static PropertyPath fromDotPath(String dotPath) {
        if (dotPath == null) {
            return null;
        }
        PropertyPath key = new PropertyPath();
        for (String name : dotPath.split("\\.")) {
            Matcher matcher = PATTERN_ATT_WITH_INDEX.matcher(name);
            if (matcher.matches()) {
                try {
                    key.elements.add(new Element(matcher.group(1), Integer.parseInt(matcher.group(2))));
                } catch (NumberFormatException e) {
                    key.elements.add(new Element(name));
                }
            } else {
                key.elements.add(new Element(name));
            }
        }
        return key;
    }

    /**
     * Convert StepList to CatalogStorePropertyName.
     *
     * @param steps XPath steplist
     * @return CatalogStorePropertyName
     */
    public static PropertyPath fromXPath(XPathUtil.StepList steps) {
        PropertyPath key = new PropertyPath();

        for (XPathUtil.Step step : steps) {
            if (step.isIndexed()) {
                key.elements.add(new Element(step.getName().getLocalPart(), step.getIndex()));
            } else {
                key.elements.add(new Element(step.getName().getLocalPart()));
            }
        }

        return key;
    }

    public String toDothPath() {
        StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            sb.append(element.name);
            if (element.index != null) {
                sb.append("[").append(element.index).append("]");
            }
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public boolean matches(PropertyPath pattern) {
        if (elements.size() != pattern.elements.size()) {
            return false;
        }
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).matches(pattern.elements.get(i))) {
                return false;
            }
        }
        return true;
    }

    public PropertyPath removeIndexes() {
        PropertyPath key = new PropertyPath();
        for (Element element : elements) {
            key.elements.add(new Element(element.getName()));
        }

        return key;
    }

    public PropertyPath add(String name) {
        PropertyPath key = new PropertyPath();
        key.elements.addAll(elements);
        key.elements.add(new Element(name));
        return key;
    }

    public String getName(int index) {
        return elements.get(index).getName();
    }

    public Integer getIndex(int index) {
        return elements.get(index).getIndex();
    }

    public int getSize() {
        return elements.size();
    }

    @Override
    public String toString() {
        return toDothPath();
    }
}
