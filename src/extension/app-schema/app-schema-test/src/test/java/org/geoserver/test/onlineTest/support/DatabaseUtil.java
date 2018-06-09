/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.support;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * This class provides utility function to split up generated sql files into individual statement to
 * be execute by JDBC. Currently oracle and postgres driver do not support execution of sql scripts.
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class DatabaseUtil {

    public static final String NEWLINE = System.getProperty("line.separator");

    /**
     * Split input stream into a list of sql statements.
     *
     * <p>there are a number of limitation when using this method. Firstly each separate query in a
     * script must be on a new line so that ";" at the end of the string can be used as a delimiter.
     * Secondly, escape character for $$ $_$ "'" have not been taken into consideration. This will
     * cause an issue if any of the operators are used in string eg 'the $quick brown' We have
     * catered to scenarios for multiple $ quoting on single/multi line.
     *
     * @param inputStream sql statements
     * @return list of SQL statements
     */
    public List<String> splitPostgisSQLScript(InputStream inputStream) throws Exception {

        StringBuilder contents = new StringBuilder();

        ArrayList<String> statements = new ArrayList<String>();
        try {
            // use buffering, reading one line at a time
            // FileReader always assumes default encoding is OK!
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(new DataInputStream(inputStream)));
            try {
                String line = null;
                PostgisIgnoreOperator pio = new PostgisIgnoreOperator();
                while ((line = input.readLine()) != null) {
                    String trimedLine = line.trim();
                    if (trimedLine.startsWith("--") || trimedLine.equals("")) {
                        continue;
                    }

                    for (String opr : pio.getOperators()) {
                        if (countMatches(trimedLine, opr) % 2 == 1) {
                            pio.setReverseStatus(opr);
                        }
                    }
                    contents.append(trimedLine + NEWLINE);

                    if (trimedLine.endsWith(";") && pio.isAllClosed()) {
                        statements.add(contents.toString());
                        pio.reset();
                        contents.setLength(0);
                    }
                }

                return statements;

            } finally {
                input.close();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Counts the number of matches that exist in the string
     *
     * @param str - the main string to be matched against
     * @param sub - the substring to be match
     * @return - count of the number of matches
     */
    public static int countMatches(String str, String sub) {
        if (str.length() == 0 || sub.length() == 0) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Return a list of SQL statements as a single string including a newline after each statement.
     *
     * @param sqls list of SQL statements
     * @return string of statements with newline after each
     */
    public String rebuildAsSingle(List<String> sqls) {
        StringBuilder sb = new StringBuilder();
        for (String sql : sqls) {
            sb.append(sql).append("\n");
        }
        return sb.toString();
    }

    /**
     * sets up the rule such that all ";" between these operators will not be treated as a delimiter
     */
    private class PostgisIgnoreOperator {

        public final String[] operators = {"$$", "$_$", "'"};

        Hashtable<String, Boolean> open;

        PostgisIgnoreOperator() {
            open = new Hashtable<String, Boolean>();
            for (String s : operators) {
                open.put(s, Boolean.FALSE);
            }
        }

        /**
         * Retrieve the operators. Operators are possible string that can be used to encapsulate a
         * sql query in postgres.
         *
         * @return - The list of operators
         */
        public String[] getOperators() {
            return operators;
        }

        /**
         * Retrieve the current status of the operator
         *
         * @param key - the operator
         * @return - the status whether the operator is currently open or closed. eg if we found $$
         *     and no corresponding $$ to close it, the status is open
         */
        public boolean getOperatorStatus(String key) {
            return open.get(key).booleanValue();
        }

        /**
         * Sets the status of the operator
         *
         * @param key - the operator's status to be set
         * @param value - the status value
         */
        public void setOperatorStatus(String key, boolean value) {
            open.put(key, value);
        }

        /**
         * Sets the reverse of status of the operator. If the current status of the operator is
         * open, close it.
         *
         * @param key - the operator
         */
        public void setReverseStatus(String key) {
            open.put(key, !open.get(key));
        }

        /**
         * Checks if all operators are currently closed
         *
         * @return - true only if the current status of all operators are closed. This determines
         *     the end of the statements in the sql script.
         */
        public boolean isAllClosed() {
            for (boolean opn : open.values()) {
                if (opn) {
                    return false;
                }
            }
            return true;
        }

        /** Resets the status of all the operators. */
        public void reset() {
            for (String s : this.operators) {
                setOperatorStatus(s, false);
            }
        }
    }

    /**
     * Splits the oracle sql script file into individual statements.
     *
     * @param inputStream The oracle sql script
     * @returnlist of sql statements
     */
    public List<String> splitOracleSQLScript(InputStream inputStream) throws Exception {

        StringBuilder contents = new StringBuilder();

        ArrayList<String> statements = new ArrayList<String>();
        try {
            // use buffering, reading one line at a time
            // FileReader always assumes default encoding is OK!
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(new DataInputStream(inputStream)));
            try {
                String line = null, suffix = null; // not declared within while loop
                boolean start = true;

                while ((line = input.readLine()) != null) {
                    String trimedLine = line.trim();
                    if (trimedLine.startsWith("--") || trimedLine.equals("")) {
                        continue;
                    }
                    if (start) {
                        boolean match = false;
                        for (OracleScriptRule ps : OracleScriptRule.values()) {
                            if (trimedLine.startsWith(ps.getPrefix())) {
                                match = true;
                                suffix = ps.getSuffix();
                                start = trimedLine.endsWith(suffix) ? true : false;
                                contents.append(trimedLine + NEWLINE);
                                if (start) {
                                    statements.add(
                                            (contents.toString().trim())
                                                    .substring(
                                                            0,
                                                            contents.toString().trim().length()
                                                                    - 1));
                                    contents.setLength(0);
                                    suffix = null;
                                }
                                break;
                            }
                        }
                        if (!match) {
                            throw new Exception("Can't match " + trimedLine);
                        }
                    } else {
                        if (trimedLine.endsWith(suffix)) {
                            trimedLine = trimedLine.trim().substring(0, trimedLine.length() - 1);
                            contents.append(trimedLine);
                            statements.add(contents.toString());
                            contents.setLength(0);
                            start = true;
                            suffix = null;
                        } else {
                            contents.append(trimedLine + NEWLINE);
                        }
                    }
                }

                return statements;

            } finally {
                input.close();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Enum class that specify the rule to breaking up the oracle script into individual statements.
     */
    private enum OracleScriptRule {
        INSERT("Insert", ");"),
        CREATE_OR_REPLACE_PROCEDURE("CREATE OR REPLACE PROCEDURE", "/"),
        CALL("CALL", ";"),
        DELETE("DELETE", ";"),
        CREATE_TABLE("CREATE TABLE", ";"),
        CREATE_OR_REPLACE_FORCE_VIEW("CREATE OR REPLACE FORCE VIEW", ";"),
        REM("REM", ";"),
        CREATE_INDEX("CREATE INDEX", ";"),
        DECLARE("declare", "/"),
        COMMIT("COMMIT", ";"),
        ALTER("ALTER TABLE", ");");

        private String prefix;

        private String suffix;

        OracleScriptRule(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }
    }
}
