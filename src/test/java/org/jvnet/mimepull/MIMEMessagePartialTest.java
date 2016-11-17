package org.jvnet.mimepull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * MIMEMessageTest tests the jvnet partial package.
 *
 * @author wayneng
 *
 */
public class MIMEMessagePartialTest {
    /**
     * Shell to call other methods.
     *
     * @throws Exception from jvnet.
     */
    @Test
    public void testShell() throws Exception {
        // testInstance();
        // testInstanceBadMIMEShell();
        // testInstanceBadBoundary();
        // testJSONBody();
        // testJSONObjectMapperSimpleJSON();
        testMIMETwoFilesPartialContentDisposition();
    }

    /**
     * Test to see how JsonGenerator works.
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testJSONObjectMapperSimpleJSON() throws Exception {
        /*
         * Do string representation JSON of:
         *
         * {
         *
         * a : { aa0:aa00, aa1:aa01 },
         *
         * b : [
         *
         * { bb0:bb00, bb1:bb01 },
         *
         * { bc0:bc01, bc1:bc01 }
         *
         * ],
         *
         * cc0:[]
         *
         * }
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("a");
        jsonGenerator.writeObjectField("aa0", false);
        jsonGenerator.writeObjectField("aa1", "aa01");
        jsonGenerator.writeEndObject();
        jsonGenerator.writeArrayFieldStart("b");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("bb0", "bb00");
        jsonGenerator.writeStringField("bb1", "bb01");
        jsonGenerator.writeEndObject();
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("bc0", "bc00");
        jsonGenerator.writeStringField("bc1", "bc01");
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndArray();
        jsonGenerator.writeArrayFieldStart("c");
        jsonGenerator.writeEndArray();
        jsonGenerator.close();
        String stringJson = new String(baos.toByteArray(), "UTF-8");
        String stringRef = "{\"a\":{\"aa0\":false,\"aa1\":\"aa01\"},"
                + "\"b\":[{\"bb0\":\"bb00\",\"bb1\":\"bb01\"},{\"bc0\":\"bc00\",\"bc1\":\"bc01\"}]," + "\"c\":[]}";
        Assert.assertNotNull(stringJson);
        boolean res = stringRef.equals(stringJson);
        Assert.assertTrue(res, "stringJson mismatch: " + stringJson);
    }

    /**
     * Another test to see how JsonGenerator works.
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testJSONObjectMapperJSONSimpleTest2() throws Exception {
        /*
         * Do string representation JSON of:
         *
         * {
         *
         * a : { aa0:aa00, aa1:aa01 },
         *
         * b : [
         *
         * { bb0:bb00, bb1:bb01 },
         *
         * { bc0:bc01, bc1:bc01 }
         *
         * ],
         *
         * cc0:[]
         *
         * }
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
        {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectFieldStart("a");
            jsonGenerator.writeObjectField("aa0", false);
            jsonGenerator.writeObjectField("aa1", "aa01");
            jsonGenerator.writeEndObject();
        }
        {
            // do not use writeArrayFieldStart and writeStartObject for the same thing, use one
            jsonGenerator.writeArrayFieldStart("b");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("bb0", "bb00");
            jsonGenerator.writeStringField("bb1", "bb01");
            jsonGenerator.writeEndObject();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("bc0", "bc00");
            jsonGenerator.writeStringField("bc1", "bc01");
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndArray();
        }
        {
            jsonGenerator.writeArrayFieldStart("c");
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.close();
        String stringJson = new String(baos.toByteArray(), "UTF-8");
        String stringRef = "{\"a\":{\"aa0\":false,\"aa1\":\"aa01\"},"
                + "\"b\":[{\"bb0\":\"bb00\",\"bb1\":\"bb01\"},{\"bc0\":\"bc00\",\"bc1\":\"bc01\"}]," + "\"c\":[]}";
        Assert.assertNotNull(stringJson);
        boolean res = stringRef.equals(stringJson);
        Assert.assertTrue(res, "stringJson mismatch: " + stringJson);
    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMEGoodFiles() throws Exception {
        // testMIMEGoodFiles(1);
        testMIMEGoodFiles(2);
        // testMIMEGoodFiles(10);
    }

    /**
     *
     * @param numFiles number of files to create.
     * @throws Exception from partial package.
     */
    private void testMIMEGoodFiles(final int numFiles) throws Exception {
        /*
         * test MIMEParser with all good attachments
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 10;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;

        String strBoundary = "Boundary_123_test";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> listMsgReferences = new ArrayList<>();
        List<String> listAttachmentFilenames = new ArrayList<>();
        // write to array and use for reference later
        int idxFile = 0;
        String filename;
        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", idxFile++);
            listAttachmentFilenames.add(filename);
        }
        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", idxFile++);
            listMsgReferences.add(String.format("file_%d", idxFile++));
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, false);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/json\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"jsonString\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        s = mimeTestUtils.getJSONBeanString(listMsgReferences, listAttachmentFilenames);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", i);
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(filename);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        try {
            final String strReference = baos.toString();
            Assert.assertNotNull(strReference);
            is = new ByteArrayInputStream(baos.toByteArray());
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            int idx = 0;
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = !mimePart.getIsPartial();
                Assert.assertTrue(res, "mimePart isPartial should be false");
                List<String> lContentDisposition = mimePart.getHeader("Content-Disposition");
                Assert.assertNotNull(lContentDisposition, "contentDisposition null");
                res = lContentDisposition.size() == 1;
                Assert.assertTrue(res, "contentDisposition size != 1");
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }
                baosCmp.write("\n".getBytes(StandardCharsets.UTF_8));
                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
                String mimeReference = listBody.get(idx);
                res = mimeReference.equals(mimeStreamVal);
                if (contentType.equals("application/json")) {
                    continue;
                }
                if (!res) {
                    int szRef = mimeReference.length();
                    int szMsg = mimeStreamVal.length();
                    res = szRef == szMsg;
                    Assert.assertTrue(res, "content length mismatch");
                }
                Assert.assertTrue(res, "string compare mismatch for " + listHeader.get(idx));
                idx++;
            }
            res = l.size() == (listHeader.size() + 1); // +1 for jsonString
            Assert.assertTrue(res, "attachment list size mismatch " + l.size());
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMETwoFilesPartialJSONBean() throws Exception {
        /*
         * Test MIMEParser that breaks at JSON Bean, the first part of attachments. This should fail and throw exception. We cannot process if the
         * JSON section not available.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        String s;
        String strBoundary = "Boundary_123_test";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> listMsgReferences = new ArrayList<>();
        List<String> listAttachmentFilenames = new ArrayList<>();
        // write to array and use for reference later
        int idxFile = 0;
        int numFiles = 2;
        String filename;
        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", idxFile++);
            listAttachmentFilenames.add(filename);
        }
        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", idxFile++);
            listMsgReferences.add(String.format("file_%d", idxFile++));
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, false);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        s = mimeTestUtils.getAttachmentHeader(MIMETestUtils.ContentType.JSON);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        int szJsonHeader = s.length();
        String stringJson = mimeTestUtils.getJSONBeanString(listMsgReferences, listAttachmentFilenames);
        baos.write(stringJson.getBytes(StandardCharsets.UTF_8));
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;

        // calculate the boundary of the Content-Disposition header of second attachment
        int szBoundary = strBoundary.length() + "--\r\n".length();
        int szFirstHeader = szBoundary + szJsonHeader;
        int szJsonBody = stringJson.length();
        int breakPoint = szFirstHeader + szJsonBody / 2;
        int cntPartial = 0;
        try {
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                List<String> lContentDisposition = mimePart.getHeader("Content-Disposition");
                Assert.assertNotNull(lContentDisposition, "contentDisposition null");
                res = lContentDisposition.size() == 1;
                Assert.assertTrue(res, "contentDisposition size != 1");
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }
                baosCmp.write("\n".getBytes(StandardCharsets.UTF_8));
                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 1; // for jsonString
            Assert.assertTrue(res, "attachment list size mismatch " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMETwoFilesPartialJSONBeanFilesFirst() throws Exception {
        /*
         * Test MIMEParser that breaks at JSON Bean, the last part of attachments. This should fail and throw exception. We cannot process if the JSON
         * section not available.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numFiles = 2;
        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 10;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;

        String strBoundary = "Boundary_123_test";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> listMsgReferences = new ArrayList<>();
        List<String> listAttachmentFilenames = new ArrayList<>();
        // write to array and use for reference later
        String filename;
        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", i);
            listAttachmentFilenames.add(filename);
        }

        for (int i = 0; i < numFiles; i++) {
            filename = String.format("file_%d", i);
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(filename);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }

        s = mimeTestUtils.getBoundaryString(strBoundary, false);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        s = mimeTestUtils.getAttachmentHeader(MIMETestUtils.ContentType.JSON);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        int szJsonHeader = s.length();

        s = mimeTestUtils.getJSONBeanString(listMsgReferences, listAttachmentFilenames);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        int szJsonBody = s.length();

        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;

        int szBoundary = strBoundary.length() + "--\r\n".length();
        int szAttachments = szBoundary * 3;
        for (int i = 0; i < numFiles; i++) {
            szAttachments += listHeader.get(i).length();
            szAttachments += listBody.get(i).length();
        }
        int breakPoint = szAttachments + szJsonHeader + szJsonBody / 2;
        int cntPartial = 0;
        try {
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            // MIMEPart mimePartJSON = mimeMsg.getPart("0");
            int idx = 0;
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                List<String> lContentDisposition = mimePart.getHeader("Content-Disposition");
                Assert.assertNotNull(lContentDisposition, "contentDisposition null");
                res = lContentDisposition.size() == 1;
                Assert.assertTrue(res, "contentDisposition size != 1");
                // String contentDisposition = lContentDisposition.get(0);
                // parse contentDisposition for filename
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }
                baosCmp.write("\n".getBytes(StandardCharsets.UTF_8));
                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
                if (contentType.equals("application/json")) {
                    res = mimePart.getIsPartial();
                    Assert.assertTrue(res, "application/json should be partial");
                    continue;
                }
                String mimeReference = listBody.get(idx);
                res = mimeReference.equals(mimeStreamVal);
                if (!res) {
                    int szRef = mimeReference.length();
                    int szMsg = mimeStreamVal.length();
                    res = szRef == szMsg;
                    Assert.assertTrue(res, "content length mismatch");
                }
                Assert.assertTrue(res, "string compare mismatch for " + listHeader.get(idx));
                idx++;
            }
            res = l.size() == (listHeader.size() + 1); // +1 for jsonString
            Assert.assertTrue(res, "attachment list size mismatch " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMETwoFilesPartialContentDisposition() throws Exception {
        /*
         * test MIMEParser with 1 good attachment and 1 partial attachment broken at Content-Disposition header. MIMEMessage.getAttachments() should
         * return 1 full attachment.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 2;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstAttach = szBoundary + listHeader.get(0).length() + listBody.get(0).length() + szBoundary;
        int szSecondHeader = listHeader.get(1).length();
        int breakPoint = szFirstAttach + szSecondHeader / 2;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        int cntPartial = 0;

        try {
            // use only partial byte array
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 2;
            Assert.assertTrue(res, "attachment list size should be 2 but is " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));

            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     * testMIMEPartialAtBoundaryString tests parsing a MIME multipart that cuts off at the boundary string.
     *
     * The result is that the partial last attachment should have length greater than the actual attachment, and the message itself should be partial.
     *
     * @throws Exception exception.
     */
    @Test
    public void testMIMEPartialAtEndBoundaryString() throws Exception {
        testMIMEPartialAtEndBoundaryString(2);
    }

    /**
     *
     * @param numAttachments int.
     * @throws Exception Exception.
     */
    private void testMIMEPartialAtEndBoundaryString(final int numAttachments) throws Exception {
        MIMETestUtils mimeTestUtils = new MIMETestUtils();
        LinkedHashMap<String, HashMap<String, String>> linkedHashMap = new LinkedHashMap<>();
        final int szPerAttachment = 1000;
        final String attachmentNamePrefix = "attachment";
        final String boundary = "boundaryString123";
        MIMEConfigYM mimeConfig = new MIMEConfigYM();
        MIMEMessagePartial mimeMsg = null;
        InputStream is = null;
        mimeTestUtils.generateAttachments(linkedHashMap, numAttachments, szPerAttachment, attachmentNamePrefix);

        /*
         * Create the MIME message. Then calculate the last string boundary length. Break off somewhere at last string boundary.
         */
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, HashMap<String, String>> e : linkedHashMap.entrySet()) {
                HashMap<String, String> v = e.getValue();
                sb.append(mimeTestUtils.getBoundaryString(boundary, false));
                sb.append(v.get(mimeTestUtils.getMapKeyHeader()));
                sb.append(v.get(mimeTestUtils.getMapKeyAttachment()));
            }
            String s = mimeTestUtils.getBoundaryString(boundary, true);
            sb.append(s);
            byte[] ba = sb.toString().getBytes(StandardCharsets.UTF_8);
            int szMsg = ba.length;
            int szBoundaryEnd = s.getBytes(StandardCharsets.UTF_8).length;
            int breakpoint = szMsg - (szBoundaryEnd / 2);
            is = new ByteArrayInputStream(ba, 0, breakpoint);
            mimeMsg = new MIMEMessagePartial(is, boundary, mimeConfig);
            boolean res = false;
            int cntPartial = 0;
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == numAttachments;
            Assert.assertTrue(res, "attachment list size should not be " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));

        } finally {
            is.close();
            mimeMsg.close();
        }
    }

    /**
     *
     * @throws Exception exception.
     */
    @Test
    public void testMIMEPartialAtBeginBoundaryString() throws Exception {
        testMIMEPartialAtBeginBoundaryString(3);
    }

    /**
     *
     * @param numAttachments int.
     * @throws Exception exception.
     */
    private void testMIMEPartialAtBeginBoundaryString(final int numAttachments) throws Exception {
        MIMETestUtils mimeTestUtils = new MIMETestUtils();
        LinkedHashMap<String, HashMap<String, String>> linkedHashMap = new LinkedHashMap<>();
        final int szPerAttachment = 1000;
        final String attachmentNamePrefix = "attachment";
        final String boundary = "boundaryString123";
        MIMEConfigYM mimeConfig = new MIMEConfigYM();
        MIMEMessagePartial mimeMsg = null;
        InputStream is = null;
        mimeTestUtils.generateAttachments(linkedHashMap, numAttachments, szPerAttachment, attachmentNamePrefix);

        /*
         * Create the MIME message. Then calculate the last string boundary length. Break off somewhere at last string boundary.
         */
        try {
            StringBuilder sb = new StringBuilder();
            String lastFilename = null;
            for (Map.Entry<String, HashMap<String, String>> e : linkedHashMap.entrySet()) {
                lastFilename = e.getKey();
                HashMap<String, String> v = e.getValue();
                sb.append(mimeTestUtils.getBoundaryString(boundary, false));
                sb.append(v.get(mimeTestUtils.getMapKeyHeader()));
                sb.append(v.get(mimeTestUtils.getMapKeyAttachment()));
                sb.append("\r\n");
            }
            String s = mimeTestUtils.getBoundaryString(boundary, true);
            sb.append(s);
            int szBoundaryEnd = s.getBytes(StandardCharsets.UTF_8).length;
            s = mimeTestUtils.getBoundaryString(boundary, false);
            int szBoundaryStart = s.getBytes(StandardCharsets.UTF_8).length;

            final String referenceString = sb.toString();
            byte[] ba = referenceString.getBytes(StandardCharsets.UTF_8);
            int szMsg = ba.length;

            /*
             * Calculate the size of the last attachment (boundary and all).
             */
            HashMap<String, String> hashmap = linkedHashMap.get(lastFilename);

            s = hashmap.get(mimeTestUtils.getMapKeyHeader());
            int szHeader = s.getBytes(StandardCharsets.UTF_8).length;

            s = hashmap.get(mimeTestUtils.getMapKeyAttachment());
            int szAttach = s.getBytes(StandardCharsets.UTF_8).length;

            int szLastAttachment = szBoundaryStart + szHeader + szAttach + szBoundaryEnd;
            int breakpoint = szMsg - szLastAttachment + (szBoundaryStart / 2);

            is = new ByteArrayInputStream(ba, 0, breakpoint);
            mimeMsg = new MIMEMessagePartial(is, boundary, mimeConfig);
            boolean res = false;
            int cntPartial = 0;
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == (numAttachments - 1);
            Assert.assertTrue(res, "attachment list size should not be " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));

        } finally {
            is.close();
            mimeMsg.close();
        }
    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMETwoFilesPartialContentType() throws Exception {
        /*
         * test MIMEParser with 1 good attachment and 1 partial attachment broken at Content-Disposition header. MIMEMessage.getAttachments() should
         * return 1 full attachment.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 2;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstAttach = szBoundary + listHeader.get(0).length() + listBody.get(0).length() + szBoundary;
        int breakPoint = szFirstAttach + 5;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        int cntPartial = 0;

        try {
            // use only partial byte array
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 2;
            Assert.assertTrue(res, "attachment list size should be 2 but is " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));

            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMETwoFilesPartialBody() throws Exception {
        /*
         * test MIMEParser with 1 good attachment and 1 partial attachment broken at content body. MIMEMessage.getAttachments() should return 1 full
         * and 1 partial attachment.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 2;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstAttach = szBoundary + listHeader.get(0).length() + listBody.get(0).length() + szBoundary;
        int szSecondAttach = listHeader.get(1).length() + listBody.get(1).length() + szBoundary;
        int breakPoint = szFirstAttach + szSecondAttach / 2;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        int cntPartial = 0;

        boolean res;
        try {
            // use only partial byte array
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }

                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 2;
            Assert.assertTrue(res, "attachment list size should be 2 but is " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMEPartialContentDispositionNoReturn() throws Exception {
        /*
         * test MIMEParser with 0 good attachment where the attachment is broken at Content-Disposition header.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 1;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstHeader = szBoundary + listHeader.get(0).length();
        int breakPoint = szFirstHeader / 2;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        try {
            // use only partial byte array
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (MIMEPart mimePart : l) {
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 1;
            Assert.assertTrue(res, "attachment list size should be 1 but is " + l.size());
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMEOneFilePartialBody() throws Exception {
        /*
         * test MIMEParser with 1 good partial attachment broken at content body. MIMEMessage.getAttachments() should return 1 partial attachment.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 1;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstAttach = szBoundary + listHeader.get(0).length() + listBody.get(0).length() + szBoundary;
        int breakPoint = szFirstAttach / 2;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        int cntPartial = 0;
        try {
            // use only partial byte array
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 1;
            Assert.assertTrue(res, "attachment list size should be 0 but is " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }

    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testMIMEOneFilePartialBodyAtBoundary() throws Exception {
        /*
         * test MIMEParser with 1 good partial attachment broken at end of boundary. MIMEMessage.getAttachments() should return 1 partial attachment.
         */
        MIMETestUtils mimeTestUtils = new MIMETestUtils();

        int numCharsPerWord = 9;
        int numWordsPerLine = 8;
        int numLines = 20;
        String delimiter = " ";

        List<String> listHeader = new ArrayList<>();
        List<String> listBody = new ArrayList<>();

        String s;
        int numFiles = 1;

        String strBoundary = "Boundary_123_test";
        int szBoundary = strBoundary.length() + "--\r\n".length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // write to array and use for reference later
        for (int i = 0; i < numFiles; i++) {
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(String.format("file_%d", i));
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listHeader.add(s);
            s = mimeTestUtils.randStringLines(numCharsPerWord, numWordsPerLine, numLines, delimiter);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            listBody.add(s);
        }
        s = mimeTestUtils.getBoundaryString(strBoundary, true);
        baos.write(s.getBytes(StandardCharsets.UTF_8));
        String stringReference = new String(baos.toByteArray(), "UTF-8");
        Assert.assertNotNull(stringReference, "stringReference is null");

        // calculate the boundary of the Content-Disposition header of second attachment
        int szFirstAttach = szBoundary + listHeader.get(0).length() + listBody.get(0).length();
        int breakPoint = szFirstAttach + szBoundary / 2;

        MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        int cntPartial = 0;
        boolean res;
        try {
            // use only partial byte array
            {
                InputStream isParser = new ByteArrayInputStream(baos.toByteArray());
                MIMEParserPartial mimeParser = new MIMEParserPartial(isParser, strBoundary, new MIMEConfigYM());
                Assert.assertNotNull(mimeParser);
            }
            is = new ByteArrayInputStream(baos.toByteArray(), 0, breakPoint);
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                res = mimePart.getIsPartial();
                if (res) {
                    cntPartial++;
                }
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }

                String mimeStreamVal = new String(baosCmp.toByteArray(), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
            }
            res = l.size() == 1;
            Assert.assertTrue(res, "attachment list size should be 0 but is " + l.size());
            res = cntPartial == 1;
            Assert.assertTrue(res, String.format("cntPartial should be 1, is %d", cntPartial));
            mimeMsg.close();
            is.close();
        } catch (IOException e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }
    }

    /**
     * testSimpleBase64FormatOddBoundary uses MIMEMessagePartial and Base64 encoding, truncating MIME Base64 content at o partial binary byte. The
     * mime parser should truncate at mod 4 boundary.
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testSimpleBase64FormatOddStringBoundary() throws Exception {
        final MIMETestUtils mimeTestUtils = new MIMETestUtils();
        final String strSet = "abcdefABCDEF0123456789";
        mimeTestUtils.loadStringSet(strSet);
        final int numChars = 20000;
        final int fileBreakpoint = 9333;
        final String sRef = mimeTestUtils.randString(numChars);
        final String subStringRef = sRef.substring(0, fileBreakpoint);
        final String strBoundary = "boundary123_test";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String filename = "file1";
        String s;
        // create the reference
        {
            // JSON portion
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(MIMETestUtils.ContentType.JSON);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            final List<String> listMsgReferences = new java.util.ArrayList<>();
            final List<String> listAttachmentFilenames = new java.util.ArrayList<>();
            listAttachmentFilenames.add(filename);
            listMsgReferences.add(filename);
            String stringJson = mimeTestUtils.getJSONBeanString(listMsgReferences, listAttachmentFilenames);
            baos.write(stringJson.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            s = "\n" + s;
            baos.write(s.getBytes(StandardCharsets.UTF_8));

            // ATTACHMENT portion
            s = "Content-Type: application/octet-stream\n";
            s += "Content-Transfer-Encoding: Base64\n";
            s += "Content-Disposition: form-data; filename=\"" + filename + "\"; name=\"" + filename + "\"\n\n";
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            baos.write(subStringRef.getBytes(StandardCharsets.UTF_8));
        }

        final MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        try {
            final String sMIMEMsg = baos.toString();
            Assert.assertNotNull(sMIMEMsg);
            is = new ByteArrayInputStream(baos.toByteArray());
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            int cntPartial = 0;
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (!res) {
                    continue;
                }
                cntPartial++;
                List<String> lContentDisposition = mimePart.getHeader("Content-Disposition");
                Assert.assertNotNull(lContentDisposition, "contentDisposition null");
                res = lContentDisposition.size() == 1;
                Assert.assertTrue(res, "contentDisposition size != 1");
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }
                byte[] baOut = Base64.getEncoder().encode(baosCmp.toByteArray());
                String mimeStreamVal = new String(baOut, "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
                final int szRead = mimeStreamVal.length();
                Assert.assertNotEquals(szRead, 0);
                final int substringSz = subStringRef.length();
                final int substringMod4 = substringSz % 4;
                final int substringTruncateSz = substringSz - substringMod4;
                final String substringTruncate = subStringRef.substring(0, substringTruncateSz);
                final int cmpInt = mimeStreamVal.compareTo(substringTruncate);
                Assert.assertEquals(cmpInt, 0);
            }
            Assert.assertEquals(cntPartial, 1);
        } catch (Exception e) {
            throw e;
        } finally {
            mimeMsg.close();
            is.close();
        }
    }

    /**
     * testSimpleBase64FormatPartial just converts binary bytes to Base64, and truncates MIME at fileBreakpoint in bytes. be mod 4, so no truncation
     * is needed. Then the comparison just reads the partial MIME attachment content in binary reference values.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSimpleBase64FormatPartial() throws Exception {
        int numBytes = 20000;
        int fileBreakpoint = 9333;
        testSimpleBase64FormatPartial(numBytes, fileBreakpoint);
        numBytes = 2000;
        fileBreakpoint = 333;
        testSimpleBase64FormatPartial(numBytes, fileBreakpoint);
    }

    /**
     * testSimpleBase64FormatPartial just converts binary bytes to Base64, and truncates MIME at fileBreakpoint in bytes. be mod 4, so no truncation
     * is needed. Then the comparison just reads the partial MIME attachment content in binary reference values.
     *
     * @param numBytes int.
     * @param fileBreakpoint int.
     * @throws Exception exception.
     */
    private void testSimpleBase64FormatPartial(final int numBytes, final int fileBreakpoint) throws Exception {
        final MIMETestUtils mimeTestUtils = new MIMETestUtils();
        final String strSet = "abcdefABCDEF0123456789";
        mimeTestUtils.loadStringSet(strSet);
        final byte[] baRef = mimeTestUtils.randBytes(numBytes);
        final byte[] baSubArrayRef = java.util.Arrays.copyOfRange(baRef, 0, fileBreakpoint);
        final String strSubArrayRef = new String(Base64.getEncoder().encode(baSubArrayRef), "UTF-8");
        final String strBoundary = "boundary123_test";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String filename = "file1";
        String s;

        // create the reference
        {
            // JSON portion
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getAttachmentHeader(MIMETestUtils.ContentType.JSON);
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            final List<String> listMsgReferences = new java.util.ArrayList<>();
            final List<String> listAttachmentFilenames = new java.util.ArrayList<>();
            listAttachmentFilenames.add(filename);
            listMsgReferences.add(filename);
            String stringJson = mimeTestUtils.getJSONBeanString(listMsgReferences, listAttachmentFilenames);
            baos.write(stringJson.getBytes(StandardCharsets.UTF_8));
            s = mimeTestUtils.getBoundaryString(strBoundary, false);
            s = "\n" + s;
            baos.write(s.getBytes(StandardCharsets.UTF_8));

            // ATTACHMENT portion
            s = "Content-Type: application/octet-stream\n";
            s += "Content-Transfer-Encoding: Base64\n";
            s += "Content-Disposition: form-data; filename=\"" + filename + "\"; name=\"" + filename + "\"\n\n";
            baos.write(s.getBytes(StandardCharsets.UTF_8));
            baos.write(strSubArrayRef.getBytes(StandardCharsets.UTF_8));
        }
        final MIMEConfigYM mimeCfg = new MIMEConfigYM();
        InputStream is = null;
        MIMEMessagePartial mimeMsg = null;
        boolean res;
        try {
            final String sMIMEMsg = baos.toString();
            Assert.assertNotNull(sMIMEMsg);
            is = new ByteArrayInputStream(baos.toByteArray());
            mimeMsg = new MIMEMessagePartial(is, strBoundary, mimeCfg);
            List<MIMEPartPartial> l = mimeMsg.getAttachmentsPartial();
            Assert.assertNotNull(l, "List MIMEPart is null");
            int cntPartial = 0;

            // for each MIMEPart, should only be JSON and Attachments
            for (int i = 0; i < l.size(); i++) {
                MIMEPartPartial mimePart = l.get(i);
                String contentId = mimePart.getContentId();
                String contentType = mimePart.getContentType();
                res = mimePart.getIsPartial();
                if (!res) {
                    continue;
                }
                cntPartial++;
                List<String> lContentDisposition = mimePart.getHeader("Content-Disposition");
                Assert.assertNotNull(lContentDisposition, "contentDisposition null");
                res = lContentDisposition.size() == 1;
                Assert.assertTrue(res, "contentDisposition size != 1");
                Assert.assertNotNull(contentId);
                Assert.assertNotNull(contentType);
                InputStream isPart = mimePart.read();
                ByteArrayOutputStream baosCmp = new ByteArrayOutputStream();
                int v;
                while ((v = isPart.read()) != -1) {
                    baosCmp.write(v);
                }
                // compare byte array first
                final byte[] baCmp = baosCmp.toByteArray();
                Assert.assertNotNull(baCmp);
                final int szBaCmp = baCmp.length;
                Assert.assertNotEquals(szBaCmp, 0);
                res = java.util.Arrays.equals(baSubArrayRef, baCmp);
                Assert.assertEquals(res, true);

                // compare Base64 String value next
                String mimeStreamVal = new String(Base64.getEncoder().encode(baCmp), "UTF-8");
                Assert.assertNotNull(mimeStreamVal);
                final int szBase64Str = mimeStreamVal.length();
                Assert.assertNotEquals(szBase64Str, 0);
                final int cmpInt = strSubArrayRef.compareTo(mimeStreamVal);
                Assert.assertEquals(cmpInt, 0);
            }
            Assert.assertEquals(cntPartial, 1);
        } finally {
            mimeMsg.close();
            is.close();
        }
    }

    /**
     *
     * @throws Exception from partial package.
     */
    @Test
    public void testSimpleCreateFormDataMultiPart() throws Exception {
        // FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
    }
}
