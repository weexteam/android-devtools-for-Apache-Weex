package com.taobao.weex.devtools.inspector.network.utils;

import android.support.annotation.Nullable;
import android.util.Pair;

import com.taobao.weex.devtools.inspector.network.NetworkEventReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by moxun on 17/5/18.
 */

public class ResponseConverter {
    public static NetworkEventReporter.InspectorResponse convertFrom(Map<String, Object> raw) {
        return new GeneralResponse(raw);
    }

    private static class GeneralResponse implements NetworkEventReporter.InspectorResponse {
        private Map<String, Object> data;
        List<Pair<String, String>> headers = new ArrayList<>(0);

        public GeneralResponse(Map<String, Object> data) {
            this.data = data;
            headers = ExtractUtil.getValue(data, "headers", headers);
        }

        @Override
        public String url() {
            return ExtractUtil.getValue(data, "url", "unknown");
        }

        @Override
        public boolean connectionReused() {
            return ExtractUtil.getValue(data, "connectionReused", false);
        }

        @Override
        public int connectionId() {
            return ExtractUtil.getValue(data, "connectionId", 0);
        }

        @Override
        public boolean fromDiskCache() {
            return ExtractUtil.getValue(data, "fromDiskCache", false);
        }

        @Override
        public String requestId() {
            return ExtractUtil.getValue(data, "requestId", "-1");
        }

        @Override
        public int statusCode() {
            return ExtractUtil.getValue(data, "statusCode", 200);
        }

        @Override
        public String reasonPhrase() {
            return ExtractUtil.getValue(data, "reasonPhrase", "OK");
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
