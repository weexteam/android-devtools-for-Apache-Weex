package com.taobao.weex.devtools.toolbox;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.taobao.weex.common.WXJSExceptionInfo;
import com.taobao.weex.inspector.R;

/**
 * Created by moxun on 2017/8/15.
 */

public class JSExceptionDetailView extends FrameLayout {
  private TextView weexVersion;
  private TextView bundleUrl;
  private TextView jsfmVersion;
  private TextView exceptionDetail;
  private TextView instanceId;
  private TextView errorCode;

  public JSExceptionDetailView(@NonNull Context context) {
    super(context);
    init();
  }

  public JSExceptionDetailView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public JSExceptionDetailView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    LayoutInflater.from(getContext()).inflate(R.layout.view_js_exception_detail, this, true);
    instantiationViews();
  }

  private void instantiationViews() {
    weexVersion = (TextView) findViewById(R.id.weex_version);
    bundleUrl = (TextView) findViewById(R.id.bundle_url);
    jsfmVersion = (TextView) findViewById(R.id.jsfm_version);
    exceptionDetail = (TextView) findViewById(R.id.exception_detail);
    instanceId = (TextView) findViewById(R.id.instance_id);
    errorCode = (TextView) findViewById(R.id.error_code);

    exceptionDetail.setTypeface(Typeface.MONOSPACE);
  }

  public void renderException(WXJSExceptionInfo info) {
    bundleUrl.setText(info.getBundleUrl());
    instanceId.setText("Instance: " + info.getInstanceId());
    errorCode.setText("Code: " + info.getErrCode());
    weexVersion.setText("Weex: " + info.getWeexVersion());
    jsfmVersion.setText("JSFM: " + info.getJsFrameworkVersion());
    exceptionDetail.setText(info.getException());
  }
}
