package com.taobao.weex.devtools.toolbox;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.taobao.weex.devtools.adapter.JsLogAdapter;
import com.taobao.weex.inspector.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class JsLogFragment extends Fragment {


  private View rootView;
  private RecyclerView logList;
  private Spinner logLevel;
  private SearchView searchView;

  public JsLogFragment() {
    // Required empty public constructor
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    rootView = inflater.inflate(R.layout.fragment_js_log, container, false);
    instantiationViews();
    logList.setLayoutManager(new LinearLayoutManager(getContext()));
    logList.setAdapter(JsLogAdapter.getInstance());

    logLevel.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, new String[]{"VERBOSE", "DEBUG", "INFO", "WARN", "ERROR", "ASSERT"}) {
      @NonNull
      @Override
      public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view instanceof TextView) {
          ((TextView) view).setTextSize(12);
        }
        return view;
      }
    });

    logLevel.setSelection(JsLogAdapter.getInstance().getLogLevel() - 2);

    logLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        JsLogAdapter.getInstance().setLogLevel(position + 2);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        JsLogAdapter.getInstance().getFilter().filter(newText);
        return true;
      }
    });

    return rootView;
  }

  private void instantiationViews() {
    logLevel = (Spinner) rootView.findViewById(R.id.log_level);
    logList = (RecyclerView) rootView.findViewById(R.id.log_list);
    searchView = (SearchView) rootView.findViewById(R.id.search_view);
  }
}
