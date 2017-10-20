package com.taobao.weex.devtools.trace;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.taobao.weex.WXSDKInstance;
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
import com.taobao.weex.utils.WXLogUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:
 * <p>
 * Created by rowandjj(chuyi)<br/>
 * Date: 16/10/1<br/>
 * Time: 下午3:49<br/>
 */

public class ViewUtils {

  private ViewUtils(){}
  private static final Map<Class, String> sVDomMap;

  static {
    sVDomMap = new HashMap<>();
    sVDomMap.put(WXComponent.class, "component");
    sVDomMap.put(WXText.class, WXBasicComponentType.TEXT);
    sVDomMap.put(WXVContainer.class, WXBasicComponentType.CONTAINER);
    sVDomMap.put(WXDiv.class, WXBasicComponentType.DIV);
    sVDomMap.put(WXEditText.class, WXBasicComponentType.TEXTAREA);
    sVDomMap.put(WXA.class, WXBasicComponentType.A);
    sVDomMap.put(WXInput.class, WXBasicComponentType.INPUT);
    sVDomMap.put(WXLoading.class, WXBasicComponentType.LOADING);
    sVDomMap.put(WXScroller.class, WXBasicComponentType.SCROLLER);
    sVDomMap.put(WXSwitch.class, WXBasicComponentType.SWITCH);
    sVDomMap.put(WXSlider.class, WXBasicComponentType.SLIDER);
    sVDomMap.put(WXVideo.class, WXBasicComponentType.VIDEO);
    sVDomMap.put(WXImage.class, WXBasicComponentType.IMAGE);
    sVDomMap.put(WXHeader.class, WXBasicComponentType.HEADER);
    sVDomMap.put(WXEmbed.class, WXBasicComponentType.EMBED);
    sVDomMap.put(WXListComponent.class, WXBasicComponentType.LIST);
    sVDomMap.put(HorizontalListComponent.class, WXBasicComponentType.HLIST);
    sVDomMap.put(WXCell.class, WXBasicComponentType.CELL);
  }

  @NonNull
  public static String getComponentName(@NonNull WXComponent component) {
    String name = sVDomMap.get(component.getClass());
    return TextUtils.isEmpty(name) ? "component" : name;
  }

  public static float dp2px(@NonNull Context context,int dp){
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dp,context.getResources().getDisplayMetrics());
  }

  public static float sp2px(@NonNull Context context,int sp){
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,sp,context.getResources().getDisplayMetrics());
  }

  public static float px2dp(@NonNull Context context, float px){
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
  }

  public static int getScreenHeight(@NonNull Context context) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    return metrics.heightPixels;
  }

  public static boolean isVerticalScroller(@NonNull WXScroller scroller) {
    return scroller.getDomObject() != null && scroller.getDomObject().getAttrs() != null
        && "vertical".equals(scroller.getDomObject().getAttrs().getScrollDirection());
  }

  @Nullable
  public static WXComponent getNestedRootComponent(@NonNull WXEmbed embed) {
    try {
      Class embedClazz = embed.getClass();
      Field field = embedClazz.getDeclaredField("mNestedInstance");
      field.setAccessible(true);
      WXSDKInstance nestedInstance = (WXSDKInstance) field.get(embed);
      if(nestedInstance == null) {
        return null;
      }
      return nestedInstance.getRootComponent();

    }catch (Exception e) {
      WXLogUtils.e(e.getMessage());
    }
    return null;
  }

  public static double findSuitableVal(double value,int step){
    if(value <= 0 || step <= 0){
      return 0;
    }
    int temp = (int) value;
    while (temp % step != 0){
      temp++;
    }
    return temp;
  }

  public static int generateViewId() {
    for (;;) {
      final int result = sNextGeneratedId.get();
      // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
      int newValue = result + 1;
      if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
      if (sNextGeneratedId.compareAndSet(result, newValue)) {
        return result;
      }
    }
  }

  private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
}