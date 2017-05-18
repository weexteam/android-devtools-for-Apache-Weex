package com.taobao.weex.devtools.inspector.network;

import com.taobao.weex.devtools.inspector.network.utils.RequestConverter;
import com.taobao.weex.devtools.inspector.network.utils.ResponseConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by moxun on 17/5/18.
 */

public class GeneralEventReporter {

    private static GeneralEventReporter sInstance;
    private NetworkEventReporter mReporter;

    public static synchronized GeneralEventReporter getInstance() {
        if (sInstance == null) {
            sInstance = new GeneralEventReporter();
        }
        return sInstance;
    }

    private GeneralEventReporter() {
        mReporter = NetworkEventReporterManager.get();
    }

    public void requestWillBeSent(Map<String, Object> request) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.requestWillBeSent(RequestConverter.convertFrom(request));
        }
    }

    public void responseHeadersReceived(Map<String, Object> response) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.responseHeadersReceived(ResponseConverter.convertFrom(response));
        }
    }

    public void httpExchangeFailed(String requestId, String errorInfo) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.httpExchangeFailed(requestId, errorInfo);
        }
    }

    public InputStream interpretResponseStream(String requestId, String contentType, String contentEncoding, InputStream stream, boolean continueRead) {
        if (mReporter != null && mReporter.isEnabled()) {
            ResponseHandler defaultHandler = new DefaultResponseHandler(mReporter, requestId);
            mReporter.interpretResponseStream(requestId, contentType, contentEncoding, stream, defaultHandler);
            if (!continueRead) {
                try {
                    readAndClose(stream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return stream;
    }

    public void responseReadFailed(String requestId, String errorInfo) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.responseReadFailed(requestId, errorInfo);
        }
    }

    public void responseReadFinished(String requestId) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.responseReadFinished(requestId);
        }
    }

    public void dataSent(String requestId, int dataLength, int encodedDataLength) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.dataSent(requestId, dataLength, encodedDataLength);
        }
    }

    public void dataReceived(String requestId, int dataLength, int encodedDataLength) {
        if (mReporter != null && mReporter.isEnabled()) {
            mReporter.dataReceived(requestId, dataLength, encodedDataLength);
        }
    }

    private byte[] readAndClose(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        byte[] result = out.toByteArray();
        out.flush();
        out.close();
        in.close();
        return result;
    }
}
