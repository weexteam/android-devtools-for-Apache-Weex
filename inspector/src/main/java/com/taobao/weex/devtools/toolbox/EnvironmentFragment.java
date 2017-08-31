package com.taobao.weex.devtools.toolbox;


import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.taobao.weex.WXEnvironment;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXModuleManager;
import com.taobao.weex.bridge.WXParams;
import com.taobao.weex.common.TypeModuleFactory;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.inspector.R;
import com.taobao.weex.ui.IFComponentHolder;
import com.taobao.weex.ui.WXComponentRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class EnvironmentFragment extends Fragment {

  Spinner spinner;
  ListView list;
  TextView info;
  List<String> items = new ArrayList<>();
  ArrayAdapter arrayAdapter;

  public EnvironmentFragment() {
    // Required empty public constructor
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View root = inflater.inflate(R.layout.fragment_environment, container, false);
    spinner = (Spinner) root.findViewById(R.id.spinner);
    spinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new String[]{"Modules", "Components", "Environment"}));
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
          case 0:
            showModules();
            break;
          case 1:
            showComponents();
            break;
          case 2:
            showEnvironment();
            break;
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

    list = (ListView) root.findViewById(R.id.list);
    info = (TextView) root.findViewById(R.id.info);
    info.setTypeface(Typeface.MONOSPACE);

    arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items);
    list.setAdapter(arrayAdapter);
    showModules();

    return root;
  }

  private void showModules() {
    final Map<String, TypeModuleFactory> modules = new HashMap<>();
    for (Map.Entry<String, TypeModuleFactory> item : EnvironmentUtil.getExistedModules(true).entrySet()) {
      modules.put(item.getKey() + "  [global]", item.getValue());
    }
    modules.putAll(EnvironmentUtil.getExistedModules(false));
    list.setVisibility(View.VISIBLE);
    info.setText("");
    items.clear();
    items.addAll(modules.keySet());
    ((ArrayAdapter) list.getAdapter()).notifyDataSetChanged();
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = items.get(position);
        TypeModuleFactory factory = modules.get(item);
        if (factory != null) {
          JSONArray array = new JSONArray(Arrays.asList(factory.getMethods()));
          try {
            info.setText(array.toString(2));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }
    });
  }

  private void showComponents() {
    final Map<String, IFComponentHolder> components = EnvironmentUtil.getExistedComponents();
    list.setVisibility(View.VISIBLE);
    info.setText("");
    items.clear();
    items.addAll(components.keySet());
    ((ArrayAdapter) list.getAdapter()).notifyDataSetChanged();
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = items.get(position);
        IFComponentHolder holder = components.get(item);
        if (holder != null) {
          JSONArray array = new JSONArray(Arrays.asList(holder.getMethods()));
          try {
            info.setText(array.toString(2));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }
    });
  }

  private void showEnvironment() {
    list.setVisibility(View.GONE);
    WXParams params = WXBridgeManager.getInstance().getInitParams();
    Map<String, String> environment = WXEnvironment.getConfig();
    environment.put("platform", params.getPlatform());
    environment.put("osVersion", params.getOsVersion());
    environment.put("appVersion", params.getAppVersion());
    environment.put("weexVersion", params.getWeexVersion());
    environment.put("deviceModel", params.getDeviceModel());
    environment.put("appName", params.getAppName());
    environment.put("deviceWidth", params.getDeviceWidth());
    environment.put("deviceHeight", params.getDeviceHeight());
    environment.put("shouldInfoCollect", params.getShouldInfoCollect());

    environment.putAll(WXEnvironment.getCustomOptions());
    JSONObject jsonObject = new JSONObject(environment);
    try {
      info.setText(jsonObject.toString(2));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private static class EnvironmentUtil {
    public static Map<String, TypeModuleFactory> getExistedModules(boolean global) {
      Map<String, TypeModuleFactory> modules = new HashMap<>();
      if (global) {
        try {
          Field sGlobalField = WXModuleManager.class.getDeclaredField("sGlobalModuleMap");
          if (sGlobalField != null) {
            sGlobalField.setAccessible(true);
            Map map = (Map) sGlobalField.get(null);
            for (Object key : map.keySet()) {
              Object value = map.get(key);
              if (value instanceof WXModule) {
                TypeModuleFactory factory = new TypeModuleFactory(value.getClass());
                modules.put((String) key, factory);
              }
            }
          }
        } catch (Throwable t) {
          //ignore
        }
      } else {
        try {
          Field sModuleField = WXModuleManager.class.getDeclaredField("sModuleFactoryMap");
          if (sModuleField != null) {
            sModuleField.setAccessible(true);
            return (Map) sModuleField.get(null);
          }
        } catch (Throwable t) {
          //ignore
        }
      }
      return modules;
    }

    public static Map<String, IFComponentHolder> getExistedComponents() {
      try {
        Field sComponentsField = WXComponentRegistry.class.getDeclaredField("sTypeComponentMap");
        sComponentsField.setAccessible(true);
        return (Map<String, IFComponentHolder>) sComponentsField.get(null);
      } catch (Throwable t) {
        t.printStackTrace();
      }
      return new HashMap<>();
    }
  }
}
