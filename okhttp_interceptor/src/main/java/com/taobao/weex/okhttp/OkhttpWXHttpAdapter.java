package com.taobao.weex.okhttp;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.HttpMethod;
import com.taobao.weex.adapter.IWXHttpAdapter;
import com.taobao.weex.common.WXRequest;
import com.taobao.weex.common.WXResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Created by moxun on 16/12/21.
 */

public class OkhttpWXHttpAdapter implements IWXHttpAdapter {
    @Override
    public void sendRequest(WXRequest request, final OnHttpListener listener) {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setConnectTimeout(request.timeoutMs, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(request.timeoutMs, TimeUnit.MILLISECONDS);
        httpClient.networkInterceptors().add(new WeexOkhttpInterceptor());

        String method = request.method == null ? "GET" : request.method.toUpperCase();
        String requestBodyString = request.body == null ? "{}" : request.body;
        RequestBody body = HttpMethod.requiresRequestBody(method) ? RequestBody.create(MediaType.parse("application/json"), requestBodyString) : null;

        Request.Builder requestBuilder = new Request.Builder()
                .url(request.url)
                .method(method, body);

        for (Map.Entry<String, String> param : request.paramMap.entrySet()) {
            requestBuilder.addHeader(param.getKey(), param.getValue());
        }

        httpClient.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                WXResponse wxResponse = new WXResponse();
                wxResponse.errorMsg = e.getMessage();
                wxResponse.errorCode = "-1";
                wxResponse.statusCode = "-1";
                listener.onHttpFinish(wxResponse);
            }

            @Override
            public void onResponse(Response response) {
                WXResponse wxResponse = new WXResponse();
                byte[] responseBody = new byte[0];
                try {
                    responseBody = response.body().bytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                wxResponse.data = new String(responseBody);
                wxResponse.statusCode = String.valueOf(response.code());
                wxResponse.originalData = responseBody;
                wxResponse.extendParams = new HashMap<>();

                Headers headers = response.headers();
                for (String name : headers.names()) {
                    wxResponse.extendParams.put(name, headers.get(name));
                }

                if (response.code() < 200 || response.code() > 299) {
                    wxResponse.errorMsg = response.message();
                }

                listener.onHttpFinish(wxResponse);
            }
        });
    }
}
