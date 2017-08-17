package com.taobao.weex.devtools.toolbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.PopupWindow;
import android.widget.TextView;


/**
 * Created by moxun on 16/6/20.
 */
public class PopupManager {
  private static PopupWindow popupWindow;
  private static int mCurrentX = 0, mCurrentY = 0, mStatusBarHeight = 0;
  private static boolean hasMoved = false;

  public static void show(final Context context, View.OnClickListener onClickListener) {
    TextView textView = new TextView(context);
    textView.setText("DevTool");
    textView.setBackgroundColor(Color.parseColor("#1E90FF"));
    textView.setTextColor(Color.WHITE);
    textView.setPadding(10, 10, 10, 10);

    if (context instanceof Activity) {
      mStatusBarHeight = getStatusBarHeight(context);
      if (mCurrentY == 0) {
        mCurrentY = mStatusBarHeight;
      }
    }

    textView.setOnTouchListener(new View.OnTouchListener() {
      private float mDx, mDy, downX, downY;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          mDx = mCurrentX - event.getRawX();
          mDy = mCurrentY - event.getRawY();
          downX = event.getX();
          downY = event.getY();
          hasMoved = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
          mCurrentX = (int) (event.getRawX() + mDx);
          mCurrentY = (int) (event.getRawY() + mDy);
          if (isValidMove(context, event.getX() - downX) || isValidMove(context, event.getY() - downY)) {
            if (mCurrentY < mStatusBarHeight) {
              mCurrentY = mStatusBarHeight;
            }
            popupWindow.update(mCurrentX, mCurrentY, -1, -1);
            hasMoved = true;
          }
        }
        return false;
      }
    });

    textView.setOnClickListener(onClickListener);

    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    textView.setLayoutParams(layoutParams);
    popupWindow = new PopupWindow(textView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    popupWindow.showAtLocation(((Activity) context).getWindow().findViewById(Window.ID_ANDROID_CONTENT)
        , Gravity.TOP | Gravity.LEFT, mCurrentX, mCurrentY);
  }

  private static boolean isValidMove(Context context, float distance) {
    return hasMoved || ViewConfiguration.get(context).getScaledTouchSlop() < Math.abs(distance);
  }

  public static void dismiss() {
    if (popupWindow != null) {
      popupWindow.dismiss();
    }
  }

  public static int getStatusBarHeight(Context host) {
    if (!(host instanceof Activity)) {
      return 0;
    } else {
      Rect frame = new Rect();
      ((Activity) host).getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
      return frame.top;
    }
  }
}
