package org.jvnet.mimepull;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Customized class from MIMEPart to support partial message. Everything overridden because of base class private variables.
 *
 * @author wayneng
 *
 */
public class MIMEPartPartial extends MIMEPart {

    /** No change from base class. */
    private volatile boolean closed;
    /** No change from base class. */
    private volatile InternetHeaders headers;
    /** No change from base class. */
    private volatile String contentId;
    /** No change from base class. */
    private String contentType;
    /** No change from base class. */
    private String contentTransferEncoding;
    /** No change from base class. */
    private volatile boolean isPartial;

    // volatile boolean parsed; // part is parsed or not
    /** No change from base class. */
    final MIMEMessagePartial msg;
    /** No change from base class. */
    private final DataHead dataHead;

    /** No change from base class. */
    private final Object lock = new Object();

    /**
     * No change from base class.
     *
     * @param msg MIMEMessagePartial.
     */
    MIMEPartPartial(final MIMEMessagePartial msg) {
        super(msg);
        this.msg = msg;
        this.dataHead = new DataHead(this);
    }

    /**
     * No change from base class.
     *
     * @param msg MIMEMessagePartial.
     * @param contentId String.
     */
    MIMEPartPartial(final MIMEMessagePartial msg, final String contentId) {
        this(msg);
        this.contentId = contentId;
    }

    /**
     * No change from base class.
     *
     * Can get the attachment part's content multiple times. That means the full content needs to be there in memory or on the file system. Calling
     * this method would trigger parsing for the part's data. So do not call this unless it is required(otherwise, just wrap MIMEPart into a object
     * that returns InputStream for e.g DataHandler)
     *
     * @return data for the part's content
     */
    @Override
    public InputStream read() {
        InputStream is = null;
        try {
            is = MimeUtility.decode(dataHead.read(), contentTransferEncoding);
        } catch (DecodingException ex) { // ignore
            // logging goes here
        }
        return is;
    }

    /**
     * No change from base class.
     *
     * Cleans up any resources that are held by this part (for e.g. deletes the temp file that is used to serve this part's content). After calling
     * this, one shouldn't call {@link #read()} or {@link #readOnce()}
     */
    @Override
    public void close() {
        if (!closed) {
            synchronized (lock) {
                if (!closed) {
                    dataHead.close();
                    closed = true;
                }
            }
        }
    }

    /**
     * No change from base class.
     *
     * Can get the attachment part's content only once. The content will be lost after the method. Content data is not be stored on the file system or
     * is not kept in the memory for the following case: - Attachement parts contents are accessed sequentially
     *
     * In general, take advantage of this when the data is used only once.
     *
     * @return data for the part's content
     */
    @Override
    public InputStream readOnce() {
        InputStream is = null;
        try {
            is = MimeUtility.decode(dataHead.readOnce(), contentTransferEncoding);
        } catch (DecodingException ex) { // ignore
            // logging goes here
        }
        return is;
    }

    /**
     * No change from base class.
     */
    @Override
    public void moveTo(final File f) {
        dataHead.moveTo(f);
    }

    /**
     * No change from base class.
     *
     * Returns Content-ID MIME header for this attachment part
     *
     * @return Content-ID of the part
     */
    @Override
    public String getContentId() {
        if (contentId == null) {
            getHeaders();
        }
        return contentId;
    }

    /**
     * No change from base class.
     *
     * Returns Content-Transfer-Encoding MIME header for this attachment part
     *
     * @return Content-Transfer-Encoding of the part
     */
    @Override
    public String getContentTransferEncoding() {
        if (contentTransferEncoding == null) {
            getHeaders();
        }
        return contentTransferEncoding;
    }

    /**
     * No change from base class.
     *
     * Returns Content-Type MIME header for this attachment part
     *
     * @return Content-Type of the part
     */
    @Override
    public String getContentType() {
        if (contentType == null) {
            getHeaders();
        }
        return contentType;
    }

    /**
     * No change from base class.
     *
     */
    private void getHeaders() {
        // Trigger parsing for the part headers
        while (headers == null) {
            if (!msg.makeProgress()) {
                if (headers == null) {
                    throw new IllegalStateException("Internal Error. Didn't get Headers even after complete parsing.");
                }
            }
        }
    }

    /**
     * No change from base class.
     *
     * Return all the values for the specified header. Returns <code>null</code> if no headers with the specified name exist.
     *
     * @param name header name
     * @return list of header values, or null if none
     */
    @Override
    public List<String> getHeader(final String name) {
        getHeaders();
        assert headers != null;
        return headers.getHeader(name);
    }

    /**
     * No change from base class.
     *
     * Return all the headers
     *
     * @return list of Header objects
     */
    @Override
    public List<? extends Header> getAllHeaders() {
        getHeaders();
        assert headers != null;
        return headers.getAllHeaders();
    }

    /**
     * No change from base class.
     *
     * Callback to set headers
     *
     * @param headers MIME headers for the part
     */
    @Override
    void setHeaders(final InternetHeaders headers) {
        this.headers = headers;
        List<String> ct = getHeader("Content-Type");
        this.contentType = (ct == null) ? "application/octet-stream" : ct.get(0);
        // this.contentType = (ct == null) ? "" : ct.get(0);
        List<String> cte = getHeader("Content-Transfer-Encoding");
        this.contentTransferEncoding = (cte == null) ? "binary" : cte.get(0);
    }

    /**
     * Callback to set contentPartial.
     *
     * I don't think this needs to be synchronized.
     *
     * @param isPartial boolean.
     */
    void setIsPartial(final boolean isPartial) {
        this.isPartial = isPartial;
    }

    /**
     * Gets if MIMEPart is partial content.
     *
     * @return isPartial boolean.
     */
    public boolean getIsPartial() {
        return isPartial;
    }

    /**
     * No change from base class.
     *
     * Callback to notify that there is a partial content for the part
     *
     * @param buf content data for the part
     */
    @Override
    void addBody(final ByteBuffer buf) {
        dataHead.addBody(buf);
    }

    /**
     * No change from base class.
     *
     * Callback to indicate that parsing is done for this part (no more update events for this part)
     */
    @Override
    void doneParsing() {
        parsed = true;
        dataHead.doneParsing();
    }

    /**
     * No change from base class.
     *
     * Callback to set Content-ID for this part
     *
     * @param cid Content-ID of the part
     */
    @Override
    void setContentId(final String cid) {
        this.contentId = cid;
    }

    /**
     * No change from base class.
     *
     * Callback to set Content-Transfer-Encoding for this part
     *
     * @param cte Content-Transfer-Encoding of the part
     */
    @Override
    void setContentTransferEncoding(final String cte) {
        this.contentTransferEncoding = cte;
    }

    /**
     * Return {@code true} if this part has already been closed, {@code false} otherwise.
     *
     * @return {@code true} if this part has already been closed, {@code false} otherwise.
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * No change from base class.
     */
    @Override
    public String toString() {
        return "Part=" + contentId + ":" + contentTransferEncoding;
    }

}
