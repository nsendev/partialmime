package org.jvnet.mimepull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Customized from MIMEMessage. Methods are overridden because base class has all private variables.
 *
 * @author wayneng
 *
 */
public class MIMEMessagePartial extends MIMEMessage {

    // MIMEConfig config;

    /** No change from base class. */
    private final InputStream in;
    /** No change from base class. */
    private final Iterator<MIMEEvent> it;
    /** No change from base class. */
    private boolean parsed; // true when entire message is parsed
    /** No change from base class. */
    private MIMEPartPartial currentPart;
    /** No change from base class. */
    private int currentIndex;

    /** preallocate lists and hashmap. */
    private final int initialCapacity = 20;
    /** No change from base class. */
    private final List<MIMEPartPartial> partsList = new ArrayList<>(initialCapacity);
    /** No change from base class. */
    private final Map<String, MIMEPartPartial> partsMap = new HashMap<>(initialCapacity);
    /** boolean for partial message. */
    private boolean isPartial = false;

    /**
     * see MIMEMessage.MIMEMessage(InputStream, String, MIMEConfig)
     * 
     * @param in InputStream.
     * @param boundary String.
     */
    public MIMEMessagePartial(final InputStream in, final String boundary) {
        this(in, boundary, new MIMEConfigYM());
    }

    /**
     * Creates a MIME message from the content's stream. The content stream is closed when EOF is reached.
     *
     * @param in MIME message stream
     * @param boundary the separator for parts(pass it without --)
     * @param config various configuration parameters
     */
    public MIMEMessagePartial(final InputStream in, final String boundary, final MIMEConfigYM config) {
        super(new ByteArrayInputStream(new String("").getBytes(StandardCharsets.UTF_8)), boundary, config);
        this.in = in;
        this.config = config;
        MIMEParserPartial parser = new MIMEParserPartial(in, boundary, config);
        it = parser.iterator();

        if (config.isParseEagerly()) {
            parseAllPartial();
        }
    }

    /**
     * No change from base class.
     *
     * Gets all the attachments by parsing the entire MIME message. Avoid this if possible since it is an expensive operation.
     *
     * @return list of attachments.
     */
    public List<MIMEPartPartial> getAttachmentsPartial() {
        if (!parsed) {
            parseAllPartial();
        }
        return partsList;
    }

    /**
     * No change from base class except logging removed.
     *
     * Creates nth attachment lazily. It doesn't validate if the message has so many attachments. To do the validation, the message needs to be
     * parsed. The parsing of the message is done lazily and is done while reading the bytes of the part.
     *
     * @param index sequential order of the part. starts with zero.
     * @return attachemnt part
     */
    @Override
    public MIMEPartPartial getPart(final int index) {
        MIMEPartPartial part = (index < partsList.size()) ? partsList.get(index) : null;
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no " + index + " attachment part ");
        }
        if (part == null) {
            // Parsing will done lazily and will be driven by reading the part
            part = new MIMEPartPartial(this);
            partsList.add(index, part);
        }
        return part;
    }

    /**
     * No change from base class.
     *
     * Gets if MIMEMessage is partial payload.
     *
     * @return isPartial
     */
    public boolean getIsPartial() {
        if (!parsed) {
            parseAllPartial();
        }
        return isPartial;
    }

    /**
     * No change from base class except logging removed.
     *
     * Creates a lazy attachment for a given Content-ID. It doesn't validate if the message contains an attachment with the given Content-ID. To do
     * the validation, the message needs to be parsed. The parsing of the message is done lazily and is done while reading the bytes of the part.
     *
     * @param contentId Content-ID of the part, expects Content-ID without brackets.
     * @return attachemnt part
     */
    @Override
    public MIMEPartPartial getPart(final String contentId) {
        MIMEPartPartial part = getDecodedCidPart(contentId);
        if (parsed && part == null) {
            throw new MIMEParsingException("There is no attachment part with Content-ID = " + contentId);
        }
        if (part == null) {
            // Parsing is done lazily and is driven by reading the part
            part = new MIMEPartPartial(this, contentId);
            partsMap.put(contentId, part);
        }
        return part;
    }

    /**
     * No change from base class.
     *
     * @param cid String.
     * @return MIMEPartPartial.
     */
    // this is required for Indigo interop, it writes content-id without escaping
    private MIMEPartPartial getDecodedCidPart(final String cid) {
        MIMEPartPartial part = partsMap.get(cid);
        if (part == null) {
            if (cid.indexOf('%') != -1) {
                try {
                    String tempCid = URLDecoder.decode(cid, "utf-8");
                    part = partsMap.get(tempCid);
                } catch (UnsupportedEncodingException ue) {
                    // Ignore it
                }
            }
        }
        return part;
    }

    /**
     * No change from base class.
     */
    public void parseAllPartial() {
        while (makeProgress()) {
            // Nothing to do
        }
    }

    /**
     * Closes all parsed {@link org.jvnet.mimepull.MIMEPartPartial parts}. This method is safe to call even if parsing of message failed.
     * 
     * Does not throw {@link org.jvnet.mimepull.MIMEParsingException} if an error occurred during closing a MIME part. The exception (if any) is still
     * logged.
     */
    @Override
    public void close() {
        close(partsList);
        close(partsMap.values());
    }

    /**
     * No change from base class, only removed logging.
     *
     * @param parts Collection.
     */
    private void close(final Collection<MIMEPartPartial> parts) {
        for (final MIMEPartPartial part : parts) {
            try {
                part.close();
            } catch (final MIMEParsingException closeError) {
                // logging goes here.
            }
        }
    }

    /**
     * Only changed case CONTENT for partial message enabling and removed logging. No other change from base class.
     *
     * Parses the MIME message in a pull fashion.
     *
     * @return false if the parsing is completed.
     */
    @Override
    public synchronized boolean makeProgress() {
        if (!it.hasNext()) {
            return false;
        }
        MIMEEvent event = it.next();

        switch (event.getEventType()) {
        case START_MESSAGE:
            break;

        case START_PART:
            break;

        case HEADERS:
            MIMEEventPartial.Headers headers = (MIMEEventPartial.Headers) event;
            InternetHeaders ih = headers.getHeaders();
            List<String> cids = ih.getHeader("content-id");
            String cid = (cids != null) ? cids.get(0) : currentIndex + "";
            if (cid.length() > 2 && cid.charAt(0) == '<') {
                cid = cid.substring(1, cid.length() - 1);
            }
            MIMEPartPartial listPart = (currentIndex < partsList.size()) ? partsList.get(currentIndex) : null;
            MIMEPartPartial mapPart = getDecodedCidPart(cid);
            if (listPart == null && mapPart == null) {
                currentPart = getPart(cid);
                partsList.add(currentIndex, currentPart);
            } else if (listPart == null) {
                currentPart = mapPart;
                partsList.add(currentIndex, mapPart);
            } else if (mapPart == null) {
                currentPart = listPart;
                currentPart.setContentId(cid);
                partsMap.put(cid, currentPart);
            } else if (listPart != mapPart) {
                throw new MIMEParsingException("Created two different attachments using Content-ID and index");
            }
            currentPart.setHeaders(ih);
            break;

        case CONTENT:
            MIMEEventPartial.ContentPartial content = (MIMEEventPartial.ContentPartial) event;
            ByteBuffer buf = content.getData();

            boolean partIsPartial = content.getIsPartial();
            if (partIsPartial) {
                currentPart.setIsPartial(partIsPartial);
                isPartial = partIsPartial;
            }
            currentPart.addBody(buf);
            break;

        case END_PART:
            currentPart.doneParsing();
            ++currentIndex;
            break;

        case END_MESSAGE:
            parsed = true;
            try {
                in.close();
            } catch (IOException ioe) {
                throw new MIMEParsingException(ioe);
            }
            break;

        default:
            throw new MIMEParsingException("Unknown Parser state = " + event.getEventType());
        }
        return true;
    }
}
