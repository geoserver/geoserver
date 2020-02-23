/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;

/**
 * Dumps a wicket component/page hierarchy to text, eventually writing down the class and the model
 * value as a string.
 *
 * <p>Each line in the dump follow the <code>componentId(class) 'value'</code> format.
 *
 * <p>The class can be reused for multiple prints, but it's not thread safe
 */
public class WicketHierarchyPrinter {
    static final Pattern NEWLINE = Pattern.compile("\\n", Pattern.MULTILINE);

    PrintStream out;

    boolean valueDumpEnabled;

    boolean classDumpEnabled;

    boolean pathDumpEnabled;

    /** Utility method to dump a single component/page to standard output */
    public static void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        WicketHierarchyPrinter printer = new WicketHierarchyPrinter();
        printer.setPathDumpEnabled(dumpClass);
        printer.setClassDumpEnabled(dumpClass);
        printer.setValueDumpEnabled(dumpValue);
        if (c instanceof Page) {
            printer.print((Page) c);
        } else {
            printer.print(c);
        }
    }

    /** Utility method to dump a single component/page to standard output */
    public static void print(Component c, boolean dumpClass, boolean dumpValue) {
        print(c, dumpClass, dumpValue, false);
    }

    /** Creates a printer that will dump to standard output */
    public WicketHierarchyPrinter() {
        out = System.out;
    }

    /** Creates a printer that will dump to the specified print stream */
    public WicketHierarchyPrinter(PrintStream out) {
        this.out = out;
    }

    /** Set to true if you want to see the model values in the dump */
    public void setValueDumpEnabled(boolean valueDumpEnabled) {
        this.valueDumpEnabled = valueDumpEnabled;
    }

    /** Set to true if you want to see the component classes in the dump */
    public void setClassDumpEnabled(boolean classDumpEnabled) {
        this.classDumpEnabled = classDumpEnabled;
    }

    /** Prints the component containment hierarchy */
    public void print(Component c) {
        walkHierarchy(c, 0);
    }

    /** Walks down the containment hierarchy depth first and prints each component found */
    private void walkHierarchy(Component c, int level) {
        printComponent(c, level);
        if (c instanceof MarkupContainer) {
            MarkupContainer mc = (MarkupContainer) c;
            for (Iterator<?> it = mc.iterator(); it.hasNext(); ) {
                walkHierarchy((Component) it.next(), level + 1);
            }
        }
    }

    /** Prints a single component */
    private void printComponent(Component c, int level) {
        if (c instanceof Page) out.print(tab(level) + "PAGE_ROOT");
        else out.print(tab(level) + c.getId());

        if (pathDumpEnabled) {
            out.print(" " + c.getPageRelativePath());
        }

        if (classDumpEnabled) {
            String className;
            if (c.getClass().isAnonymousClass()) {
                className = c.getClass().getSuperclass().getName();
            } else {
                className = c.getClass().getName();
            }

            out.print("(" + className + ")");
        }

        if (valueDumpEnabled) {
            try {
                String value =
                        NEWLINE.matcher(c.getDefaultModelObjectAsString()).replaceAll("\\\\n");
                out.print(" '" + value + "'");
            } catch (Exception e) {
                out.print(" 'ERROR_RETRIEVING_MODEL " + e.getMessage() + "'");
            }
        }

        out.println();
    }

    /** Generates three spaces per level */
    String tab(int level) {
        char[] spaces = new char[level * 3];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }

    /** If the page relative path dumping is enabled */
    public boolean isPathDumpEnabled() {
        return pathDumpEnabled;
    }

    /** Sets/unsets the relative path dumping */
    public void setPathDumpEnabled(boolean pathDumpEnabled) {
        this.pathDumpEnabled = pathDumpEnabled;
    }
}
