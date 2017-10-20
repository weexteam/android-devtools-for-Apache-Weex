package com.taobao.weex.devtools.inspector.network.utils;

import android.support.annotation.Nullable;
import android.util.Pair;

import com.taobao.weex.devtools.inspector.network.NetworkEventReporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by moxun on 17/5/18.
 */

public class RequestConverter {
    public static NetworkEventReporter.InspectorRequest convertFrom(Map<String, Object> raw) {
        return new GeneralRequest(raw);
    }

    private static class GeneralRequest implements NetworkEventReporter.InspectorRequest {
        private Map<String, Object> data;
        List<Pair<String, String>> headers = new ArrayList<>(0);

        public GeneralRequest(Map<String, Object> data) {
            this.data = data;
            headers = ExtractUtil.getValue(data, "headers", headers);
        }

        @Nullable
        @Override
        public Integer friendlyNameExtra() {
            return ExtractUtil.getValue(data, "friendlyNameExtra", null);
        }

        @Override
        public String url() {
            return ExtractUtil.getValue(data, "url", "unknown");
        }

        @Override
        public String method() {
            return ExtractUtil.getValue(data, "method", "GET");
        }

        @Nullable
        @Override
        public byte[] body() throws IOException {
            Object body = data.get("body");

            if (body != null) {
                if (body.getClass().isArray() && body.getClass().getComponentType().getName().equals("byte")) {
                    return (byte[]) body;
                } else if (body instanceof String) {
                    return ((String) body).getBytes();
                }
            }

            return new byte[0];
        }

        @Override
        public String id() {
            return ExtractUtil.getValue(data, "requestId", "-1");
        }

        @Override
        public String friendlyName() {
            return ExtractUtil.getValue(data, "friendlyName", "None");
        }

        @Override
        public int headerCount() {
            return headers.size();
        }

        @Override
        public String headerName(int index) {
            return headers.get(index).first;
        }

        @Override
        public String headerValue(int index) {
            return headers.get(index).second;
        }

        @Nullable
        @Override
        public String firstHeaderValue(String name) {
            for (Pair<String, String> pair : headers) {
                if (pair.first != null && pair.first.equalsIgnoreCase(name)) {
                    return pair.second;
                }
            }
            return null;
        }
    }
}
