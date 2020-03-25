package com.taobao.weex.devtools.toolbox;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
//import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.taobao.weex.inspector.R;

public class PerformanceActivity extends AppCompatActivity {

  private LinearLayout segmentHost;

  public static void start(Context context, int instanceId) {
    Intent intent = new Intent(context, PerformanceActivity.class);
    intent.putExtra("instanceId", instanceId);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_performance);
    segmentHost = (LinearLayout) findViewById(R.id.segment_control);
    addTab("页面性能", true, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        switchFragment(EventOverviewFragment.getInstance(getIntent().getIntExtra("instanceId", -1)));
      }
    });

    addTab("JS LOG", false, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        switchFragment(new JsLogFragment());
      }
    });

    addTab("环境变量", false, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        switchFragment(new EnvironmentFragment());
      }
    });

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("Weex Monitor");
    }
  }

  private void switchFragment(Fragment fragment) {
    int count = getSupportFragmentManager().getBackStackEntryCount();
    if (count > 0) {
      getSupportFragmentManager().popBackStack();
    }
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commitAllowingStateLoss();
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    FragmentManager fragmentManager = getSupportFragmentManager();

    Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
    if (fragment == null) {
      fragmentManager
          .beginTransaction()
          .add(R.id.fragment_container, EventOverviewFragment.getInstance(getIntent().getIntExtra("instanceId", -1)))
          .commitAllowingStateLoss();
    }
  }

  private void addTab(String title, final boolean selected, final View.OnClickListener listener) {
    final TextView textView = new TextView(this);
    textView.setText(title);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    lp.weight = 1;
    lp.setMargins(1, 1, 1, 1);
    if (selected) {
      textView.setBackgroundColor(Color.TRANSPARENT);
    } else {
      textView.setBackgroundColor(Color.WHITE);
    }
    textView.setGravity(Gravity.CENTER);
    textView.setLayoutParams(lp);
    textView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!v.isSelected() && v instanceof TextView) {
          listener.onClick(v);
          for (int i = 0; i < segmentHost.getChildCount(); i++) {
            segmentHost.getChildAt(i).setSelected(false);
            segmentHost.getChildAt(i).setBackgroundColor(Color.WHITE);
            ((TextView) segmentHost.getChildAt(i)).setTextColor(Color.parseColor("#1E90FF"));
          }
          v.setSelected(true);
          v.setBackgroundColor(Color.TRANSPARENT);
          ((TextView) v).setTextColor(Color.WHITE);
        }
      }
    });
    textView.setSelected(selected);
    if (selected) {
      textView.setTextColor(Color.WHITE);
    } else {
      textView.setTextColor(Color.parseColor("#1E90FF"));
    }
    segmentHost.addView(textView);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
