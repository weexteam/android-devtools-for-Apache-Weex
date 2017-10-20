package com.taobao.weex.devtools.trace;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXScroller;
import com.taobao.weex.ui.component.list.WXListComponent;
import com.taobao.weex.ui.view.listview.WXRecyclerView;
import com.taobao.weex.ui.view.refresh.wrapper.BounceRecyclerView;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

class ComponentHeightComputer {
  private ComponentHeightComputer(){}

  static int computeComponentContentHeight(@NonNull WXComponent component) {
    View view = component.getHostView();
    if(view == null) {
      return 0;
    }
    if(component instanceof WXListComponent) {
      WXListComponent listComponent = (WXListComponent) component;
      BounceRecyclerView bounceRecyclerView = listComponent.getHostView();
      if(bounceRecyclerView == null) {
        return 0;
      }
      WXRecyclerView innerView = bounceRecyclerView.getInnerView();
      if(innerView == null) {
        return bounceRecyclerView.getMeasuredHeight();
      } else {
        return innerView.computeVerticalScrollRange();
      }
    } else if(component instanceof WXScroller) {
      WXScroller scroller = (WXScroller) component;
      if(!ViewUtils.isVerticalScroller(scroller)) {
        return view.getMeasuredHeight();
      }
      ViewGroup group = scroller.getInnerView();
      if(group == null) {
        return view.getMeasuredHeight();
      }
      if(group.getChildCount() != 1) {
        return view.getMeasuredHeight();
      } else {
        return group.getChildAt(0).getMeasuredHeight();
      }
    } else {
      return view.getMeasuredHeight();
    }
  }

}