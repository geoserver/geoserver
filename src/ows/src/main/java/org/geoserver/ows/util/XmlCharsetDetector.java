/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a methods that can be used to detect charset of some XML document and (optionally)
 * return a reader that is aware of this charset and can correctly decode document's data.
 */
public class XmlCharsetDetector {
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests");

    /** In current context naming this "GT", "GREATER_THAN" or like would be misleading. */
    private static final char RIGHT_ANGLE_BRACKET = '\u003E';

    private static final Pattern ENCODING_PATTERN =
            Pattern.compile("encoding\\s*\\=\\s*\"([^\"]+)\"");

    /**
     * Maximum number of characters we are expecting in XML Declaration. There are probably will be
     * less then 100, but just in case...
     */
    private static final int MAX_XMLDECL_SIZE = 100;

    /**
     * Based on Xerces-J code, this method will try its best to return a reader which is able to
     * decode content of incoming XML document properly. To achieve this goal, it first infers
     * general encoding scheme of the above document and then uses this information to extract
     * actual charset from XML declaration. In any recoverable error situation default UTF-8 reader
     * will be created.
     *
     * @param istream Byte stream (most probably obtained with <code>
     *     HttpServletRequest.getInputStream</code> that gives access to XML document in question).
     * @param encInfo Instance of EncodingInfo where information about detected charset will be
     *     stored. You can then use it, for example, to form a response encoded with this charset.
     * @throws IOException in case of any unrecoverable I/O errors.
     * @throws UnsupportedCharsetException <code>InputStreamReader</code>'s constructor will
     *     probably throw this exception if inferred charset of XML document is not supported by
     *     current JVM.
     */
    public static Reader getCharsetAwareReader(InputStream istream, EncodingInfo encInfo)
            throws IOException, UnsupportedCharsetException {
        RewindableInputStream stream;
        stream = new RewindableInputStream(istream, false);

        //
        // Phase 1. Reading first four bytes and determining encoding scheme.
        final byte[] b4 = new byte[4];

        int count = 0;

        for (; count < 4; count++) {
            int b = stream.read();

            if (-1 != b) {
                b4[count] = (byte) b;
            } else {
                break;
            }
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            // Such number of concatenating strings makes me sick.
            // But using StringBuffer will make this uglier, not?
            LOGGER.finer(
                    "First 4 bytes of XML doc are : "
                            + Integer.toHexString((int) b4[0] & 0xff).toUpperCase()
                            + " ('"
                            + (char) b4[0]
                            + "') "
                            + Integer.toHexString((int) b4[1] & 0xff).toUpperCase()
                            + " ('"
                            + (char) b4[1]
                            + "') "
                            + Integer.toHexString((int) b4[2] & 0xff).toUpperCase()
                            + " ('"
                            + (char) b4[2]
                            + "') "
                            + Integer.toHexString((int) b4[3] & 0xff).toUpperCase()
                            + " ('"
                            + (char) b4[3]
                            + "')");
        }

        /*
         * `getEncodingName()` is capable of detecting following encoding
         * schemes:
         * "UTF-8", "UTF-16LE", "UTF-16BE", "ISO-10646-UCS-4",
         * or "CP037". It cannot distinguish between UTF-16 (without BOM)
         * and "ISO-10646-UCS-2", so latter will be interpreted as UTF-16
         * for the purpose of reading XML declaration. There shouldn't be
         * much trouble though as (I believe) these formats are identical for
         * the Basic Multilingual Plane, except that UTF-16-encoded text
         * can contain values from surrogate range and valid UCS-2 input
         * cannot (imho).
         * This ugly form of copying charset data is required to maintain
         * "reference integrity" of encInfo variable. As it can be possibly
         * used after this method call, it should point to the same memory
         * structure, and assignment or cloning doesn't work for me there.
         */
        encInfo.copyFrom(getEncodingName(b4, count));

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Charset detection phase 1. Inferred encoding: " + encInfo.toString());
        }

        // Rewinding to beginning of data
        stream.reset();

        String ENCODING = encInfo.getEncoding().toUpperCase(Locale.ENGLISH);
        Boolean isBigEndian = encInfo.isBigEndian();
        boolean hasBOM = encInfo.hasBOM();

        /*
         * Special case UTF-8 files with BOM created by Microsoft
         * tools. It's more efficient to consume the BOM than make
         * the reader perform extra checks. -Ac
         */
        if (hasBOM && ENCODING.equals("UTF-8")) {
            // ignore first three bytes...
            if (stream.skip(3) < 3) {
                return null;
            }
        }

        /*
         * The specifics of `getEncodingName` work is that it always returns
         * UTF-16 with BOM as either UTF-16LE or UTF-16BE, and
         * InputStreamReader doesn't expect BOM coming with UTF-16LE|BE
         * encoded data. So this BOM should also be removed, if present.
         */
        if ((count > 1) && (ENCODING.equals("UTF-16LE") || ENCODING.equals("UTF-16BE"))) {
            int b0 = b4[0] & 0xFF;
            int b1 = b4[1] & 0xFF;

            if (((b0 == 0xFF) && (b1 == 0xFE)) || ((b0 == 0xFE) && (b1 == 0xFF))) {
                // ignore first two bytes...
                if (stream.skip(2) < 2) {
                    return null;
                }
            }
        }

        Reader reader = null;

        /*
         * We must use custom class to read UCS-4 data, my JVM doesn't support
         * this encoding scheme by default and I doubt other JVMs are.
         *
         * There was another specific reader for UTF-8 encoding in Xerces
         * (org.apache.xerces.impl.io.UTF8Reader), which they say is
         * optimized one. May be it is really better than JVM's default
         * decoding algorithm but I doubt the necessity of porting just
         * another (not so small) class in order to "efficiently" extract
         * a couple of chars from XML declaration. Still I may be mistaking
         * there. Moreover, Xerces' UTF8Reader has some internal dependencies
         * and it will take much more effort to extract it from there.
         *
         * Also, at this stage it is quite impossible to have "ISO-10646-UCS-2"
         * as a value for ENCODING.
         *
         * You can avoid possible bugs in UCSReader by commenting out this
         * block of code together with following `if`. Then you will get an
         * UnsupportedEncodingException for UCS-4 encoded data.
         */
        if ("ISO-10646-UCS-4".equals(ENCODING)) {
            if (null != isBigEndian) {
                boolean isBE = isBigEndian.booleanValue();

                if (isBE) {
                    reader = new UCSReader(stream, UCSReader.UCS4BE);
                } else {
                    reader = new UCSReader(stream, UCSReader.UCS4LE);
                }
            } else {
                // Fatal error, UCSReader will fail to decode this properly
                String s = "Unsupported byte order for ISO-10646-UCS-4 encoding.";
                throw new UnsupportedCharsetException(s);
            }
        }

        if (null == reader) {
            reader = new InputStreamReader(stream, ENCODING);
        }

        //
        // Phase 2. Reading XML declaration and extracting charset info from it.
        String declEncoding = getXmlEncoding(reader);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Charset detection phase 2. Charset in XML declaration "
                            + "is `"
                            + declEncoding
                            + "`.");
        }

        stream.reset();

        /*
         * Now RewindableInputStream is allowed to return more than one byte
         * per read operation. It also will not buffer bytes read using
         * `read(byte[], int, int)` method.
         */
        stream.setChunkedMode(true);

        /*
         * Reusing existing reader if possible, creating new one only if
         * declared charset name differs from guessed one
         */
        if ((null != declEncoding) && !declEncoding.equals(ENCODING)) {
            /*
             * I believe that for UCS-2 encoding default UTF-16 reader
             * (which is already created at this time) should suffice
             * in most cases. Though, we can always construct a new
             * UCSReader instance, if I am wrong here.
             */
            if (!declEncoding.equals("ISO-10646-UCS-2")) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Declared charset differs from inferred one. "
                                    + "Trying to construct InputStreamReader for `"
                                    + declEncoding
                                    + "`.");
                }

                reader = new InputStreamReader(stream, declEncoding);
                encInfo.setEncoding(declEncoding);
            }
        }

        return reader;
    } // END getCharsetAwareReader(InputStream) : Reader

    /**
     * Use this variant when you aren't interested in encoding data, and just want to get a suitable
     * reader for incoming request.
     *
     * @param istream See <code>getCharsetAwareReader(InputStream,
     *                              EncodingInfo)</code>.
     */
    public static Reader getCharsetAwareReader(InputStream istream)
            throws IOException, UnsupportedCharsetException {
        return getCharsetAwareReader(istream, new EncodingInfo());
    }

    /**
     * Creates a new reader on top of the given <code>InputStream</code> using existing (external)
     * encoding information. Unlike <code>getCharsetAwareReader</code>, this method never tries to
     * detect charset or encoding scheme of <code>InputStream</code>'s data. This also means that it
     * <em>must</em> be provided with valid <code>EncodingInfo</code> instance, which may be
     * obtained, for example, from previous <code>getCharsetAwareReader(InputStream, EncodingInfo)
     * </code> call.
     *
     * @param istream byte-stream containing textual (presumably XML) data
     * @param encInfo correctly initialized object which holds information of the above
     *     byte-stream's contents charset.
     * @throws IllegalArgumentException if charset name is not specified
     * @throws UnsupportedEncodingException in cases when specified charset is not supported by
     *     platform or due to invalid byte order for <code>ISO-10646-UCS-2|4</code> charsets.
     */
    public static Reader createReader(InputStream istream, EncodingInfo encInfo)
            throws IllegalArgumentException, UnsupportedEncodingException {
        String charset = encInfo.getEncoding();
        Boolean isBigEndian = encInfo.isBigEndian();

        // We MUST know encoding (in fact, charset) name, and as EncodingInfo
        // have non-arg constructor, its `getEncoding` can return null.
        if (null == charset) {
            String s = "Name of the charset must not be NULL!";
            throw new IllegalArgumentException(s);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Trying to create reader basing on existing charset "
                            + "information: `"
                            + encInfo
                            + "`.");
        }

        Reader reader = null;

        // UCS-2|4 charsets are handled with custom reader
        if ("ISO-10646-UCS-4".equals(charset)) {
            if (null != isBigEndian) {
                boolean isBE = isBigEndian.booleanValue();

                if (isBE) {
                    reader = new UCSReader(istream, UCSReader.UCS4BE);
                } else {
                    reader = new UCSReader(istream, UCSReader.UCS4LE);
                }
            } else {
                // Fatal error, UCSReader will fail to decode this properly
                String s = "Unsupported byte order for ISO-10646-UCS-4 encoding.";
                throw new UnsupportedEncodingException(s);
            }
        } else if ("ISO-10646-UCS-2".equals(charset)) {
            if (null != isBigEndian) {
                boolean isBE = isBigEndian.booleanValue();

                if (isBE) {
                    reader = new UCSReader(istream, UCSReader.UCS4BE);
                } else {
                    reader = new UCSReader(istream, UCSReader.UCS4LE);
                }
            } else {
                // Cannot construct UCSReader without byte order info
                String s = "Byte order must be specified for ISO-10646-UCS-2.";
                throw new UnsupportedEncodingException(s);
            }
        } else {
            reader = new InputStreamReader(istream, charset);
        }

        return reader;
    } // END createReader(InputStream, EncodingInfo) : Reader

    /**
     * Returns the IANA encoding name that is auto-detected from the bytes specified, with the
     * endian-ness of that encoding where appropriate. Note, that encoding obtained this way is only
     * an <em>encoding scheme</em> of the request, i.e. step 1 of detection process. To learn the
     * exact <em>charset</em> of the request data, you should also perform step 2 - read XML
     * declaration and get the value of its <code>encoding</code> pseudoattribute.
     *
     * @param b4 The first four bytes of the input.
     * @param count The number of bytes actually read.
     * @return Instance of EncodingInfo incapsulating all encoding-related data.
     */
    public static EncodingInfo getEncodingName(byte[] b4, int count) {
        if (count < 2) {
            return new EncodingInfo("UTF-8", null);
        }

        // UTF-16, with BOM
        int b0 = b4[0] & 0xFF;
        int b1 = b4[1] & 0xFF;

        if ((b0 == 0xFE) && (b1 == 0xFF)) {
            // UTF-16, big-endian
            return new EncodingInfo("UTF-16BE", Boolean.TRUE, true);
        }

        if ((b0 == 0xFF) && (b1 == 0xFE)) {
            // UTF-16, little-endian
            return new EncodingInfo("UTF-16LE", Boolean.FALSE, true);
        }

        // default to UTF-8 if we don't have enough bytes to make a
        // good determination of the encoding
        if (count < 3) {
            return new EncodingInfo("UTF-8", null);
        }

        // UTF-8 with a BOM
        int b2 = b4[2] & 0xFF;

        if ((b0 == 0xEF) && (b1 == 0xBB) && (b2 == 0xBF)) {
            return new EncodingInfo("UTF-8", null, true);
        }

        // default to UTF-8 if we don't have enough bytes to make a
        // good determination of the encoding
        if (count < 4) {
            return new EncodingInfo("UTF-8", null);
        }

        // other encodings
        int b3 = b4[3] & 0xFF;

        if ((b0 == 0x00) && (b1 == 0x00) && (b2 == 0x00) && (b3 == 0x3C)) {
            // UCS-4, big endian (1234)
            return new EncodingInfo("ISO-10646-UCS-4", Boolean.TRUE);
        }

        if ((b0 == 0x3C) && (b1 == 0x00) && (b2 == 0x00) && (b3 == 0x00)) {
            // UCS-4, little endian (4321)
            return new EncodingInfo("ISO-10646-UCS-4", Boolean.FALSE);
        }

        if ((b0 == 0x00) && (b1 == 0x00) && (b2 == 0x3C) && (b3 == 0x00)) {
            // UCS-4, unusual octet order (2143)
            // REVISIT: What should this be? (Currently this would be
            // an exception :)
            return new EncodingInfo("ISO-10646-UCS-4", null);
        }

        if ((b0 == 0x00) && (b1 == 0x3C) && (b2 == 0x00) && (b3 == 0x00)) {
            // UCS-4, unusual octect order (3412)
            // REVISIT: What should this be?
            return new EncodingInfo("ISO-10646-UCS-4", null);
        }

        if ((b0 == 0x00) && (b1 == 0x3C) && (b2 == 0x00) && (b3 == 0x3F)) {
            // UTF-16, big-endian, no BOM
            // (or could turn out to be UCS-2...
            // REVISIT: What should this be?
            return new EncodingInfo("UTF-16BE", Boolean.TRUE);
        }

        if ((b0 == 0x3C) && (b1 == 0x00) && (b2 == 0x3F) && (b3 == 0x00)) {
            // UTF-16, little-endian, no BOM
            // (or could turn out to be UCS-2...
            return new EncodingInfo("UTF-16LE", Boolean.FALSE);
        }

        if ((b0 == 0x4C) && (b1 == 0x6F) && (b2 == 0xA7) && (b3 == 0x94)) {
            // EBCDIC
            // a la xerces1, return CP037 instead of EBCDIC here
            return new EncodingInfo("CP037", null);
        }

        // default encoding
        return new EncodingInfo("UTF-8", null);
    } // END getEncodingName(byte[], int) : EncodingInfo

    /**
     * Gets the encoding of the xml request made to the dispatcher. This works by reading the temp
     * file where we are storing the request, looking to match the header specified encoding that
     * should be present on all xml files. This call should only be made after the temp file has
     * been set. If no encoding is found, or if an IOError is encountered then null shall be
     * returned.
     *
     * @param reader This character stream is supposed to contain XML data (i.e. it should start
     *     with valid XML declaration).
     * @return The encoding specified in the xml header read from the supplied character stream.
     */
    protected static String getXmlEncoding(Reader reader) {
        try {
            StringWriter sw = new StringWriter(MAX_XMLDECL_SIZE);

            int c;
            int count = 0;

            for (; (6 > count) && (-1 != (c = reader.read())); count++) {
                sw.write(c);
            }

            /*
             * Hmm, checking for the case when there is no XML declaration and
             * document begins with processing instruction whose target name
             * starts with "<?xml" ("<?xmlfoo"). Sounds like a nearly impossible
             * thing, but Xerces guys are checking for that somewhere in the
             * depths of their code :)
             */
            if ((6 > count) || (!"<?xml ".equals(sw.toString()))) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Invalid(?) XML declaration: " + sw.toString() + ".");
                }

                return null;
            }

            /*
             * Continuing reading declaration(?) til the first '>' ('\u003E')
             * encountered. Conversion from `int` to `char` should be safe
             * for our purposes, at least I'm not expecting any extended
             * (0x10000+) characters in xml declaration. I also limited
             * the total number of chars read this way to prevent any
             * malformed (no '>') input potentially forcing us to read
             * megabytes of useless data :)
             */
            for (;
                    (MAX_XMLDECL_SIZE > count)
                            && (-1 != (c = reader.read()))
                            && (RIGHT_ANGLE_BRACKET != (char) c);
                    count++) {
                sw.write(c);
            }

            Matcher m = ENCODING_PATTERN.matcher(sw.toString());

            if (m.find()) {
                String result = m.group(1);

                return result;
            } else {
                return null;
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(
                        "Failed to extract charset info from XML "
                                + "declaration due to IOException: "
                                + e.getMessage());
            }

            return null;
        }
    } // END getXmlEncoding(Reader) : String
} // END class XmlCharsetDetector
