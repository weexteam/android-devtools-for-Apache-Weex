package com.taobao.weex.devtools.inspector.elements.android;

import android.view.View;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.devtools.common.Accumulator;
import com.taobao.weex.devtools.inspector.elements.AbstractChainedDescriptor;
import com.taobao.weex.devtools.inspector.elements.AttributeAccumulator;
import com.taobao.weex.devtools.inspector.elements.StyleAccumulator;
import com.taobao.weex.ui.component.WXComponent;

import javax.annotation.Nullable;

/**
 * Created by budao on 16/8/4.
 */
public class WXSDKInstanceDescriptor extends AbstractChainedDescriptor<WXSDKInstance> implements
        HighlightableDescriptor {

    private static final String ID_NAME = "id";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    @Override
    protected String onGetNodeName(WXSDKInstance element) {
        return "instance";
    }

    @Override
    protected void onGetChildren(WXSDKInstance element, Accumulator<Object> children) {
        WXComponent component = element.getRootComponent();
        if (component != null) {
            children.store(component);
        }
    }

    @Override
    @Nullable
    public View getViewForHighlighting(Object element) {
        return ((WXSDKInstance) element).getRootView();
    }

    @Override
    protected void onGetStyles(WXSDKInstance element, StyleAccumulator accumulator) {
        accumulator.store(ID_NAME, element.getInstanceId(), true);
        accumulator.store(WIDTH, String.valueOf(element.getWeexWidth()), false);
        accumulator.store(HEIGHT, String.valueOf(element.getWeexHeight()), false);
    }

    @Override
    protected void onGetAttributes(WXSDKInstance element, AttributeAccumulator attributes) {
        attributes.store(ID_NAME, element.getInstanceId());
        attributes.store(WIDTH, String.valueOf(element.getWeexWidth()));
        attributes.store(HEIGHT, String.valueOf(element.getWeexHeight()));
    }
}
