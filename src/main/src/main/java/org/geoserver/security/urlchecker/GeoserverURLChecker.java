/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecker;

/*
 * This is the Geoserver implementation of URLChecker interface
 * */

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.ows.Dispatcher;
import org.geotools.data.ows.URLChecker;
import org.geotools.util.logging.Logging;

public class GeoserverURLChecker implements URLChecker, Serializable, Cloneable {

    static final Logger LOGGER = Logging.getLogger(GeoserverURLChecker.class.getCanonicalName());

    /** serialVersionUID */
    private static final long serialVersionUID = -7056796646665162468L;

    private boolean enabled;

    private List<URLEntry> regexList;

    public GeoserverURLChecker(List<URLEntry> regexList) {
        this.regexList = regexList;
    }

    /** @return the regexList */
    public List<URLEntry> getRegexList() {
        //  if (regexList == null) regexList = new ArrayList<URLEntry>();
        return regexList;
    }

    /** @param regexList the regexList to set */
    public void setRegexList(List<URLEntry> regexList) {
        this.regexList = regexList;
    }

    /** @param enabled the enabled to set */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return "Geoserver";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean evaluate(String url) {
        if (Dispatcher.REQUEST.get() == null) {
            // ignore calls made from admin console
            return true;
        }
        if (url == null) return false;
        else if (url.isEmpty()) return false;
        List<URLEntry> enabledUrlList = getEnabled();
        // ignore
        if (enabledUrlList.isEmpty()) return true;

        for (URLEntry u : enabledUrlList) {
            if (url.matches(u.getRegexExpression())) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, url + " has matched regex " + u.getRegexExpression());
                }
                return true;
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, url + " did not match any REGEX");
        }

        return false;
    }

    private List<URLEntry> getEnabled() {
        return getRegexList().stream().filter(e -> e.isEnable()).collect(Collectors.toList());
    }

    protected void addURLEntry(URLEntry urlEntry) {
        if (urlEntry == null) return;
        if (urlEntry.getName() == null) return;
        if (urlEntry.getName().isEmpty()) return;

        int idx = getRegexList().indexOf(urlEntry);

        if (idx == -1) getRegexList().add(urlEntry);
        else getRegexList().set(idx, urlEntry);
    }

    protected boolean removeURLEntry(List<URLEntry> deleteList) {
        return getRegexList().removeAll(deleteList);
    }

    public URLEntry get(final String urlEntryName) {
        Optional<URLEntry> entry =
                getRegexList()
                        .stream()
                        .filter(urlEntry -> urlEntry.getName().equalsIgnoreCase(urlEntryName))
                        .findFirst();
        if (entry.isPresent()) return entry.get();
        else return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GeoserverURLChecker)) return false;
        GeoserverURLChecker other = (GeoserverURLChecker) obj;
        return getName().equalsIgnoreCase(other.getName());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
