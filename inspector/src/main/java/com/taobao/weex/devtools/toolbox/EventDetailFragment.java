package com.taobao.weex.devtools.toolbox;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.taobao.weex.WXSDKManager;
import com.taobao.weex.adapter.ITracingAdapter;
import com.taobao.weex.devtools.adapter.WXTracingAdapter;
import com.taobao.weex.inspector.R;
import com.taobao.weex.tracing.WXTracing;
import com.taobao.weex.utils.WXViewUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * A simple {@link Fragment} subclass.
 */
public class EventDetailFragment extends Fragment {

  private WXTracing.TraceEvent rootEvent;
  private View rootView;
  private TextView eventName;
  private LinearLayout subEvents;
  private TextView eventPayload;

  public EventDetailFragment() {
    // Required empty public constructor
  }

  public static EventDetailFragment getFragment(int instanceId, int traceId) {
    EventDetailFragment fragment = new EventDetailFragment();
    Bundle args = new Bundle();
    args.putInt("instanceId", instanceId);
    args.putInt("traceId", traceId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    int iid = getArguments().getInt("instanceId");
    int traceId = getArguments().getInt("traceId");
    ITracingAdapter adapter = WXSDKManager.getInstance().getTracingAdapter();
    if (adapter != null && adapter instanceof WXTracingAdapter) {
      rootEvent = ((WXTracingAdapter) adapter).getTraceEventByInstanceId(iid).subEvents.get(traceId);
    }
    rootView = inflater.inflate(R.layout.fragment_event_detail, container, false);
    instantiationViews();

    eventName.setText(rootEvent.fname);

    for (int i = 0; i < rootEvent.subEvents.size(); i++) {
      WXTracing.TraceEvent event = rootEvent.subEvents.valueAt(i);

      if ("DomExecute".equals(event.fname) || "UIExecute".equals(event.fname)) {
        continue;
      }

      EventView eventView = new EventView(getContext());
      eventView.desc.setText(event.fname);
      subEvents.addView(eventView);

      long gap = event.ts  - rootEvent.ts;
      int margin = (int) (gap / rootEvent.duration * (WXViewUtils.getScreenWidth(getContext()) - WXViewUtils.dip2px(8)));
      int width = (int) (event.duration / rootEvent.duration * WXViewUtils.getScreenWidth(getContext()));
      ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) eventView.indicator.getLayoutParams();
      lp.width = width + WXViewUtils.dip2px(2);
      lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
      lp.leftMargin = margin - WXViewUtils.dip2px(2);
      lp.bottomMargin = 1;

      if ("DOMThread".equals(event.tname)) {
        eventView.indicator.setBackgroundColor(Color.parseColor("#84A6E8"));
      } else if ("UIThread".equals(event.tname)) {
        eventView.indicator.setBackgroundColor(Color.parseColor("#83B86E"));
      } else {
        eventView.indicator.setBackgroundColor(Color.CYAN);
      }
      eventView.indicator.setLayoutParams(lp);
      eventView.duration.setText(event.duration + " ms");
    }

    if (rootEvent.payload != null) {
      try {
        if (rootEvent.payload.startsWith("{")) {
          JSONObject jsonObject = new JSONObject(rootEvent.payload);
          rootEvent.payload = jsonObject.toString(2);
        } else if (rootEvent.payload.startsWith("[")) {
          JSONArray jsonArray = new JSONArray(rootEvent.payload);
          rootEvent.payload = jsonArray.toString(2);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      eventPayload.setText(rootEvent.payload);
    }

    return rootView;
  }

  private void instantiationViews() {
    eventName = (TextView) rootView.findViewById(R.id.event_name);
    subEvents = (LinearLayout) rootView.findViewById(R.id.sub_events);
    eventPayload = (TextView) rootView.findViewById(R.id.event_payload);
    eventPayload.setTypeface(Typeface.MONOSPACE);
  }

  private static class EventView extends FrameLayout {
    TextView desc;
    TextView duration;
    View indicator;

    public EventView(@NonNull Context context) {
      super(context);
      init();
    }

    private void init() {
      indicator = new View(getContext());
      desc = new TextView(getContext());
      duration = new TextView(getContext());
      duration.setGravity(Gravity.RIGHT);

      addView(indicator);
      addView(desc);
      addView(duration);
    }
  }
}
