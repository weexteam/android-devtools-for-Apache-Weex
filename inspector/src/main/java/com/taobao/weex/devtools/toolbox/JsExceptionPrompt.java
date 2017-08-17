package com.taobao.weex.devtools.toolbox;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.adapter.IWXJSExceptionAdapter;
import com.taobao.weex.common.WXJSExceptionInfo;

/**
 * Created by moxun on 2017/8/15.
 */

public class JsExceptionPrompt implements IWXJSExceptionAdapter {
  @Override
  public void onJSException(WXJSExceptionInfo info) {
    WXSDKInstance instance = WXSDKManager.getInstance().getSDKInstance(info.getInstanceId());
    if (instance != null && instance.getContext() != null && info != null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(instance.getContext());
      builder.setTitle("JS Exception on: " + info.getFunction());
      JSExceptionDetailView detailView = new JSExceptionDetailView(instance.getContext());
      detailView.renderException(info);
      builder.setView(detailView);
      builder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();
        }
      });
      builder.create().show();
    }
  }
}
