package com.taobao.weex.devtools.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.taobao.weex.utils.WXLogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by moxun on 2017/7/27.
 */

public class JsLogAdapter extends RecyclerView.Adapter<JsLogAdapter.ItemHolder> implements WXLogUtils.JsLogWatcher, Filterable {
  private static JsLogAdapter sInstance;
  private List<LogItem> originLogItems = new CopyOnWriteArrayList<>();
  private List<LogItem> displayedLogItems = new CopyOnWriteArrayList<>();
  private int currentLogLevel = Log.VERBOSE;

  public static synchronized JsLogAdapter getInstance() {
    //Haha, ugly singleton but quick to write
    //Performance? don't care ~
    if (sInstance == null) {
      sInstance = new JsLogAdapter();
    }
    return sInstance;
  }

  private JsLogAdapter() {

  }

  @Override
  public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LinearLayout linearLayout = new LinearLayout(parent.getContext());
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    TextView textView = new TextView(parent.getContext());
    textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    linearLayout.addView(textView);
    View divider = new View(parent.getContext());
    divider.setBackgroundColor(Color.GRAY);
    divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    linearLayout.addView(divider);
    return new ItemHolder(linearLayout);
  }

  @Override
  public void onBindViewHolder(ItemHolder holder, int position) {
    LogItem logItem = displayedLogItems.get(position);
    holder.render(logItem);
  }

  @Override
  public int getItemCount() {
    return displayedLogItems.size();
  }

  @Override
  public void onJsLog(int level, String log) {
    while (originLogItems.size() >= 2000) {
      originLogItems.remove(0);
    }
    LogItem item = new LogItem();
    item.level = level;
    item.msg = log;
    originLogItems.add(item);
    if (level >= currentLogLevel) {
      displayedLogItems.add(item);
      notifyItemInserted(displayedLogItems.size() - 1);
    }
  }

  @Override
  public Filter getFilter() {
    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults filterResults = new FilterResults();
        filterResults.values = new ArrayList<>();
        for (LogItem item : originLogItems) {
          if (item.level >= currentLogLevel) {
            if (TextUtils.isEmpty(constraint)) {
              ((List) filterResults.values).add(item);
            } else if (item.msg.toLowerCase().contains(constraint.toString())) {
              ((List) filterResults.values).add(item);
            }
          }
        }
        filterResults.count = ((List) filterResults.values).size();
        return filterResults;
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
        displayedLogItems.clear();
        displayedLogItems.addAll((List)results.values);
        notifyDataSetChanged();
      }
    };
  }

  public void setLogLevel(int level) {
    currentLogLevel = level;
    getFilter().filter("");
  }

  public int getLogLevel() {
    return currentLogLevel;
  }

  class ItemHolder extends RecyclerView.ViewHolder {
    public ItemHolder(View itemView) {
      super(itemView);
    }

    public void render(LogItem logItem) {
      TextView textView = getTextView();
      if (textView != null) {
        switch (logItem.level) {
          case Log.VERBOSE:
          case Log.DEBUG:
            textView.setTextColor(Color.parseColor("#B4000000"));
            break;
          case Log.INFO:
            textView.setTextColor(Color.parseColor("#1E00CA"));
            break;
          case Log.WARN:
            textView.setTextColor(Color.parseColor("#E9B200"));
            break;
          case Log.ERROR:
            textView.setTextColor(Color.parseColor("#EF0000"));
            break;
        }
        textView.setText(logItem.msg);
      }
    }

    private TextView getTextView() {
      if (itemView instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) itemView;
        if (viewGroup.getChildCount() == 2 && viewGroup.getChildAt(0) instanceof TextView) {
          return (TextView) viewGroup.getChildAt(0);
        }
      }
      return null;
    }
  }

  class LogItem {
    int level;
    String msg;
  }
}
