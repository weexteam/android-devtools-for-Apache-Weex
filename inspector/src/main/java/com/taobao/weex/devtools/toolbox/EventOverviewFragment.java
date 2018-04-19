package com.taobao.weex.devtools.toolbox;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.taobao.weex.WXSDKManager;
import com.taobao.weex.adapter.ITracingAdapter;
import com.taobao.weex.devtools.adapter.WXTracingAdapter;
import com.taobao.weex.tracing.WXTracing;
import com.taobao.weex.ui.WXRenderManager;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.view.listview.WXRecyclerView;

import com.taobao.weex.inspector.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventOverviewFragment extends Fragment {

  private RecyclerView list;

  public EventOverviewFragment() {
    // Required empty public constructor
  }

  public static EventOverviewFragment getInstance(int instanceId) {
    Bundle bundle = new Bundle();
    bundle.putInt("instanceId", instanceId);
    EventOverviewFragment fragment = new EventOverviewFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_event_overview, container, false);
    int instanceId = getArguments().getInt("instanceId", -1);
    list = (RecyclerView) root.findViewById(R.id.perf_list);
    list.setLayoutManager(new LinearLayoutManager(getContext()));
    ITracingAdapter adapter = WXSDKManager.getInstance().getTracingAdapter();
    if (adapter != null && adapter instanceof WXTracingAdapter) {
      if (instanceId != -1) {
        list.setAdapter(new PerfListAdapter(((WXTracingAdapter) adapter).getTraceEventByInstanceId(instanceId)));
      }
    }
    return root;
  }

  private class PerfListAdapter extends WXRecyclerView.Adapter<ItemHolder> {
    private WXTracing.TraceEvent rootEvent;

    public PerfListAdapter(WXTracing.TraceEvent rootEvent) {
      this.rootEvent = rootEvent;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ItemHolder(LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_trace_event_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {
      final WXTracing.TraceEvent event = rootEvent.subEvents.valueAt(position);
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (event.subEvents == null) {
            return;
          }
          getActivity()
              .getSupportFragmentManager()
              .beginTransaction()
              .replace(R.id.fragment_container, EventDetailFragment.getFragment(rootEvent.traceId, event.traceId))
              .addToBackStack(EventDetailFragment.class.getSimpleName())
              .commitAllowingStateLoss();
        }
      });

      if (event.subEvents == null) {
        holder.info.setVisibility(View.INVISIBLE);
      } else {
        holder.info.setVisibility(View.VISIBLE);
      }

      if(event.ts < rootEvent.ts) {
        rootEvent.ts = event.ts;
      }
      holder.actionName.setText(event.fname);
      holder.actionDuration.setText(event.duration + " ms");
      final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.duration.getLayoutParams();
      holder.itemView.post(new Runnable() {
        @Override
        public void run() {
          long gap = event.ts  - rootEvent.ts;
          int margin = (int) (gap / rootEvent.duration * holder.getItemWidth());
          int width = (int) (event.duration / rootEvent.duration * holder.getItemWidth());
          lp.width = width;
          lp.leftMargin = margin;
          holder.duration.setLayoutParams(lp);
        }
      });
      if (event.ref != null) {
        WXRenderManager renderManager = WXSDKManager.getInstance().getWXRenderManager();
        WXComponent component = renderManager.getWXComponent(event.iid, event.ref);
        if (component != null) {
          String type = component.getComponentType();
          holder.compType.setText("<" + type + "/>");
          if (component.getRealView() != null) {
            holder.viewType.setText(component.getRealView().getClass().getSimpleName());
          }
          if (component.isLazy()) {
            holder.compType.append(" @lazy");
          }
          holder.compRef.setText("Ref: " + component.getRef());
        }
      } else {
        holder.compType.setText("-");
        holder.viewType.setText("-");
        holder.compRef.setText("-");
      }
    }

    @Override
    public int getItemCount() {
      return rootEvent.subEvents.size();
    }
  }

  public class ItemHolder extends RecyclerView.ViewHolder {
    public TextView actionName;
    public TextView compRef;
    public View duration;
    public LinearLayout infoContent;
    public TextView actionDuration;
    public TextView viewType;
    public TextView compType;
    public ImageView info;

    public ItemHolder(View itemView) {
      super(itemView);
      actionName = (TextView) itemView.findViewById(R.id.action_name);
      compRef = (TextView) itemView.findViewById(R.id.comp_ref);
      duration = (View) itemView.findViewById(R.id.duration);
      infoContent = (LinearLayout) itemView.findViewById(R.id.info_content);
      actionDuration = (TextView) itemView.findViewById(R.id.action_duration);
      viewType = (TextView) itemView.findViewById(R.id.view_type);
      compType = (TextView) itemView.findViewById(R.id.comp_type);
      info = (ImageView) itemView.findViewById(R.id.info);
    }

    public int getItemWidth() {
      return infoContent.getMeasuredWidth() - 200;
    }
  }
}
