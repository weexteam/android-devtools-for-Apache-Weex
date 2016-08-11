package com.taobao.weex.devtools.inspector.elements.android;

import android.view.ViewGroup;

import com.taobao.weex.devtools.common.Accumulator;
import com.taobao.weex.devtools.inspector.elements.AbstractChainedDescriptor;
import com.taobao.weex.devtools.inspector.elements.AttributeAccumulator;
import com.taobao.weex.devtools.inspector.elements.StyleAccumulator;
import com.taobao.weex.devtools.inspector.elements.W3CStyleConstants;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXVContainer;

/**
 * Created by budao on 16/8/4.
 */
public class WXVContainerDescriptor extends AbstractChainedDescriptor<WXVContainer> {

    @Override
    protected void onGetChildren(WXVContainer element, Accumulator<Object> children) {
        for (int i = 0; i < element.getChildCount(); i++) {
            WXComponent component = element.getChild(i);
            if (component != null) {
                children.store(component);
            }
        }
    }

    @Override
    protected void onGetStyles(WXVContainer element, StyleAccumulator accumulator) {
        ViewGroup view = element.getRealView();
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams != null) {
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) layoutParams;
                    if (margins.leftMargin != 0
                            || margins.rightMargin != 0
                            || margins.topMargin != 0
                            || margins.bottomMargin != 0) {
                        accumulator.store(W3CStyleConstants.MARGIN_LEFT, String.valueOf(margins.leftMargin), false);
                        accumulator.store(W3CStyleConstants.MARGIN_TOP, String.valueOf(margins.topMargin), false);
                        accumulator.store(W3CStyleConstants.MARGIN_RIGHT, String.valueOf(margins.rightMargin), false);
                        accumulator.store(W3CStyleConstants.MARGIN_BOTTOM, String.valueOf(margins.bottomMargin), false);
                    }

                }

            }
        }

    }

//    @Override
//    protected void onGetAttributes(WXVContainer element, AttributeAccumulator attributes) {
//        ViewGroup view = element.getRealView();
//        if (view != null) {
//            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
//            if (layoutParams != null) {
//                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
//                    ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) layoutParams;
//                    if (margins.leftMargin != 0
//                            || margins.rightMargin != 0
//                            || margins.topMargin != 0
//                            || margins.bottomMargin != 0) {
//                        attributes.store(W3CStyleConstants.MARGIN_LEFT, String.valueOf(margins.leftMargin));
//                        attributes.store(W3CStyleConstants.MARGIN_TOP, String.valueOf(margins.topMargin));
//                        attributes.store(W3CStyleConstants.MARGIN_RIGHT, String.valueOf(margins.rightMargin));
//                        attributes.store(W3CStyleConstants.MARGIN_BOTTOM, String.valueOf(margins.bottomMargin));
//                    }
//
//                }
//            }
//        }
//    }
}
