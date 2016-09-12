package com.taobao.weex.devtools.inspector.protocol.module;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.taobao.weex.devtools.inspector.elements.android.ActivityTracker;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcResult;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsMethod;
import com.taobao.weex.devtools.inspector.screencast.ScreencastDispatcher;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.devtools.json.annotation.JsonProperty;

import org.json.JSONObject;

public class Input implements ChromeDevtoolsDomain {
  private static final String KEY_UP = "keyUp";
  private static final String MOUSE_BUTTON_LEFT = "left";
  private static final String MOUSE_BUTTON_RIGHT = "right";
  private static final String MOUSE_MOVED = "mouseMoved";
  private static final String MOUSE_WHEEL = "mouseWheel";
  private static final String MOUSE_PRESSED = "mousePressed";
  private static final String MOUSE_RELEASED = "mouseReleased";

  private ObjectMapper mObjectMapper;
  private long downTime;
  private Point lastPoint = new Point();

  public Input() {
    mObjectMapper = new ObjectMapper();
  }

  private static int getStatusBarHeight(Context host) {
    if (!(host instanceof Activity)) {
      return 0;
    } else {
      Rect frame = new Rect();
      ((Activity) host).getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
      return frame.top;
    }
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult dispatchKeyEvent(JsonRpcPeer peer, JSONObject params) {
    final DispatchKeyEventRequest request = mObjectMapper.convertValue(params, DispatchKeyEventRequest.class);
    if (KEY_UP.equals(request.type)) {
      Instrumentation instrumentation = new Instrumentation();
      if (request.nativeVirtualKeyCode >= 48 && request.nativeVirtualKeyCode <= 57) {
        int code = request.nativeVirtualKeyCode - 48 + KeyEvent.KEYCODE_0;
        instrumentation.sendKeyDownUpSync(code);
        return null;
      } else if (request.nativeVirtualKeyCode >= 65 && request.nativeVirtualKeyCode <= 90) {
        // a-z
        int code = request.nativeVirtualKeyCode - 65 + KeyEvent.KEYCODE_A;
        instrumentation.sendKeyDownUpSync(code);
        return null;
      } else if (request.nativeVirtualKeyCode == 8) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
        return null;
      } else if (request.nativeVirtualKeyCode == 13) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
        return null;
      } else if (request.nativeVirtualKeyCode == 16) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_SHIFT_LEFT);
        return null;
      } else if (request.nativeVirtualKeyCode == 20) {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_CAPS_LOCK);
        return null;
      }
      int ascii = request.nativeVirtualKeyCode;
      char c = (char) ascii;
      instrumentation.sendCharacterSync(c);
    }
    return null;
  }

  @ChromeDevtoolsMethod
  public JsonRpcResult emulateTouchFromMouseEvent(JsonRpcPeer peer, JSONObject params) {
    final EmulateTouchFromMouseEventRequest request = mObjectMapper.convertValue(
        params, EmulateTouchFromMouseEventRequest.class);
    try {
      if (MOUSE_PRESSED.equals(request.type) && MOUSE_BUTTON_LEFT.equals(request.button)) {
        lastPoint.x = request.x;
        lastPoint.y = request.y;
        down(request.x, request.y);
      } else if (MOUSE_RELEASED.equals(request.type)) {
        if (MOUSE_BUTTON_LEFT.equals(request.button)) {
          lastPoint.x = request.x;
          lastPoint.y = request.y;
          up(request.x, request.y);
        } else if (MOUSE_BUTTON_RIGHT.equals(request.button)) {
          if (ActivityTracker.get().getActivitiesView().size() > 1) {
            Instrumentation instrumentation = new Instrumentation();
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
          }
        }
      } else if (MOUSE_MOVED.equals(request.type)) {
        move(request.x, request.y);
      } else if (MOUSE_WHEEL.equals(request.type)) {
//        int distanceX = (int) (request.deltaX / 120 * 2);
//        int distanceY = (int) (request.deltaY / 120 * 2);
//        int moveX = lastPoint.x + distanceX;
//        int moveY = lastPoint.y + distanceY;
//        Point now = new Point(moveX, moveY);
//        scroll(lastPoint, now);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  private synchronized void scroll(Point last, Point now) {
    down(last.x, last.y);
    move(now.x, now.y);
    up(now.x, now.y);
    lastPoint.x = now.x;
    lastPoint.y = now.y;
  }

  private void move(int x, int y) {
    final Activity activity = ActivityTracker.get().tryGetTopActivity();
    if (activity != null) {
      final MotionEvent move = MotionEvent.obtain(
          downTime, System.currentTimeMillis(),
          MotionEvent.ACTION_MOVE,
          x / ScreencastDispatcher.getsBitmapScale(),
          y / ScreencastDispatcher.getsBitmapScale() + getStatusBarHeight(activity), 0);
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.dispatchTouchEvent(move);
        }
      });
      move.recycle();
    }
  }

  private void down(int x, int y) {
    final Activity activity = ActivityTracker.get().tryGetTopActivity();
    if (activity != null) {
      downTime = System.currentTimeMillis();
      final MotionEvent down = MotionEvent.obtain(
          downTime, downTime,
          MotionEvent.ACTION_DOWN,
          x / ScreencastDispatcher.getsBitmapScale(),
          y / ScreencastDispatcher.getsBitmapScale() + getStatusBarHeight(activity), 0);
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.dispatchTouchEvent(down);
        }
      });
      down.recycle();
    }
  }

  private void up(int x, int y) {
    final Activity activity = ActivityTracker.get().tryGetTopActivity();
    if (activity != null) {
      final MotionEvent up = MotionEvent.obtain(
          downTime, System.currentTimeMillis(),
          MotionEvent.ACTION_UP,
          x / ScreencastDispatcher.getsBitmapScale(),
          y / ScreencastDispatcher.getsBitmapScale() + getStatusBarHeight(activity), 0);
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.dispatchTouchEvent(up);
        }
      });
      up.recycle();
    }
  }

  public static class EmulateTouchFromMouseEventRequest {
    @JsonProperty(required = true)
    public String type;

    @JsonProperty(required = true)
    public int x;

    @JsonProperty(required = true)
    public int y;

    @JsonProperty(required = true)
    public double timestamp;

    @JsonProperty(required = true)
    public String button;

    @JsonProperty
    public Double deltaX;

    @JsonProperty
    public Double deltaY;

    @JsonProperty
    public Integer modifiers;

    @JsonProperty
    public Integer clickCount;
  }

  public static class DispatchKeyEventRequest {
    @JsonProperty(required = true)
    public String type;

    @JsonProperty
    public Integer modifiers;

    @JsonProperty
    public Double timestamp;

    @JsonProperty
    public String text;

    @JsonProperty
    public String unmodifiedText;

    @JsonProperty
    public String keyIdentifier;

    @JsonProperty
    public String code;

    @JsonProperty
    public String key;

    @JsonProperty
    public Integer windowsVirtualKeyCode;

    @JsonProperty
    public Integer nativeVirtualKeyCode;

    @JsonProperty
    public Boolean autoRepeat;

    @JsonProperty
    public Boolean isKeypad;

    @JsonProperty
    public Boolean isSystemKey;
  }
}
