package ch.milosz.reactnative;

import android.util.Log;
import android.content.Context;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.MobileRTCVideoUnitRenderInfo;
import us.zoom.sdk.MobileRTCVideoUnitAspectMode;
import us.zoom.sdk.MobileRTCVideoView;
import us.zoom.sdk.MobileRTCVideoViewManager;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.InMeetingService;

import java.util.Collections;
import java.util.List;

// TODO: Solve SurfaceView blank problem.

class RNZoomUsVideoView extends MobileRTCVideoView {

  private final static String TAG = "RNZoomUs";

  private ReadableArray currentLayout = null;

  public RNZoomUsVideoView(Context context) {
    super(context);
  }

  public void setZoomLayout(ReadableArray layout) {
    currentLayout = layout;
    update();
  }

  private List<Long> getUserIdList() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return Collections.emptyList();
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService == null) {
      return Collections.emptyList();
    }

    return inMeetingService.getInMeetingUserList();
  }

  public void update() {
    if (currentLayout == null) {
      return;
    }
    MobileRTCVideoViewManager manager = getVideoViewManager();
    if (manager == null) {
      Log.e(TAG, "The video view is not initialized complately");
      return;
    }
    manager.removeAllVideoUnits();
    List<Long> users = getUserIdList();
    for (int i = 0; i < currentLayout.size(); i++) {
      ReadableMap unit = currentLayout.getMap(i);
      String kind = unit.getString("kind");
      int x = unit.getInt("x");
      int y = unit.getInt("y");
      int width = unit.getInt("width");
      int height = unit.getInt("height");
      boolean border = unit.getBoolean("border");
      boolean showUsername = unit.getBoolean("showUsername");
      boolean showAudioOff = unit.getBoolean("showAudioOff");
      int userIndex = unit.hasKey("userIndex") ? unit.getInt("userIndex") : 0;
      int background = unit.getInt("background");
      MobileRTCVideoUnitRenderInfo renderInfo = new MobileRTCVideoUnitRenderInfo(x, y, width, height);
      if (border) {
        renderInfo.is_border_visible = border;
      }
      if (showUsername) {
        renderInfo.is_username_visible = showUsername;
      }
      if (showAudioOff) {
        renderInfo.is_show_audio_off = showAudioOff;
      }
      renderInfo.backgroud_color = background;
      switch (kind) {
        case "active":
          manager.addActiveVideoUnit(renderInfo);
          break;
        case "preview":
          manager.addPreviewVideoUnit(renderInfo);
          break;
        case "share":
          if (userIndex >= users.size()) {
            break;
          }
          manager.addShareVideoUnit(users.get(userIndex), renderInfo);
          break;
        case "attendee":
          if (userIndex >= users.size()) {
            break;
          }
          manager.addAttendeeVideoUnit(users.get(userIndex), renderInfo);
          break;
      }
    }
  }

}
