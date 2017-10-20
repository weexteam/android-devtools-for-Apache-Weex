package com.taobao.weex.devtools.trace;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXEmbed;
import com.taobao.weex.ui.component.WXScroller;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.component.list.WXCell;
import com.taobao.weex.ui.component.list.WXListComponent;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Description:
 *
 * Created by rowandjj(chuyi)<br/>
 */

public class DomTracker {
  private WXSDKInstance mWxInstance;
  private Deque<LayeredNode<WXComponent>> mLayeredQueue;
  private ObjectPool<LayeredNode<WXComponent>> mVDomObjectPool;
  private ObjectPool<LayeredNode<View>> mRealDomObjectPool;

  private static final String TAG = "VDomTracker";

  private OnTrackNodeListener mOnTrackNodeListener;

  private static final int START_LAYER_OF_VDOM = 2;//instance为第一层，rootComponent为第二层
  private static final int START_LAYER_OF_REAL_DOM = 2;//renderContainer为第一层



  interface OnTrackNodeListener {
    void onTrackNode(@NonNull WXComponent component, int layer);
  }

  public DomTracker(@NonNull WXSDKInstance instance) {
    this.mWxInstance = instance;
    mLayeredQueue = new ArrayDeque<>();
    mVDomObjectPool = new ObjectPool<LayeredNode<WXComponent>>(10) {
      @Override
      LayeredNode<WXComponent> newObject() {
        return new LayeredNode<>();
      }
    };
    mRealDomObjectPool = new ObjectPool<LayeredNode<View>>(15) {
      @Override
      LayeredNode<View> newObject() {
        return new LayeredNode<>();
      }
    };
  }

  void setOnTrackNodeListener(OnTrackNodeListener listener) {
    this.mOnTrackNodeListener = listener;
  }

  @Nullable
  public HealthReport traverse() {
    long start = System.currentTimeMillis();
    if (Looper.getMainLooper() == Looper.myLooper()) {
      WXLogUtils.e(TAG, "illegal thread...");
      return null;
    }
    int pageHeight = 0;
    WXComponent godComponent = mWxInstance.getRootComponent();
    if (godComponent == null) {
      WXLogUtils.e(TAG, "god component not found");
      return null;
    }
    HealthReport report = new HealthReport(mWxInstance.getBundleUrl());

    View hostView = godComponent.getHostView();
    if(hostView != null) {
      report.maxLayerOfRealDom = getRealDomMaxLayer(hostView);
      pageHeight = hostView.getMeasuredHeight();
    }

    LayeredNode<WXComponent> layeredNode = mVDomObjectPool.obtain();
    layeredNode.set(godComponent, ViewUtils.getComponentName(godComponent), START_LAYER_OF_VDOM);

    mLayeredQueue.add(layeredNode);

    while (!mLayeredQueue.isEmpty()) {
      LayeredNode<WXComponent> domNode = mLayeredQueue.removeFirst();
      WXComponent component = domNode.component;
      report.componentCount++;
      int layer = domNode.layer;

      report.maxLayer = Math.max(report.maxLayer,layer);
      report.estimateContentHeight = Math.max(report.estimateContentHeight,
          ComponentHeightComputer.computeComponentContentHeight(component));

      //如果节点被染色，需要计算其该染色子树的最大深度
      if(!TextUtils.isEmpty(domNode.tint)) {
        for(HealthReport.EmbedDesc desc : report.embedDescList) {
          if(desc.src != null && desc.src.equals(domNode.tint)) {
            desc.actualMaxLayer = Math.max(desc.actualMaxLayer,(layer-desc.beginLayer));
          }
        }
      }

      if (mOnTrackNodeListener != null) {
        mOnTrackNodeListener.onTrackNode(component, layer);
      }

      if (component instanceof WXListComponent) {
        report.hasList = true;

        if(report.listDescMap == null) {
          report.listDescMap = new LinkedHashMap<>();
        }

        HealthReport.ListDesc listDesc = report.listDescMap.get(component.getRef());
        if(listDesc == null) {
          listDesc = new HealthReport.ListDesc();
        }
        listDesc.ref = component.getRef();
        listDesc.totalHeight = ComponentHeightComputer.computeComponentContentHeight(component);
        report.listDescMap.put(listDesc.ref,listDesc);

      } else if (component instanceof WXScroller) {
        if(ViewUtils.isVerticalScroller((WXScroller) component)) {
          report.hasScroller = true;
        }

      } else if (component instanceof WXCell) {

        WXVContainer parentContainer = component.getParent();
        if(parentContainer != null && parentContainer instanceof WXListComponent && report.listDescMap != null) {
          HealthReport.ListDesc listDesc = report.listDescMap.get(parentContainer.getRef());
          if(listDesc != null) {
            listDesc.cellNum++;
          }
        }

        int num = getComponentNumOfNode(component);
        report.maxCellViewNum = Math.max(report.maxCellViewNum, num);

        if(((WXCell) component).getHostView() != null) {
          int height = ((WXCell) component).getHostView().getMeasuredHeight();
          report.hasBigCell |= isBigCell(height);
          report.componentNumOfBigCell = num;
        }

      } else if(component instanceof WXEmbed) {
        report.hasEmbed = true;
      }

      //restore to pool for later use
      domNode.clear();
      mVDomObjectPool.recycle(domNode);

      if(component instanceof WXEmbed) {

        if(report.embedDescList == null) {
          report.embedDescList = new ArrayList<>();
        }
        HealthReport.EmbedDesc desc = new HealthReport.EmbedDesc();
        desc.src = ((WXEmbed)component).getSrc();
        desc.beginLayer = layer;

        report.embedDescList.add(desc);

        //add nested component to layeredQueue
        WXComponent nestedRootComponent = ViewUtils.getNestedRootComponent((WXEmbed) component);
        if(nestedRootComponent != null) {
          LayeredNode<WXComponent> childNode = mVDomObjectPool.obtain();
          childNode.set(nestedRootComponent, ViewUtils.getComponentName(nestedRootComponent), layer+1);
          mLayeredQueue.add(childNode);

          //tint
          childNode.tint = desc.src;
        }
      } else if (component instanceof WXVContainer) {
        WXVContainer container = (WXVContainer) component;
        for (int i = 0, count = container.childCount(); i < count; i++) {
          WXComponent child = container.getChild(i);
          LayeredNode<WXComponent> childNode = mVDomObjectPool.obtain();
          childNode.set(child, ViewUtils.getComponentName(child), layer + 1);

          //if parent is tinted,then tint it's children
          if(!TextUtils.isEmpty(domNode.tint)) {
            childNode.tint = domNode.tint;
          }

          mLayeredQueue.add(childNode);

        }
      }
    }
    Context context = mWxInstance.getContext();
    if(context != null) {
      pageHeight = (pageHeight == 0) ? ViewUtils.getScreenHeight(context) : pageHeight;
    }

    if(pageHeight != 0) {
      report.estimatePages = String.format(Locale.CHINA,"%.2f",report.estimateContentHeight/(double) pageHeight);
    } else {
      report.estimatePages = "0";
    }

    long end = System.currentTimeMillis();
    WXLogUtils.d(TAG,"[traverse] elapse time :"+(end-start)+"ms");
    return report;

  }


  private int getComponentNumOfNode(@NonNull WXComponent rootNode) {
    Deque<WXComponent> deque = new ArrayDeque<>();
    deque.add(rootNode);
    int viewNum = 0;
    while (!deque.isEmpty()) {
      WXComponent node = deque.removeFirst();
      viewNum++;
      if (node instanceof WXVContainer) {
        WXVContainer container = (WXVContainer) node;
        for (int i = 0, count = container.childCount(); i < count; i++) {
          deque.add(container.getChild(i));
        }
      }
    }
    return viewNum;
  }

  private int getRealDomMaxLayer(@NonNull View rootView) {
    int maxLayer = 0;
    Deque<LayeredNode<View>> deque = new ArrayDeque<>();
    LayeredNode<View> rootNode = mRealDomObjectPool.obtain();
    rootNode.set(rootView,null,START_LAYER_OF_REAL_DOM);
    deque.add(rootNode);

    while (!deque.isEmpty()) {
      LayeredNode<View> currentNode = deque.removeFirst();
      maxLayer = Math.max(maxLayer,currentNode.layer);

      View component = currentNode.component;
      int layer = currentNode.layer;

      currentNode.clear();
      mRealDomObjectPool.recycle(currentNode);

      if(component instanceof ViewGroup && ((ViewGroup) component).getChildCount() > 0) {
        ViewGroup viewGroup = (ViewGroup) component;
        for (int i = 0,count = viewGroup.getChildCount(); i < count; i++) {
          View child = viewGroup.getChildAt(i);
          LayeredNode<View> childNode = mRealDomObjectPool.obtain();
          childNode.set(child,null,layer+1);
          deque.add(childNode);
        }
      }
    }

    return maxLayer;
  }

  private boolean isBigCell(float maxHeight) {
    if (maxHeight <= 0) {
      return false;
    }
    return maxHeight > WXViewUtils.getScreenHeight() * 2 / 3.0;
  }


  private static class LayeredNode<T> {
    T component;
    String simpleName;
    int layer;

    String tint;

    void set(T component, String simpleName, int layer) {
      this.component = component;
      this.layer = layer;
      this.simpleName = simpleName;
    }

    void clear() {
      component = null;
      layer = -1;
      simpleName = null;
    }
  }

  private static abstract class ObjectPool<T> {
    private final Deque<T> mPool;

    ObjectPool(int capacity) {
      capacity = Math.max(0, capacity);
      this.mPool = new ArrayDeque<>(capacity);
      for (int i = 0; i < capacity; i++) {
        this.mPool.add(newObject());
      }
    }

    abstract T newObject();

    T obtain() {
      return mPool.isEmpty() ? newObject() : mPool.removeLast();
    }

    void recycle(@NonNull T obj) {
      mPool.addLast(obj);
    }

  }

}
