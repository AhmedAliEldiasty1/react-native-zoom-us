package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingShareController;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, MeetingServiceListener, InMeetingShareController.InMeetingShareListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private Boolean isInitialized = false;
  private Promise initializePromise;
  private Promise meetingPromise;

  private Boolean shouldDisablePreview = false;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }

  @ReactMethod
  public void initialize(final ReadableMap params, final ReadableMap settings, final Promise promise) {
    if (isInitialized) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }

    isInitialized = true;

    try {
      initializePromise = promise;

      if (settings.hasKey("disableShowVideoPreviewWhenJoinMeeting")) {
        shouldDisablePreview = settings.getBoolean("disableShowVideoPreviewWhenJoinMeeting");
      }

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            zoomSDK.initialize(
              reactContext.getCurrentActivity(),
              params.getString("clientKey"),
              params.getString("clientSecret"),
              params.getString("domain"),
              RNZoomUsModule.this
            );
          }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void startMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
        return;
      }

      final String meetingNo = paramMap.getString("meetingNumber");
      final MeetingService meetingService = zoomSDK.getMeetingService();
      if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
        long lMeetingNo = 0;
        try {
          lMeetingNo = Long.parseLong(meetingNo);
        } catch (NumberFormatException e) {
          promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
          return;
        }

        if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
          meetingService.returnToMeeting(reactContext.getCurrentActivity());
          promise.resolve("Already joined zoom meeting");
          return;
        }
      }

      StartMeetingOptions opts = new StartMeetingOptions();
      StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
      params.displayName = paramMap.getString("userName");
      params.meetingNo = paramMap.getString("meetingNumber");
      params.userId = paramMap.getString("userId");
      params.userType = paramMap.getInt("userType");
      params.zoomAccessToken = paramMap.getString("zoomAccessToken");

      int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

      if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      if(paramMap.hasKey("participantID")) opts.participant_id = paramMap.getString("participantID");
      if(paramMap.hasKey("noAudio")) opts.no_audio = paramMap.getBoolean("noAudio");
      if(paramMap.hasKey("noVideo")) opts.no_video = paramMap.getBoolean("noVideo");

      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = paramMap.getString("userName");
      params.meetingNo = paramMap.getString("meetingNumber");
      if(paramMap.hasKey("password")) params.password = paramMap.getString("password");

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeetingWithPassword(
    final String displayName,
    final String meetingNo,
    final String password,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = displayName;
      params.meetingNo = meetingNo;
      params.password = password;

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    sendEvent("AuthEvent", getAuthErrorName(errorCode));
    if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      initializePromise.reject(
              "ERR_ZOOM_INITIALIZATION",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");

      ZoomSDK.getInstance().getMeetingSettingsHelper().disableShowVideoPreviewWhenJoinMeeting(shouldDisablePreview);
    }
  }

  @Override
  public void onZoomAuthIdentityExpired(){
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    sendEvent("MeetingEvent", getMeetErrorName(errorCode));

    if (meetingPromise == null) {
      return;
    }

    if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      meetingPromise.reject(
              "ERR_ZOOM_MEETING",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
      meetingPromise = null;
    } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
      meetingPromise.resolve("Connected to zoom meeting");
      meetingPromise = null;
    }
  }

  private void registerListener() {
    Log.i(TAG, "registerListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();
    if(meetingService != null) {
      meetingService.addListener(this);
    }
    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
    if (inMeetingService != null) {
      InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
      if (inMeetingShareController != null) {
        inMeetingShareController.addListener(this);
      }
    }
  }

  private void unregisterListener() {
    Log.i(TAG, "unregisterListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if(zoomSDK.isInitialized()) {
      MeetingService meetingService = zoomSDK.getMeetingService();
      meetingService.removeListener(this);
      InMeetingShareController inMeetingShareController = zoomSDK.getInMeetingService().getInMeetingShareController();
      inMeetingShareController.removeListener(this);
    }
  }

  @Override
  public void onShareActiveUser(long userId) {
    if (userId == ZoomSDK.getInstance().getInMeetingService().getMyUserID()) {
      sendEvent("MeetingEvent", "screenShareStarted");
    } else {
      sendEvent("MeetingEvent", "screenShareStopped");
    }
  }

  @Override
  public void onShareUserReceivingStatus(long l) {}


  @Override
  public void onCatalystInstanceDestroy() {
    unregisterListener();
  }

  // React LifeCycle
  @Override
  public void onHostDestroy() {
    unregisterListener();
  }
  @Override
  public void onHostPause() {}
  @Override
  public void onHostResume() {}

  // React Native event emitters and event handling

  private void sendEvent(String name, String event) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private String getAuthErrorName(final int errorCode) {
    switch (errorCode) {
      case ZoomError.ZOOM_ERROR_SUCCESS: return "success";
      case ZoomError.ZOOM_ERROR_INVALID_ARGUMENTS: return "invalidArguments"; // Android only
      case ZoomError.ZOOM_ERROR_ILLEGAL_APP_KEY_OR_SECRET: return "illegalAppKeyOrSecret"; // Android only
      case ZoomError.ZOOM_ERROR_NETWORK_UNAVAILABLE: return "networkUnavailable"; // Android only
      case ZoomError.ZOOM_ERROR_AUTHRET_CLIENT_INCOMPATIBLEE: return "clientIncompatible";
      case ZoomError.ZOOM_ERROR_DEVICE_NOT_SUPPORTED: return "deviceNotSupported"; // Android only
      default: return "unknown";
    }
  }

  private String getMeetErrorName(final int errorCode) {
    switch (errorCode) {
      case MeetingError.MEETING_ERROR_SUCCESS: return "success";
      case MeetingError.MEETING_ERROR_INCORRECT_MEETING_NUMBER: return "incorrectMeetingNumber"; // Android only
      case MeetingError.MEETING_ERROR_TIMEOUT: return "timeout"; // Android only
      case MeetingError.MEETING_ERROR_NETWORK_UNAVAILABLE: return "networkUnavailable"; // Android only
      case MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE: return "meetingClientIncompatible";
      case MeetingError.MEETING_ERROR_NETWORK_ERROR: return "networkError";
      case MeetingError.MEETING_ERROR_MMR_ERROR: return "mmrError";
      case MeetingError.MEETING_ERROR_SESSION_ERROR: return "sessionError";
      case MeetingError.MEETING_ERROR_MEETING_OVER: return "meetingOver";
      case MeetingError.MEETING_ERROR_MEETING_NOT_EXIST: return "meetingNotExist";
      case MeetingError.MEETING_ERROR_USER_FULL: return "meetingUserFull";
      case MeetingError.MEETING_ERROR_NO_MMR: return "noMMR";
      case MeetingError.MEETING_ERROR_LOCKED: return "meetingLocked";
      case MeetingError.MEETING_ERROR_RESTRICTED: return "meetingRestricted";
      case MeetingError.MEETING_ERROR_RESTRICTED_JBH: return "meetingRestrictedJBH";
      case MeetingError.MEETING_ERROR_WEB_SERVICE_FAILED: return "webServiceFailed"; // Android only
      case MeetingError.MEETING_ERROR_REGISTER_WEBINAR_FULL: return "registerWebinarFull";
      case MeetingError.MEETING_ERROR_DISALLOW_HOST_RESGISTER_WEBINAR: return "registerWebinarHostRegister";
      case MeetingError.MEETING_ERROR_DISALLOW_PANELIST_REGISTER_WEBINAR: return "registerWebinarPanelistRegister";
      case MeetingError.MEETING_ERROR_HOST_DENY_EMAIL_REGISTER_WEBINAR: return "registerWebinarDeniedEmail";
      case MeetingError.MEETING_ERROR_WEBINAR_ENFORCE_LOGIN: return "registerWebinarEnforceLogin";
      case MeetingError.MEETING_ERROR_EXIT_WHEN_WAITING_HOST_START: return "exitWhenWaitingHostStart"; // Android only
      case MeetingError.MEETING_ERROR_REMOVED_BY_HOST: return "removedByHost";
      case MeetingError.MEETING_ERROR_INVALID_ARGUMENTS: return "invalidArguments";
      case MeetingError.MEETING_ERROR_INVALID_STATUS: return "invalidStatus"; // Android only
      default: return "unknown";
    }
  }
}
