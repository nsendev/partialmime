package org.jvnet.mimepull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Customized MIMEParser. All methods overridden because base class has privates.
 *
 * @author wayneng
 *
 */
class MIMEParserPartial extends MIMEParser {

    /** No change from base class. */
    private static final String HEADER_ENCODING = "ISO8859-1";

    // Actually, the grammar doesn't support whitespace characters
    // after boundary. But the mail implementation checks for it.
    // We will only check for these many whitespace characters after boundary
    /** No change from base class. */
    private static final int NO_LWSP = 1000;

    /** No change from base class. */
    private enum STATE {
        /** No change from base class. */
        START_MESSAGE,
        /** No change from base class. */
        SKIP_PREAMBLE,
        /** No change from base class. */
        START_PART,
        /** No change from base class. */
        HEADERS, BODY,
        /** No change from base class. */
        END_PART,
        /** No change from base class. */
        END_MESSAGE
    }

    /** No change from base class. */
    private STATE state = STATE.START_MESSAGE;

    /** No change from base class. */
    private final InputStream in;
    /** No change from base class. */
    private final byte[] bndbytes;
    /** No change from base class. */
    private final int bl;
    /** This is derived from base class. */
    private final MIMEConfigYM config;
    /** No change from base class. */
    private final int[] bcs = new int[128]; // BnM algo: Bad Character Shift table
    /** No change from base class. */
    private final int[] gss; // BnM algo : Good Suffix Shift table

    /**
     * Have we parsed the data from our InputStream yet?
     */
    private boolean parsed;

    /*
     * Read and process body partsList until we see the terminating boundary line (or EOF).
     */
    /** No change from base class. */
    private boolean done = false;

    /** No change from base class. */
    private boolean eof;
    /** No change from base class. */
    private final int capacity;
    /** No change from base class. */
    private byte[] buf;
    /** No change from base class. */
    private int len;
    /** No change from base class. */
    private boolean bol; // beginning of the line
    /** boolean for partial messages. */
    protected boolean isPartial = false;
    /** maxInputStreamSize limit. */
    protected final int maxInputStreamSize;
    /** counter for number of bytes read from InputStream. */
    private int totalBytesRead;
    /** truncate size for partial message. */
    protected final int moduloTruncateSize = 4;


    /**
     * Parses the MIME content. At the EOF, it also closes input stream.
     *
     * This calls super with min memory alloc, adds totalBytes behavior. Made params final. No other changes from base class.
     *
     * @param in InputStream.
     * @param boundary String.
     * @param config MIMEConfig.
     */
    MIMEParserPartial(final InputStream in, final String boundary, final MIMEConfigYM config) {
        super(new ByteArrayInputStream(new String("").getBytes(StandardCharsets.UTF_8)), boundary, new MIMEConfigYM(false, 1, 2, false));
        this.in = in;
        this.bndbytes = getBytes("--" + boundary);
        bl = bndbytes.length;
        this.config = config;
        gss = new int[bl];
        this.totalBytesRead = 0;
        this.maxInputStreamSize = config.getMaxMIMESize();
        compileBoundaryPattern();

        // \r\n + boundary + "--\r\n" + lots of LWSP
        capacity = config.chunkSize + 2 + bl + 4 + NO_LWSP;
        createBuf(capacity);
    }

    /**
     * No change from base class. Returns iterator for the parsing events. Use the iterator to advance the parsing.
     *
     * @return iterator for parsing events
     */
    @Override
    public Iterator<MIMEEvent> iterator() {
        return new MIMEEventIterator();
    }

    /**
     * Customized from base class for partial message enabling. Mostly no change from base class.
     *
     * @author wayneng
     *
     */
    class MIMEEventIterator implements Iterator<MIMEEvent> {

        /**
         * No change from base class.
         */
        @Override
        public boolean hasNext() {
            return !parsed;
        }

        /**
         * This is the next implementation for iterator.
         *
         * Only BODY case is changed to support partial, and logging removed. No change from base class.
         */
        @Override
        public MIMEEvent next() {
            switch (state) {
            case START_MESSAGE:
                state = STATE.SKIP_PREAMBLE;
                return MIMEEvent.START_MESSAGE;

            case SKIP_PREAMBLE:
                skipPreamble();
                // fall through
            case START_PART:
                state = STATE.HEADERS;
                return MIMEEvent.START_PART;

            case HEADERS:
                InternetHeaders ih = readHeaders();
                state = STATE.BODY;
                bol = true;
                return new MIMEEvent.Headers(ih);

            case BODY:
                ByteBuffer buf = readBody();
                bol = false;
                if (isPartial) {
                    return new MIMEEventPartial.ContentPartial(buf, isPartial);
                }
                return new MIMEEventPartial.ContentPartial(buf);

            case END_PART:
                if (done) {
                    state = STATE.END_MESSAGE;
                } else {
                    state = STATE.START_PART;
                }
                return MIMEEventPartial.END_PART;

            case END_MESSAGE:
                parsed = true;
                return MIMEEvent.END_MESSAGE;

            default:
                throw new MIMEParsingException("Unknown Parser state = " + state);
            }
        }

        /**
         * No change from base class.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * No change from base class. Collects the headers for the current part by parsing mesage stream.
     *
     * @return headers for the current part
     */
    private InternetHeaders readHeaders() {
        if (!eof) {
            fillBuf();
        }
        return new InternetHeaders(new LineInputStream());
    }

    /**
     * Only logging is removed and partial message enabling added. No other change from base class.
     *
     * Reads and saves the part of the current attachment part's content. At the end of this method, buf should have the remaining data at index 0.
     *
     * @return a chunk of the part's content
     *
     */
    private ByteBuffer readBody() {
        if (!eof) {
            fillBuf();
        }
        int start = match(buf, 0, len); // matches boundary
        if (start == -1) {
            // No boundary is found
            assert eof || len >= config.chunkSize;
            int chunkSize = eof ? len : config.chunkSize;
            if (eof) {
                done = true;
                if (config.isEnablePartial()) {
                    // This is for partial message parsing.
                    isPartial = true;
                    state = STATE.END_PART;
                    // This is for truncating at 4 byte boundaries for base64 encoding, and don't care for other encoding formats.
                    final int remaining = len - chunkSize;
                    final int chunkSizeMod4 = chunkSize % moduloTruncateSize;
                    final int chunkSizeTruncated = chunkSize - chunkSizeMod4;
                    return adjustBuf(chunkSizeTruncated, remaining);
                } else {
                    throw new MIMEParsingException("Reached EOF, but there is no closing MIME boundary.");
                }
            }
            return adjustBuf(chunkSize, len - chunkSize);
        }
        // Found boundary.
        // Is it at the start of a line ?
        int chunkLen = start;
        if (bol && start == 0) {
            // nothing to do
        } else if (start > 0 && (buf[start - 1] == '\n' || buf[start - 1] == '\r')) {
            --chunkLen;
            if (buf[start - 1] == '\n' && start > 1 && buf[start - 2] == '\r') {
                --chunkLen;
            }
        } else {
            return adjustBuf(start + 1, len - start - 1); // boundary is not at beginning of a line
        }

        if (start + bl + 1 < len && buf[start + bl] == '-' && buf[start + bl + 1] == '-') {
            state = STATE.END_PART;
            done = true;
            return adjustBuf(chunkLen, 0);
        }

        // Consider all the whitespace in boundary+whitespace+"\r\n"
        int lwsp = 0;
        for (int i = start + bl; i < len && (buf[i] == ' ' || buf[i] == '\t'); i++) {
            ++lwsp;
        }

        // Check for \n or \r\n in boundary+whitespace+"\n" or boundary+whitespace+"\r\n"
        if (start + bl + lwsp < len && buf[start + bl + lwsp] == '\n') {
            state = STATE.END_PART;
            return adjustBuf(chunkLen, len - start - bl - lwsp - 1);
        } else if (start + bl + lwsp + 1 < len && buf[start + bl + lwsp] == '\r' && buf[start + bl + lwsp + 1] == '\n') {
            state = STATE.END_PART;
            return adjustBuf(chunkLen, len - start - bl - lwsp - 2);
        } else if (start + bl + lwsp + 1 < len) {
            return adjustBuf(chunkLen + 1, len - chunkLen - 1); // boundary string in a part data
        } else if (eof) {
            done = true;
            throw new MIMEParsingException("Reached EOF, but there is no closing MIME boundary.");
        }

        // Some more data needed to determine if it is indeed a proper boundary
        return adjustBuf(chunkLen, len - chunkLen);
    }

    /**
     * No change from base class.
     *
     * Returns a chunk from the original buffer. A new buffer is created with the remaining bytes.
     *
     * @param chunkSize create a chunk with these many bytes
     * @param remaining bytes from the end of the buffer that need to be copied to the beginning of the new buffer
     * @return chunk
     */
    private ByteBuffer adjustBuf(final int chunkSize, final int remaining) {
        assert buf != null;
        assert chunkSize >= 0;
        assert remaining >= 0;

        byte[] temp = buf;
        // create a new buf and adjust it without this chunk
        createBuf(remaining);
        System.arraycopy(temp, len - remaining, buf, 0, remaining);
        len = remaining;

        return ByteBuffer.wrap(temp, 0, chunkSize);
    }

    /**
     * No change from base class.
     *
     * @param min int.
     */
    private void createBuf(final int min) {
        buf = new byte[min < capacity ? capacity : min];
    }

    /**
     * Skips the preamble to find the first attachment part.
     *
     * No change from base class except logging removed.
     *
     */
    private void skipPreamble() {

        while (true) {
            if (!eof) {
                fillBuf();
            }
            int start = match(buf, 0, len); // matches boundary
            if (start == -1) {
                // No boundary is found
                if (eof) {
                    throw new MIMEParsingException("Missing start boundary");
                } else {
                    adjustBuf(len - bl + 1, bl - 1);
                    continue;
                }
            }

            if (start > config.chunkSize) {
                adjustBuf(start, len - start);
                continue;
            }
            // Consider all the whitespace boundary+whitespace+"\r\n"
            int lwsp = 0;
            for (int i = start + bl; i < len && (buf[i] == ' ' || buf[i] == '\t'); i++) {
                ++lwsp;
            }
            // Check for \n or \r\n
            if (start + bl + lwsp < len && (buf[start + bl + lwsp] == '\n' || buf[start + bl + lwsp] == '\r')) {
                if (buf[start + bl + lwsp] == '\n') {
                    adjustBuf(start + bl + lwsp + 1, len - start - bl - lwsp - 1);
                    break;
                } else if (start + bl + lwsp + 1 < len && buf[start + bl + lwsp + 1] == '\n') {
                    adjustBuf(start + bl + lwsp + 2, len - start - bl - lwsp - 2);
                    break;
                }
            }
            adjustBuf(start + 1, len - start - 1);
        }
    }

    /**
     * No change from base class.
     *
     * @param s String.
     * @return byte array.
     */
    private static byte[] getBytes(final String s) {
        char[] chars = s.toCharArray();
        int size = chars.length;
        byte[] bytes = new byte[size];

        for (int i = 0; i < size;) {
            bytes[i] = (byte) chars[i++];
        }
        return bytes;
    }

    /**
     * No change from base class. Boyer-Moore search method. Copied from java.util.regex.Pattern.java
     *
     * Pre calculates arrays needed to generate the bad character shift and the good suffix shift. Only the last seven bits are used to see if chars
     * match; This keeps the tables small and covers the heavily used ASCII range, but occasionally results in an aliased match for the bad character
     * shift.
     */
    private void compileBoundaryPattern() {
        int i, j;

        // Precalculate part of the bad character shift
        // It is a table for where in the pattern each
        // lower 7-bit value occurs
        for (i = 0; i < bndbytes.length; i++) {
            bcs[bndbytes[i] & 0x7F] = i + 1;
        }

        // Precalculate the good suffix shift
        // i is the shift amount being considered
        NEXT: for (i = bndbytes.length; i > 0; i--) {
            // j is the beginning index of suffix being considered
            for (j = bndbytes.length - 1; j >= i; j--) {
                // Testing for good suffix
                if (bndbytes[j] == bndbytes[j - i]) {
                    // src[j..len] is a good suffix
                    gss[j - 1] = i;
                } else {
                    // No match. The array has already been
                    // filled up with correct values before.
                    continue NEXT;
                }
            }
            // This fills up the remaining of optoSft
            // any suffix can not have larger shift amount
            // then its sub-suffix. Why???
            while (j > 0) {
                gss[--j] = i;
            }
        }
        // Set the guard value because of unicode compression
        gss[bndbytes.length - 1] = 1;
    }

    /**
     * Finds the boundary in the given buffer using Boyer-Moore algo. Copied from java.util.regex.Pattern.java Params changed to final and added
     * copied variable because of final variables. No other change from base class.
     *
     * @param mybuf boundary to be searched in this mybuf
     * @param off start index in mybuf
     * @param len number of bytes in mybuf
     *
     * @return -1 if there is no match or index where the match starts
     */
    private int match(final byte[] mybuf, final int off, final int len) {
        int last = len - bndbytes.length;
        int offset = off;

        // Loop over all possible match positions in text
        NEXT: while (offset <= last) {
            // Loop over pattern from right to left
            for (int j = bndbytes.length - 1; j >= 0; j--) {
                byte ch = mybuf[offset + j];
                if (ch != bndbytes[j]) {
                    // Shift search to the right by the maximum of the
                    // bad character shift and the good suffix shift
                    offset += Math.max(j + 1 - bcs[ch & 0x7F], gss[j]);
                    continue NEXT;
                }
            }
            // Entire pattern matched starting at off
            return offset;
        }
        return -1;
    }

    /**
     * Adds max bytes read from InputStream behavior. No change from base class.
     *
     * Fills the remaining buf to the full capacity
     */
    private void fillBuf() {
        assert !eof;
        while (len < buf.length) {
            int read;
            try {
                read = in.read(buf, len, buf.length - len);
            } catch (IOException ioe) {
                final String msg = ioe.getMessage();
                if (msg != null && msg.equals("Early EOF")) {
                    read = -1;
                } else {
                    throw new MIMEParsingException(ioe);
                }
            }
            if (read == -1) {
                eof = true;
                try {
                    in.close();
                } catch (IOException ioe) {
                    throw new MIMEParsingException(ioe);
                }
                break;
            } else {
                len += read;
                this.totalBytesRead += read;
                if (this.totalBytesRead > maxInputStreamSize) {
                    throw new MIMEParsingException("MIMEParserYM total bytes read exceeds limit of 45 MB.");
                }
            }
        }
    }

    /**
     * No change from base class.
     */
    private void doubleBuf() {
        byte[] temp = new byte[2 * len];
        System.arraycopy(buf, 0, temp, 0, len);
        buf = temp;
        if (!eof) {
            fillBuf();
        }
    }

    /**
     * No change from base class.
     *
     * @author wayneng
     *
     */
    class LineInputStream extends MIMEParser.LineInputStream {
        /** No change from base class. */
        private int offset;

        /**
         * No change from base class.
         *
         * Read a line containing only ASCII characters from the input stream. A line is terminated by a CR or NL or CR-NL sequence. A common error is
         * a CR-CR-NL sequence, which will also terminate a line. The line terminator is not returned as part of the returned String. Returns null if
         * no data is available.
         * <p>
         *
         * This class is similar to the deprecated <code>DataInputStream.readLine()</code>
         */
        @Override
        public String readLine() throws IOException {

            int hdrLen = 0;
            int lwsp = 0;
            while (offset + hdrLen < len) {
                if (buf[offset + hdrLen] == '\n') {
                    lwsp = 1;
                    break;
                }
                if (offset + hdrLen + 1 == len) {
                    doubleBuf();
                }
                if (offset + hdrLen + 1 >= len) { // No more data in the stream
                    assert eof;
                    return null;
                }
                if (buf[offset + hdrLen] == '\r' && buf[offset + hdrLen + 1] == '\n') {
                    lwsp = 2;
                    break;
                }
                ++hdrLen;
            }
            if (hdrLen == 0) {
                adjustBuf(offset + lwsp, len - offset - lwsp);
                return null;
            }

            String hdr = new String(buf, offset, hdrLen, HEADER_ENCODING);
            offset += hdrLen + lwsp;
            return hdr;
        }

    }
}
