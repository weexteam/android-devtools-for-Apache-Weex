package com.taobao.weex.devtools.adapter;

import android.graphics.Color;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.taobao.weex.RenderContainer;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.adapter.ITracingAdapter;
import com.taobao.weex.common.WXPerformance;
import com.taobao.weex.devtools.common.LogUtil;
import com.taobao.weex.devtools.debug.DebugBridge;
import com.taobao.weex.devtools.toolbox.PerformanceActivity;
import com.taobao.weex.devtools.trace.DomTracker;
import com.taobao.weex.devtools.trace.HealthReport;
import com.taobao.weex.tracing.WXTracing;
import com.taobao.weex.utils.WXLogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by moxun on 2017/7/6.
 */

public class WXTracingAdapter implements ITracingAdapter {
  private volatile SparseArray<WXTracing.TraceEvent> traceEvents = new SparseArray<>();

  public WXTracingAdapter() {
    WXSDKManager.getInstance().registerInstanceLifeCycleCallbacks(new WXSDKManager.InstanceLifeCycleCallbacks() {
      @Override
      public void onInstanceDestroyed(String s) {
        if (traceEvents != null) {
          WXLogUtils.d("WXTracingAdapter", "Destroy trace events with instance id " + s);
          traceEvents.remove(Integer.parseInt(s));
        }
      }

      @Override
      public void onInstanceCreated(String s) {

      }
    });
  }

  @Override
  public void enable() {

  }

  @Override
  public void disable() {

  }

  @Override
  public void submitTracingEvent(WXTracing.TraceEvent event) {

    int instanceId = Integer.parseInt(event.iid);
    if (instanceId == -1) {
      WXLogUtils.e("Wrong instance id: " + instanceId);
    }
    WXTracing.TraceEvent head = traceEvents.get(instanceId);

    if (head == null) {
      head = new WXTracing.TraceEvent();
      head.traceId = instanceId;
      head.ts = event.ts;
      head.subEvents = new SparseArray<>();
      head.extParams = new HashMap<>();
      traceEvents.append(instanceId, head);
    }

    if ("renderFinish".equals(event.fname)) {
      head.duration = event.duration;
      if (head.subEvents != null) {
        event.duration = 0;
        head.subEvents.append(event.traceId, event);
        sendTracingData(event.iid);
        //enableMonitor(event.iid);
      }
      return;
    }

    if (event.parentId == -1) {
      if (head.subEvents == null) {
        head.subEvents = new SparseArray<>();
      }
      if ("B".equals(event.ph) || "X".equals(event.ph)) {
        head.subEvents.append(event.traceId, event);
      } else if ("E".equals(event.ph)) {
        WXTracing.TraceEvent beginEvent = head.subEvents.get(event.traceId);
        if (beginEvent == null) {
          WXLogUtils.w("WXTracingAdapter", "begin event not found: " + event.fname + "@" + event.traceId);
          return;
        }
        beginEvent.duration = event.ts - beginEvent.ts;
        beginEvent.ph = "X";
      }
    } else {
      WXTracing.TraceEvent parent = head.subEvents.get(event.parentId);
      if (parent != null) {
        if (parent.subEvents == null) {
          parent.subEvents = new SparseArray<>();
        }
        parent.subEvents.append(event.traceId, event);
      }
    }
  }

  public WXTracing.TraceEvent getTraceEventByInstanceId(final int iid) {
    return traceEvents.get(iid);
  }

  private void enableMonitor(final String instanceId) {
    final WXSDKInstance instance = WXSDKManager.getInstance().getSDKInstance(instanceId);
    if (instance != null) {
      RenderContainer container = (RenderContainer) instance.getContainerView();
      TextView textView = new TextView(instance.getUIContext());
      textView.setText("Weex MNT:" + instanceId);
      textView.setBackgroundColor(Color.parseColor("#AA1E90FF"));
      textView.setTextColor(Color.WHITE);
      textView.setPadding(10, 10, 10, 10);
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lp.gravity = Gravity.RIGHT | Gravity.CENTER;
      textView.setLayoutParams(lp);

      container.addView(textView);
      textView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          PerformanceActivity.start(instance.getUIContext(), Integer.parseInt(instanceId));
        }
      });
    }
  }

  private void sendTracingData(final String instanceId) {

    if (!DebugBridge.getInstance().isSessionActive()) {
      WXLogUtils.w("WXTracingAdapter", "Debug session not active");
      return;
    }

    JSONArray array = new JSONArray();
    collectNativeTracingData(traceEvents.get(Integer.parseInt(instanceId)), array);
    try {
      JSONObject rpc = new JSONObject();
      JSONObject data = new JSONObject();
      data.put("data", array);
      rpc.put("method", "WxDebug.sendTracingData");
      rpc.put("params", data);

      DebugBridge.getInstance().sendToRemote(rpc.toString());
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }

    DebugBridge.getInstance().post(new Runnable() {
      @Override
      public void run() {
        sendSummaryInfo(String.valueOf(instanceId));
      }
    });

    WXLogUtils.d("WXTracingAdapter", "Send tracing data with instance id " + instanceId);
  }

  private void collectNativeTracingData(WXTracing.TraceEvent head, JSONArray out) {
    if (head.subEvents != null) {
      for (int i = 0; i < head.subEvents.size(); i++) {
        WXTracing.TraceEvent subEvent = head.subEvents.valueAt(i);
        if (subEvent.isSegment) {
          continue;
        }
        if ("domBatch".equals(subEvent.fname)) {
          //continue;
        }
        JSONObject object = parseToJSONObject(subEvent);
        if ("JSThread".equals(subEvent.tname)) {
          try {
            object.put("duration", subEvent.parseJsonTime);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        out.put(object);
        collectNativeTracingData(subEvent, out);
      }
    }
  }

  private void sendSummaryInfo(String instanceId) {
    WXSDKInstance instance = WXSDKManager.getInstance().getSDKInstance(instanceId);
    if (instance != null) {
      WXPerformance performance = instance.getWXPerformance();
      try {
        JSONObject params = new JSONObject();
        params.put("platform", "Android");
        params.put("JSTemplateSize", performance.JSTemplateSize);
        params.put("screenRenderTime", performance.screenRenderTime);
        params.put("totalTime", performance.totalTime);
        params.put("networkTime", performance.networkTime);

        HealthReport report = new DomTracker(instance).traverse();
        if (report != null) {
          params.put("maxDeepViewLayer", report.maxLayer);
          params.put("componentCount", report.componentCount);
        }

        JSONObject rpc = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("summaryInfo", params);
        rpc.put("method", "WxDebug.sendSummaryInfo");
        rpc.put("params", data);
        LogUtil.d("SummaryInfo", params.toString());
        DebugBridge.getInstance().sendToRemote(rpc.toString());
      } catch (JSONException e) {
        e.printStackTrace();
      }
    } else {
      WXLogUtils.e("WXTracing", "Instance " + instanceId + " not found");
    }
  }

  private JSONObject parseToJSONObject(WXTracing.TraceEvent event) {
    JSONObject object = new JSONObject();
    try {
      object.put("parentId", event.parentId);
      object.put("ref", event.ref);
      object.put("parentRef", event.parentRef);
      object.put("className", event.classname);
      object.put("ts", event.ts);
      object.put("traceId", event.traceId);
      object.put("iid", event.iid);
      object.put("duration", event.duration);
      object.put("fName", event.fname);
      object.put("ph", event.ph);
      object.put("name", event.name);
      object.put("tName", event.tname);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return object;
  }
}
