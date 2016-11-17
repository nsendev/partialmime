package org.jvnet.mimepull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * MIMETestUtils
 *
 * This is a utility file used to generate random MIMEPart and MIMEMessage for tests in this package.
 *
 * @author wayneng
 *
 */
public class MIMETestUtils {
    /**
     * Random instance
     */
    private Random rand = new java.util.Random();

    /**
     * Reference string for loading random chars.
     */
    private static final String STRREF = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Private String to use for random strings.
     */
    private String strSet = STRREF;

    /**
     * Size of strSet.
     */
    private int szString = 0;

    /**
     * Preloaded character array of strSet.
     */
    private char[] ac;

    /**
     * Default MIMEConfig for use to construct MIMEMessage.
     */
    private final MIMEConfigYM mimeConfig = new MIMEConfigYM();

    /**
     * String boundary for MIME attachments.
     */
    private String stringBoundary = "StringBoundary";

    /**
     * Default constructor.
     */
    public MIMETestUtils() {
        loadStringSet();
    }

    /** key attachment. */
    private final String mapKeyAttachment = "at";
    /** mapKeySize. */
    private final String mapKeySize = "sz";
    /** key header. */
    private final String mapKeyHeader = "hd";
    /** content id. */
    private final String mapKeyContentId = "ci";
    /** crc32. */
    private final String mapKeyCrc32 = "crc";

    /**
     * gets attachment key name.
     *
     * @return String.
     */
    public String getMapKeyAttachment() {
        return mapKeyAttachment;
    }

    /**
     * gets attachment size key name.
     *
     * @return String.
     */
    public String getMapKeySize() {
        return mapKeySize;
    }

    /**
     * gets attachment header key name.
     *
     * @return String.
     */
    public String getMapKeyHeader() {
        return mapKeyHeader;
    }

    /**
     * gets attachment content id key name.
     *
     * @return String.
     */
    public String getMapKeyContentId() {
        return mapKeyContentId;
    }

    /**
     * gets attachment crc32 key name.
     *
     * @return String.
     */
    public String getMapKeyCrc32() {
        return mapKeyCrc32;
    }

    /**
     * Sets the stringBoundary for MIMEMessage.
     *
     * @param stringBoundary String
     */
    public void setStringBoundary(final String stringBoundary) {
        this.stringBoundary = stringBoundary;
    }

    /**
     * Loads default String set for random choosing.
     */
    public void loadStringSet() {
        loadStringSet(null);
    }

    /**
     * Loads String s as string set to choose for string randomization.
     *
     * @param s String
     */
    public void loadStringSet(final String s) {
        if (s == null) {
            strSet = STRREF;
        } else {
            strSet = s;
        }
        szString = strSet.length();
        ac = strSet.toCharArray();
    }

    /**
     * Returns a random int between min and max, excluding max.
     *
     * @param min min number.
     * @param max max number.
     * @return random number.
     */
    public int randInt(final int min, final int max) {
        return randInt(min, max, false);
    }

    /**
     * Returns a random int between min and max, either including or excluding max.
     *
     * @param min min int.
     * @param max max int.
     * @param isInclusive whether or not to include max int.
     * @return random int.
     */
    public int randInt(final int min, final int max, final boolean isInclusive) {
        /**
         * returns rand int. If isInclusive, then range is: [min, max] If not inclusive, then range is: [min, max)
         */
        if (max < min) {
            return 0;
        }
        if (max == min && isInclusive) {
            return min;
        }
        int n = max - min;
        if (isInclusive) {
            n++;
        }
        return (rand.nextInt(n) + min);
    }

    /**
     * Returns a random char from the loaded StringSet.
     *
     * @return char
     */
    public char randChar() {
        int i = randInt(0, szString, false);
        return ac[i];
    }

    /**
     * Returns a random byte array.
     *
     * @param numBytes int.
     * @return byte array.
     */
    public byte[] randBytes(final int numBytes) {
        if (numBytes <= 0) {
            return null;
        }
        byte[] ba = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            int randInt = rand.nextInt();
            ba[i] = (byte) (randInt & 0xff);
        }
        return ba;
    }

    /**
     * returns a random string based on loaded String set, where return string size is numCharsPerWord.
     *
     * @param numCharsPerWord number of chars to return in String.
     * @return randomString
     */
    public String randString(final int numCharsPerWord) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < numCharsPerWord; i++) {
            c = randChar();
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns a line of random string with inputs numChars per word, num words per line, and delimiter for each word.
     *
     * @param numCharsPerWord Num chars per word.
     * @param numWordsPerLine Num words per line.
     * @param delimiter String to delimit each word.
     * @return line of random String.
     */
    public String randStringLine(final int numCharsPerWord, final int numWordsPerLine, final String delimiter) {
        String thisDelimiter = delimiter;
        if (thisDelimiter == null) {
            thisDelimiter = " ";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numWordsPerLine; i++) {
            String s = randString(numCharsPerWord);
            sb.append(s + thisDelimiter);
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Returns a set of lines with random strings. Refer to randStringLine for single line usage.
     *
     * @param numCharsPerWord Num chars per word.
     * @param numWordsPerLine Num words per line.
     * @param numLines Num lines in return set.
     * @param delimiter String to delimit each word.
     * @return Set of random lines of string.
     */
    public String randStringLines(final int numCharsPerWord, final int numWordsPerLine, final int numLines, final String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numLines; i++) {
            String s = randStringLine(numCharsPerWord, numWordsPerLine, delimiter);
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Get attachment header for MIMEMessage in this form: Content-Type:application/octet-stream Content-Disposition:form-data; filename=input;
     * name=input.
     *
     * @param filename filename.
     * @return String of attachment header for each attachment.
     */
    public String getAttachmentHeader(final String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append("Content-Type: application/octet-stream\r\n");
        sb.append(String.format("Content-Disposition: form-data; filename=\"%s\"; name=\"%s\"\r\n\r\n", filename, filename));
        return sb.toString();
    }

    /**
     * Gets a random text file attachment, which includes the attachment header and the random string file attachment.
     *
     * @param numCharsPerWord for file attachment, the number of char per word.
     * @param numWordsPerLine for file attachment, the number of words per line.
     * @param numLines for file attachment, the number of lines for this attachment.
     * @param delimiter for file attachment, the delimiter for each word.
     * @param filename the name of the attachment.
     * @return random text attachment.
     */
    public String textRandFileAttachment(final int numCharsPerWord, final int numWordsPerLine, final int numLines, final String delimiter,
            final String filename) {
        StringBuilder sb = new StringBuilder();
        String s;
        s = getAttachmentHeader(filename);
        sb.append(s);
        s = randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
        sb.append(s);
        return sb.toString();
    }

    /**
     * Gets random text file attachment.
     *
     * @param szFile size of file.
     * @param filename name of file.
     * @return String that has attachment header and random string attachment.
     */
    public String textRandFileAttachment(final int szFile, final String filename) {
        int szPerLine = 80;
        int numLines = szFile / szPerLine;
        int numCharsPerWord = 8;
        int numWordsPerLine = 9;
        return randStringLines(numCharsPerWord, numWordsPerLine, numLines, " ");
    }

    /**
     * Gets the string boundary delimiter for MIME Multipart.
     *
     * @param boundary the string used for the boundary creation.
     * @param isEndAttachment if the boundary is for the end of MIME Multipart.
     * @return the string boundary delimiter for MIME Multipart.
     */
    public String getBoundaryString(final String boundary, final boolean isEndAttachment) {
        if (isEndAttachment) {
            return "--" + boundary + "--\r\n";
        }
        return "--" + boundary + "\r\n";
    }

    /**
     * ContentType for the type of MIME attachment, used only for testing.
     *
     * @author wayneng
     *
     */
    public enum ContentType {
        /**
         * JSON type.
         */
        JSON,
        /**
         * FILE attachment type.
         */
        FILE
    }

    /**
     * Gets the attachment header for each attachment in MIMEPart.
     *
     * @param eType the type of attachment.
     * @return the string of attachment header.
     * @throws Exception from jvnet.
     */
    protected String getAttachmentHeader(final ContentType eType) throws Exception {
        return getAttachmentHeader(eType, null);
    }

    /**
     *
     * @param eType type of attachment.
     * @param filename filename used for header construction.
     * @return String attachment header, depending on content type.
     * @throws Exception from jvnet.
     */
    public String getAttachmentHeader(final ContentType eType, final String filename) throws Exception {
        StringBuilder sb = new StringBuilder();

        if (eType == ContentType.JSON) {
            sb.append("Content-Type: application/json\r\n");
            sb.append("Content-Disposition: form-data; name=\"jsonString\"\r\n\r\n");
        } else if (eType == ContentType.FILE) {
            if (filename == null) {
                throw new Exception("getAttachmentHeader method needs filename");
            }
            sb.append("Content-Type: application/octet-stream\r\n");
            sb.append(String.format("Content-Disposition: form-data; filename=\"%s\"; name=\"%s\"\r\n\r\n", filename, filename));
        } else {
            throw new Exception("getAttachmentHeader ContentType undefined");
        }
        return sb.toString();
    }

    /**
     * Creates JSONBean for use with MIMEMultiPart or MIMEMessage.
     *
     * @param listMsgReferences list of message references put into JSONBean.
     * @param listAttachmentFilenames list of multipart attachments put into JSONBean.
     * @return string of JSONBeant section of MIMEMessage.
     * @throws Exception from jvnet.
     */
    public String getJSONBeanString(final List<String> listMsgReferences, final List<String> listAttachmentFilenames) throws Exception {
        /*
         * listMsgReferences is a list of filenames. This populates it with random fields for messageReference.id and messageReference.pid.
         *
         * listAttachmentFilenames is a list of filenames. This populates it with multipartName
         */
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);

        {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectFieldStart("actions");
            jsonGenerator.writeObjectField("applyFilters", false);
            jsonGenerator.writeObjectField("applyNotifications", false);
            jsonGenerator.writeObjectField("responseMessage", true);
            jsonGenerator.writeEndObject();
        }
        {
            jsonGenerator.writeObjectFieldStart("simpleBody");
            jsonGenerator.writeObjectField("html", "<html>some body</html>");
            jsonGenerator.writeArrayFieldStart("attachments");
            int idx = 1;
            for (String msg : listMsgReferences) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField("disposition", "attachment");
                jsonGenerator.writeObjectField("type", "application");
                jsonGenerator.writeObjectField("filename", msg);
                jsonGenerator.writeObjectField("contentId", String.format("<contentId_%s>", msg));
                jsonGenerator.writeObjectFieldStart("messageReference");
                jsonGenerator.writeObjectField("id", String.format("id_%s", msg));
                jsonGenerator.writeObjectField("pid", idx++);
                jsonGenerator.writeEndObject();
                jsonGenerator.writeObjectField("included", "Y");
                jsonGenerator.writeEndObject();
            }
            for (String msg : listAttachmentFilenames) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField("disposition", "attachment");
                jsonGenerator.writeObjectField("type", "application");
                jsonGenerator.writeObjectField("filename", msg);
                jsonGenerator.writeObjectField("contentId", String.format("<contentId_%s>", msg));
                jsonGenerator.writeObjectField("multipartName", String.format("multipart://%s", msg));
                jsonGenerator.writeObjectField("included", "Y");
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        {
            jsonGenerator.writeObjectFieldStart("message");
            jsonGenerator.writeObjectField("newMessage", true);
            jsonGenerator.writeObjectField("csid", "1234");

            jsonGenerator.writeObjectFieldStart("folder");
            jsonGenerator.writeObjectField("id", "3");
            jsonGenerator.writeEndObject();

            jsonGenerator.writeObjectFieldStart("headers");
            jsonGenerator.writeObjectField("subject", "some subject");

            jsonGenerator.writeArrayFieldStart("from");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("name", "first last");
            jsonGenerator.writeObjectField("email", "email@yahoo.com");
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndArray();

            jsonGenerator.writeArrayFieldStart("to");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("name", "first last");
            jsonGenerator.writeObjectField("email", "email@yahoo.com");
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();

            jsonGenerator.writeObjectField("id", "id1234");
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.close();
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
        String s = new String(baos.toByteArray(), "UTF-8");
        sb.append(s);
        s = sb.toString();
        return s;
    }

    /**
     * Creates a MIMEPart, which is a proxy method where MIMEPart can only be created from same package.
     *
     * @param mimeMessage full MIMEMessage.
     * @return MIMEPart wrapper.
     * @throws Exception from jvnet, if any thrown.
     */
    public MIMEPartPartial createMIMEPart(final MIMEMessagePartial mimeMessage) throws Exception {
        final MIMEPartPartial mimePart = new MIMEPartPartial(mimeMessage);
        return mimePart;
    }

    /**
     * Creates a MIMEMessage. It creates a MultiPart MIMEMessage with generic JSON Bean, and numAttachment number of random text message attachments.
     *
     * @param numAttachments numAttachments.
     * @return MIMEMessage.
     * @throws IOException from jvnet or IO op.
     * @throws Exception from jvnet.
     */
    public MIMEMessagePartial createMIMEMessage(final int numAttachments) throws IOException, Exception {
        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 10;
        String delimiter = " ";
        String s;

        String strBoundary = "Boundary_123_wayneng";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> listMsgReferences = new ArrayList<>();
        List<String> listAttachmentFilenames = new ArrayList<>();
        int idxFile = 0;
        String filename;
        for (int i = 0; i < numAttachments; i++) {
            filename = String.format("file_%d", idxFile++);
            listAttachmentFilenames.add(filename);
        }
        for (int i = 0; i < numAttachments; i++) {
            filename = String.format("file_%d", idxFile++);
            listMsgReferences.add(String.format("file_%d", idxFile++));
        }
        s = getBoundaryString(strBoundary, false);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/json\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"jsonString\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        s = getJSONBeanString(listMsgReferences, listAttachmentFilenames);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        for (int i = 0; i < numAttachments; i++) {
            filename = String.format("file_%d", i);
            s = getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = getAttachmentHeader(filename);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
        }
        s = getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        MIMEMessagePartial mimeMessage = new MIMEMessagePartial(is, stringBoundary, mimeConfig);
        return mimeMessage;
    }

    /**
     * Generates a LinkedHashMap<String, HashMap<String, String>> of attachments. The structure of LinkedHashMap is:
     *
     * key = String filename
     *
     * HashMap<String, String>
     *
     * content: file contents
     *
     * path: string full path
     *
     * size: string converted integer size of attachment content.
     *
     * @param linkedHashMap The hashmap to populate.
     * @param numAttachments The input number of attachments to generate.
     * @param szPerAttachment The approx size of each attachment.
     * @param attachmentNamePrefix String.
     */
    public void generateAttachments(final LinkedHashMap<String, HashMap<String, String>> linkedHashMap, final int numAttachments,
            final int szPerAttachment, final String attachmentNamePrefix) {
        if (linkedHashMap == null) {
            return;
        }
        String prefix = attachmentNamePrefix;
        if (attachmentNamePrefix == null) {
            prefix = "attachment";
        }
        for (int i = 0; i < numAttachments; i++) {
            String filename = String.format("%s_%02d.txt", prefix, i);
            String attachment = textRandFileAttachment(szPerAttachment, filename);
            String header = getAttachmentHeader(filename);
            String contentId = String.format("<ContentId%s>", filename);
            byte[] ba = attachment.getBytes(StandardCharsets.UTF_8);
            CRC32 crc32 = new CRC32();
            crc32.update(ba);
            linkedHashMap.put(filename, new HashMap<String, String>());
            linkedHashMap.get(filename).put(mapKeyAttachment, attachment);
            linkedHashMap.get(filename).put(mapKeySize, Integer.toString(attachment.length()));
            linkedHashMap.get(filename).put(mapKeyHeader, header);
            linkedHashMap.get(filename).put(mapKeyContentId, contentId);
            linkedHashMap.get(filename).put(mapKeyCrc32, Long.toString(crc32.getValue()));
        }
    }

}
