package com.taobao.weex.devtools.inspector.elements.android;

import android.text.TextUtils;
import android.view.View;

import com.taobao.weex.devtools.common.Accumulator;
import com.taobao.weex.devtools.common.StringUtil;
import com.taobao.weex.devtools.inspector.elements.AbstractChainedDescriptor;
import com.taobao.weex.devtools.inspector.elements.AttributeAccumulator;
import com.taobao.weex.devtools.inspector.elements.StyleAccumulator;
import com.taobao.weex.devtools.inspector.elements.W3CStyleConstants;
import com.taobao.weex.dom.ImmutableDomObject;
import com.taobao.weex.ui.component.WXA;
import com.taobao.weex.ui.component.WXBasicComponentType;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXDiv;
import com.taobao.weex.ui.component.WXEmbed;
import com.taobao.weex.ui.component.WXHeader;
import com.taobao.weex.ui.component.WXImage;
import com.taobao.weex.ui.component.WXInput;
import com.taobao.weex.ui.component.WXLoading;
import com.taobao.weex.ui.component.WXScroller;
import com.taobao.weex.ui.component.WXSlider;
import com.taobao.weex.ui.component.WXSwitch;
import com.taobao.weex.ui.component.WXText;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.component.WXVideo;
import com.taobao.weex.ui.component.list.HorizontalListComponent;
import com.taobao.weex.ui.component.list.WXCell;
import com.taobao.weex.ui.component.list.WXListComponent;
import com.taobao.weex.ui.view.WXEditText;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by budao on 16/8/4.
 */
public class WXComponentDescriptor extends AbstractChainedDescriptor<WXComponent> implements
    HighlightableDescriptor {

  private static HashMap<Class, String> sClassName = new HashMap<Class, String>();

  static {
    sClassName.put(WXComponent.class, "component");
    // sClassName.put(WXBorder.class, "border");
    sClassName.put(WXText.class, WXBasicComponentType.TEXT);
    sClassName.put(WXVContainer.class, WXBasicComponentType.CONTAINER);
    sClassName.put(WXDiv.class, WXBasicComponentType.DIV);
    sClassName.put(WXEditText.class, WXBasicComponentType.TEXTAREA);
    sClassName.put(WXA.class, WXBasicComponentType.A);
    sClassName.put(WXInput.class, WXBasicComponentType.INPUT);
    sClassName.put(WXLoading.class, WXBasicComponentType.LOADING);
    sClassName.put(WXScroller.class, WXBasicComponentType.SCROLLER);
    sClassName.put(WXSwitch.class, WXBasicComponentType.SWITCH);
    sClassName.put(WXSlider.class, WXBasicComponentType.SLIDER);
    sClassName.put(WXVideo.class, WXBasicComponentType.VIDEO);
    sClassName.put(WXImage.class, WXBasicComponentType.IMAGE);
    sClassName.put(WXHeader.class, WXBasicComponentType.HEADER);
    sClassName.put(WXEmbed.class, WXBasicComponentType.EMBED);
    sClassName.put(WXListComponent.class, WXBasicComponentType.LIST);
    sClassName.put(HorizontalListComponent.class, WXBasicComponentType.HLIST);
    sClassName.put(WXCell.class, WXBasicComponentType.CELL);
  }

  @Override
  protected String onGetNodeName(WXComponent element) {
    Class clazz = element.getClass();
    String name = sClassName.get(clazz);
    if (TextUtils.isEmpty(name)) {
      name = StringUtil.removePrefix(clazz.getSimpleName(), "WX");
    }
    return name;
  }

  @Override
  protected void onGetChildren(WXComponent element, Accumulator<Object> children) {
//        View view = element.getRealView();
//        if (view != null) {
//            children.store(view);
//        }
  }

  @Override
  @Nullable
  public View getViewForHighlighting(Object element) {
    return ((WXComponent) element).getRealView();
  }

  public HashMap<String, String> getStyles(WXComponent element) {
    HashMap<String, String> map = null;
    ImmutableDomObject domObject = element.getDomObject();
    if (domObject != null && domObject.getStyles() != null) {
      map = new HashMap<>();
      for (Map.Entry<String, Object> entry : domObject.getStyles().entrySet()) {
        if (entry.getValue() != null) {
          map.put(entry.getKey(), entry.getValue().toString());
        }

      }
    }
    return map;
  }

  public HashMap<String, String> getAttribute(WXComponent element) {
    HashMap<String, String> map = null;
    ImmutableDomObject domObject = element.getDomObject();
    if (domObject != null && domObject.getAttrs() != null) {
      map = new HashMap<>();
      for (Map.Entry<String, Object> entry : domObject.getAttrs().entrySet()) {
        if (entry.getValue() != null) {
          map.put(entry.getKey(), entry.getValue().toString());
        }

      }
    }
    return map;
  }

  private static boolean filter(String key) {
    boolean result = false;
    if (key != null) {
      String exp = key.toLowerCase();
      result = !exp.contains("padding") && !exp.contains("margin")
          && !exp.contains("width") && !exp.contains("height")
          && !exp.contains("left")
          && !exp.contains("right")
          && !exp.contains("top")
          && !exp.contains("bottom");
    }
    return result;
  }

  @Override
  protected void onGetStyles(WXComponent element, StyleAccumulator accumulator) {
    HashMap<String, String> map = getStyles(element);
    if (map != null && map.size() > 0) {
      for (HashMap.Entry<String, String> entry : map.entrySet()) {
        accumulator.store(W3CStyleConstants.V_PREFIX + entry.getKey(), entry.getValue(), false);
        if (filter(entry.getKey())) {
          accumulator.store(entry.getKey(), entry.getValue(), false);
        }
      }
    }

    View view = element.getRealView();
    if (view != null) {
      accumulator.store(W3CStyleConstants.LEFT, String.valueOf(view.getLeft()), false);
      accumulator.store(W3CStyleConstants.TOP, String.valueOf(view.getTop()), false);
      accumulator.store(W3CStyleConstants.RIGHT, String.valueOf(view.getRight()), false);
      accumulator.store(W3CStyleConstants.BOTTOM, String.valueOf(view.getBottom()), false);
      accumulator.store(W3CStyleConstants.WIDTH, String.valueOf(view.getWidth()), false);
      accumulator.store(W3CStyleConstants.HEIGHT, String.valueOf(view.getHeight()), false);


//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.LEFT,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getLeft())), false);
//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.TOP,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getTop())), false);
//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.RIGHT,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getRight())), false);
//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.BOTTOM,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getBottom())), false);
//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.WIDTH,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getWidth())), false);
//            accumulator.store(W3CStyleConstants.V_PREFIX + W3CStyleConstants.HEIGHT,
//                    String.valueOf(WXViewUtils.getWeexPxByReal(view.getHeight())), false);

      if (view.getPaddingTop() != 0
          || view.getPaddingBottom() != 0
          || view.getPaddingLeft() != 0
          || view.getPaddingRight() != 0) {
        accumulator.store(W3CStyleConstants.PADDING_LEFT, String.valueOf(view.getPaddingLeft()), false);
        accumulator.store(W3CStyleConstants.PADDING_TOP, String.valueOf(view.getPaddingTop()), false);
        accumulator.store(W3CStyleConstants.PADDING_RIGHT, String.valueOf(view.getPaddingRight()), false);
        accumulator.store(W3CStyleConstants.PADDING_BOTTOM, String.valueOf(view.getPaddingBottom()), false);

//                accumulator.store(W3CStyleConstants.PADDING_LEFT, String.valueOf(
//                        WXViewUtils.getWeexPxByReal(view.getPaddingLeft())), false);
//                accumulator.store(W3CStyleConstants.PADDING_TOP, String.valueOf(
//                        WXViewUtils.getWeexPxByReal(view.getPaddingTop())), false);
//                accumulator.store(W3CStyleConstants.PADDING_RIGHT, String.valueOf(
//                        WXViewUtils.getWeexPxByReal(view.getPaddingRight())), false);
//                accumulator.store(W3CStyleConstants.PADDING_BOTTOM, String.valueOf(
//                        WXViewUtils.getWeexPxByReal(view.getPaddingBottom())), false);
      }

      accumulator.store(W3CStyleConstants.VISIBILITY, String.valueOf(view.isShown()), false);
    }
  }

  @Override
  protected void onGetAttributes(WXComponent element, AttributeAccumulator attributes) {
    HashMap<String, String> map = getAttribute(element);
    if (map != null && map.size() > 0) {
      for (HashMap.Entry<String, String> entry : map.entrySet()) {
        attributes.store(entry.getKey(), entry.getValue());
      }
    }
    View view = element.getRealView();
    if (view != null) {
      if (!view.isShown()) {
        attributes.store(W3CStyleConstants.VISIBILITY, String.valueOf(view.isShown()));
      }
    }
  }

}
