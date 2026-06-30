package com.milkfoam.infraredcamera.hikvision;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public interface HCNetSdkLibrary extends Library {

  int COMM_FIREDETECTION_ALARM = 0x4991;
  int COMM_THERMOMETRY_ALARM = 0x5212;
  int NET_DVR_GET_REALTIME_THERMOMETRY = 3629;
  int NET_SDK_CALLBACK_TYPE_DATA = 2;
  int NET_DVR_DEV_ADDRESS_MAX_LEN = 129;
  int NET_DVR_LOGIN_USERNAME_MAX_LEN = 64;
  int NET_DVR_LOGIN_PASSWD_MAX_LEN = 64;
  int SERIALNO_LEN = 48;
  int NAME_LEN = 32;
  int VCA_MAX_POLYGON_POINT_NUM = 10;
  int NET_SDK_MAX_FILE_PATH = 256;
  int NET_SDK_INIT_CFG_TYPE_CHECK_MODULE_COM = 0;
  int NET_SDK_INIT_CFG_SDK_PATH = 2;
  int NET_SDK_INIT_CFG_LIBEAY_PATH = 3;
  int NET_SDK_INIT_CFG_SSLEAY_PATH = 4;

  static HCNetSdkLibrary load(String libraryPath) {
    String target = (libraryPath == null || libraryPath.isBlank()) ? defaultLibraryName() : libraryPath;
    Class<? extends HCNetSdkLibrary> type = isWindows() ? Windows.class : Posix.class;
    return Native.load(target, type);
  }

  private static String defaultLibraryName() {
    return isWindows() ? "HCNetSDK" : "hcnetsdk";
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }

  interface Windows extends HCNetSdkLibrary, StdCallLibrary {
  }

  interface Posix extends HCNetSdkLibrary, Library {
  }

  boolean NET_DVR_SetSDKInitCfg(int enumType, Pointer lpInBuff);

  boolean NET_DVR_Init();

  boolean NET_DVR_SetConnectTime(int dwWaitTime, int dwTryTimes);

  boolean NET_DVR_SetReconnect(int dwInterval, boolean bEnableRecon);

  boolean NET_DVR_SetLogToFile(int nLogLevel, String strLogDir, boolean bAutoDel);

  int NET_DVR_GetLastError();

  boolean NET_DVR_Cleanup();

  int NET_DVR_Login_V40(NET_DVR_USER_LOGIN_INFO pLoginInfo, NET_DVR_DEVICEINFO_V40 lpDeviceInfo);

  boolean NET_DVR_Logout(int lUserID);

  boolean NET_DVR_STDXMLConfig(
      int lUserID,
      NET_DVR_XML_CONFIG_INPUT lpInputParam,
      NET_DVR_XML_CONFIG_OUTPUT lpOutputParam);

  boolean NET_DVR_SetDVRMessageCallBack_V50(int iIndex, FMSGCallBack_V50 fMessageCallBack, Pointer pUser);

  int NET_DVR_SetupAlarmChan_V41(int lUserID, NET_DVR_SETUPALARM_PARAM lpSetupParam);

  boolean NET_DVR_CloseAlarmChan_V30(int lAlarmHandle);

  int NET_DVR_StartRemoteConfig(
      int lUserID,
      int dwCommand,
      Pointer lpInBuffer,
      int dwInBufferLen,
      FRemoteConfigCallback cbStateCallback,
      Pointer pUserData);

  boolean NET_DVR_StopRemoteConfig(int lHandle);

  boolean NET_DVR_CaptureJPEGPicture_NEW(
      int lUserID,
      int lChannel,
      NET_DVR_JPEGPARA lpJpegPara,
      Pointer sJpegPicBuffer,
      int dwPicSize,
      int[] lpSizeReturned);

  interface FMSGCallBack_V50 extends StdCallLibrary.StdCallCallback {
    void invoke(int lCommand, NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser);
  }

  interface FRemoteConfigCallback extends StdCallLibrary.StdCallCallback {
    void invoke(int dwType, Pointer lpBuffer, int dwBufLen, Pointer pUserData);
  }

  final class NET_DVR_INIT_CHECK_MODULE_COM extends Structure {
    public byte byEnable;
    public byte[] byRes = new byte[255];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("byEnable", "byRes");
    }
  }

  final class NET_DVR_LOCAL_SDK_PATH extends Structure {
    public byte[] sPath = new byte[NET_SDK_MAX_FILE_PATH];
    public byte[] byRes = new byte[128];

    public void setPath(String path) {
      copyAscii(path, sPath);
      write();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("sPath", "byRes");
    }
  }

  final class NET_DVR_USER_LOGIN_INFO extends Structure {
    public byte[] sDeviceAddress = new byte[NET_DVR_DEV_ADDRESS_MAX_LEN];
    public byte byUseTransport;
    public short wPort;
    public byte[] sUserName = new byte[NET_DVR_LOGIN_USERNAME_MAX_LEN];
    public byte[] sPassword = new byte[NET_DVR_LOGIN_PASSWD_MAX_LEN];
    public Pointer cbLoginResult;
    public Pointer pUser;
    public boolean bUseAsynLogin;
    public byte byProxyType;
    public byte byUseUTCTime;
    public byte byLoginMode;
    public byte byHttps;
    public int iProxyID;
    public byte byVerifyMode;
    public byte[] byRes3 = new byte[119];

    public void setCredentials(String host, int port, String username, String password) {
      copyAscii(host, sDeviceAddress);
      wPort = (short) port;
      copyAscii(username, sUserName);
      copyAscii(password, sPassword);
      bUseAsynLogin = false;
      write();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "sDeviceAddress",
          "byUseTransport",
          "wPort",
          "sUserName",
          "sPassword",
          "cbLoginResult",
          "pUser",
          "bUseAsynLogin",
          "byProxyType",
          "byUseUTCTime",
          "byLoginMode",
          "byHttps",
          "iProxyID",
          "byVerifyMode",
          "byRes3");
    }
  }

  final class NET_DVR_DEVICEINFO_V40 extends Structure {
    public NET_DVR_DEVICEINFO_V30 struDeviceV30 = new NET_DVR_DEVICEINFO_V30();
    public byte bySupportLock;
    public byte byRetryLoginTime;
    public byte byPasswordLevel;
    public byte byProxyType;
    public int dwSurplusLockTime;
    public byte byCharEncodeType;
    public byte bySupportDev5;
    public byte byLoginMode;
    public int byRes3;
    public int iResidualValidity;
    public byte byResidualValidity;
    public byte bySingleStartDTalkChan;
    public byte bySingleDTalkChanNums;
    public byte byPassWordResetLevel;
    public byte bySupportStreamEncrypt;
    public byte byMarketType;
    public byte[] byRes2 = new byte[238];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "struDeviceV30",
          "bySupportLock",
          "byRetryLoginTime",
          "byPasswordLevel",
          "byProxyType",
          "dwSurplusLockTime",
          "byCharEncodeType",
          "bySupportDev5",
          "byLoginMode",
          "byRes3",
          "iResidualValidity",
          "byResidualValidity",
          "bySingleStartDTalkChan",
          "bySingleDTalkChanNums",
          "byPassWordResetLevel",
          "bySupportStreamEncrypt",
          "byMarketType",
          "byRes2");
    }
  }

  final class NET_DVR_DEVICEINFO_V30 extends Structure {
    public byte[] sSerialNumber = new byte[SERIALNO_LEN];
    public byte byAlarmInPortNum;
    public byte byAlarmOutPortNum;
    public byte byDiskNum;
    public byte byDVRType;
    public byte byChanNum;
    public byte byStartChan;
    public byte byAudioChanNum;
    public byte byIPChanNum;
    public byte byZeroChanNum;
    public byte byMainProto;
    public byte bySubProto;
    public byte bySupport;
    public byte bySupport1;
    public byte bySupport2;
    public short wDevType;
    public byte bySupport3;
    public byte byMultiStreamProto;
    public byte byStartDChan;
    public byte byStartDTalkChan;
    public byte byHighDChanNum;
    public byte bySupport4;
    public byte byLanguageType;
    public byte byVoiceInChanNum;
    public byte byStartVoiceInChanNo;
    public byte[] byRes3 = new byte[2];
    public byte byMirrorChanNum;
    public short wStartMirrorChanNo;
    public byte[] byRes2 = new byte[2];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "sSerialNumber",
          "byAlarmInPortNum",
          "byAlarmOutPortNum",
          "byDiskNum",
          "byDVRType",
          "byChanNum",
          "byStartChan",
          "byAudioChanNum",
          "byIPChanNum",
          "byZeroChanNum",
          "byMainProto",
          "bySubProto",
          "bySupport",
          "bySupport1",
          "bySupport2",
          "wDevType",
          "bySupport3",
          "byMultiStreamProto",
          "byStartDChan",
          "byStartDTalkChan",
          "byHighDChanNum",
          "bySupport4",
          "byLanguageType",
          "byVoiceInChanNum",
          "byStartVoiceInChanNo",
          "byRes3",
          "byMirrorChanNum",
          "wStartMirrorChanNo",
          "byRes2");
    }
  }

  final class NET_DVR_SETUPALARM_PARAM extends Structure {
    public int dwSize;
    public byte byLevel;
    public byte byAlarmInfoType;
    public byte byRetAlarmTypeV40;
    public byte byRetDevInfoVersion;
    public byte byRetVQDAlarmType;
    public byte byFaceAlarmDetection;
    public byte bySupport;
    public byte byBrokenNetHttp;
    public short wTaskNo;
    public byte byDeployType;
    public byte[] byRes1 = new byte[3];
    public byte byAlarmTypeURL;
    public byte byCustomCtrl;

    public NET_DVR_SETUPALARM_PARAM() {
      dwSize = size();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "byLevel",
          "byAlarmInfoType",
          "byRetAlarmTypeV40",
          "byRetDevInfoVersion",
          "byRetVQDAlarmType",
          "byFaceAlarmDetection",
          "bySupport",
          "byBrokenNetHttp",
          "wTaskNo",
          "byDeployType",
          "byRes1",
          "byAlarmTypeURL",
          "byCustomCtrl");
    }
  }

  final class NET_DVR_ALARMER extends Structure {
    public byte byUserIDValid;
    public byte bySerialValid;
    public byte byVersionValid;
    public byte byDeviceNameValid;
    public byte byMacAddrValid;
    public byte byLinkPortValid;
    public byte byDeviceIPValid;
    public byte bySocketIPValid;
    public int lUserID;
    public byte[] sSerialNumber = new byte[SERIALNO_LEN];
    public int dwDeviceVersion;
    public byte[] sDeviceName = new byte[32];
    public byte[] byMacAddr = new byte[6];
    public short wLinkPort;
    public byte[] sDeviceIP = new byte[128];
    public byte[] sSocketIP = new byte[128];
    public byte byIpProtocol;
    public byte[] byRes1 = new byte[2];
    public byte bJSONBroken;
    public short wSocketPort;
    public byte[] byRes2 = new byte[6];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "byUserIDValid",
          "bySerialValid",
          "byVersionValid",
          "byDeviceNameValid",
          "byMacAddrValid",
          "byLinkPortValid",
          "byDeviceIPValid",
          "bySocketIPValid",
          "lUserID",
          "sSerialNumber",
          "dwDeviceVersion",
          "sDeviceName",
          "byMacAddr",
          "wLinkPort",
          "sDeviceIP",
          "sSocketIP",
          "byIpProtocol",
          "byRes1",
          "bJSONBroken",
          "wSocketPort",
          "byRes2");
    }
  }

  final class NET_DVR_XML_CONFIG_INPUT extends Structure {
    public int dwSize;
    public Pointer lpRequestUrl;
    public int dwRequestUrlLen;
    public Pointer lpInBuffer;
    public int dwInBufferSize;
    public int dwRecvTimeOut;
    public byte byForceEncrpt;
    public byte[] byRes = new byte[31];

    public NET_DVR_XML_CONFIG_INPUT() {
      dwSize = size();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "lpRequestUrl",
          "dwRequestUrlLen",
          "lpInBuffer",
          "dwInBufferSize",
          "dwRecvTimeOut",
          "byForceEncrpt",
          "byRes");
    }
  }

  final class NET_DVR_XML_CONFIG_OUTPUT extends Structure {
    public int dwSize;
    public Pointer lpOutBuffer;
    public int dwOutBufferSize;
    public int dwReturnedXMLSize;
    public Pointer lpStatusBuffer;
    public int dwStatusSize;
    public Pointer lpDataBuffer;
    public byte byNumOfMultiPart;
    public byte[] byRes = new byte[23];

    public NET_DVR_XML_CONFIG_OUTPUT() {
      dwSize = size();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "lpOutBuffer",
          "dwOutBufferSize",
          "dwReturnedXMLSize",
          "lpStatusBuffer",
          "dwStatusSize",
          "lpDataBuffer",
          "byNumOfMultiPart",
          "byRes");
    }
  }

  final class NET_DVR_IPADDR extends Structure {
    public byte[] sIpV4 = new byte[16];
    public byte[] sIpV6 = new byte[128];

    public String ipv4() {
      return readCString(sIpV4);
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("sIpV4", "sIpV6");
    }
  }

  final class NET_VCA_DEV_INFO extends Structure {
    public NET_DVR_IPADDR struDevIP = new NET_DVR_IPADDR();
    public short wPort;
    public byte byChannel;
    public byte byIvmsChannel;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("struDevIP", "wPort", "byChannel", "byIvmsChannel");
    }
  }

  final class NET_VCA_RECT extends Structure {
    public float fX;
    public float fY;
    public float fWidth;
    public float fHeight;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("fX", "fY", "fWidth", "fHeight");
    }
  }

  final class NET_VCA_POINT extends Structure {
    public float fX;
    public float fY;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("fX", "fY");
    }
  }

  final class NET_PTZ_INFO extends Structure {
    public float fPan;
    public float fTilt;
    public float fZoom;
    public int dwFocus;
    public byte[] byRes = new byte[4];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("fPan", "fTilt", "fZoom", "dwFocus", "byRes");
    }
  }

  final class NET_VCA_POLYGON extends Structure {
    public int dwPointNum;
    public NET_VCA_POINT[] struPos = new NET_VCA_POINT[VCA_MAX_POLYGON_POINT_NUM];

    public NET_VCA_POLYGON() {
      for (int i = 0; i < struPos.length; i++) {
        struPos[i] = new NET_VCA_POINT();
      }
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("dwPointNum", "struPos");
    }
  }

  final class NET_DVR_POINT_THERM_CFG extends Structure {
    public float fTemperature;
    public NET_VCA_POINT struPoint = new NET_VCA_POINT();
    public byte[] byRes = new byte[120];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("fTemperature", "struPoint", "byRes");
    }
  }

  final class NET_DVR_LINEPOLYGON_THERM_CFG extends Structure {
    public float fMaxTemperature;
    public float fMinTemperature;
    public float fAverageTemperature;
    public float fTemperatureDiff;
    public NET_VCA_POLYGON struRegion = new NET_VCA_POLYGON();
    public byte[] byRes = new byte[32];

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "fMaxTemperature",
          "fMinTemperature",
          "fAverageTemperature",
          "fTemperatureDiff",
          "struRegion",
          "byRes");
    }
  }

  final class NET_DVR_JPEGPARA extends Structure {
    public short wPicSize;
    public short wPicQuality;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("wPicSize", "wPicQuality");
    }
  }

  final class NET_DVR_REALTIME_THERMOMETRY_COND extends Structure {
    public int dwSize;
    public int dwChan;
    public byte byRuleID;
    public byte byMode;
    public short wInterval;
    public float fTemperatureDiff;
    public byte[] byRes = new byte[56];

    public NET_DVR_REALTIME_THERMOMETRY_COND() {
      dwSize = size();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("dwSize", "dwChan", "byRuleID", "byMode", "wInterval", "fTemperatureDiff", "byRes");
    }
  }

  final class NET_DVR_THERMOMETRY_ALARM extends Structure {
    public int dwSize;
    public int dwChannel;
    public byte byRuleID;
    public byte byThermometryUnit;
    public short wPresetNo;
    public NET_PTZ_INFO struPtzInfo = new NET_PTZ_INFO();
    public byte byAlarmLevel;
    public byte byAlarmType;
    public byte byAlarmRule;
    public byte byRuleCalibType;
    public NET_VCA_POINT struPoint = new NET_VCA_POINT();
    public NET_VCA_POLYGON struRegion = new NET_VCA_POLYGON();
    public float fRuleTemperature;
    public float fCurrTemperature;
    public int dwPicLen;
    public int dwThermalPicLen;
    public int dwThermalInfoLen;
    public Pointer pPicBuff;
    public Pointer pThermalPicBuff;
    public Pointer pThermalInfoBuff;
    public NET_VCA_POINT struHighestPoint = new NET_VCA_POINT();
    public byte fToleranceTemperature;
    public byte dwAlertFilteringTime;
    public byte dwAlarmFilteringTime;
    public byte[] byRes = new byte[48];

    public NET_DVR_THERMOMETRY_ALARM() {
      dwSize = size();
    }

    public NET_DVR_THERMOMETRY_ALARM(Pointer pointer) {
      super(pointer);
      read();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "dwChannel",
          "byRuleID",
          "byThermometryUnit",
          "wPresetNo",
          "struPtzInfo",
          "byAlarmLevel",
          "byAlarmType",
          "byAlarmRule",
          "byRuleCalibType",
          "struPoint",
          "struRegion",
          "fRuleTemperature",
          "fCurrTemperature",
          "dwPicLen",
          "dwThermalPicLen",
          "dwThermalInfoLen",
          "pPicBuff",
          "pThermalPicBuff",
          "pThermalInfoBuff",
          "struHighestPoint",
          "fToleranceTemperature",
          "dwAlertFilteringTime",
          "dwAlarmFilteringTime",
          "byRes");
    }
  }

  final class NET_DVR_THERMOMETRY_UPLOAD extends Structure {
    public int dwSize;
    public int dwRelativeTime;
    public int dwAbsTime;
    public byte[] szRuleName = new byte[NAME_LEN];
    public byte byRuleID;
    public byte byRuleCalibType;
    public short wPresetNo;
    public NET_DVR_POINT_THERM_CFG struPointThermCfg = new NET_DVR_POINT_THERM_CFG();
    public NET_DVR_LINEPOLYGON_THERM_CFG struLinePolygonThermCfg = new NET_DVR_LINEPOLYGON_THERM_CFG();
    public byte byThermometryUnit;
    public byte byDataType;
    public byte byRes1;
    public byte bySpecialPointThermType;
    public float fCenterPointTemperature;
    public float fHighestPointTemperature;
    public float fLowestPointTemperature;
    public NET_VCA_POINT struHighestPoint = new NET_VCA_POINT();
    public NET_VCA_POINT struLowestPoint = new NET_VCA_POINT();
    public byte byIsFreezedata;
    public byte byFaceSnapThermometryEnabled;
    public byte[] byRes2 = new byte[2];
    public int dwChan;
    public NET_VCA_RECT struFaceRect = new NET_VCA_RECT();
    public int dwTimestamp;
    public byte[] byRes = new byte[68];

    public NET_DVR_THERMOMETRY_UPLOAD() {
    }

    public NET_DVR_THERMOMETRY_UPLOAD(Pointer pointer) {
      super(pointer);
      read();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "dwRelativeTime",
          "dwAbsTime",
          "szRuleName",
          "byRuleID",
          "byRuleCalibType",
          "wPresetNo",
          "struPointThermCfg",
          "struLinePolygonThermCfg",
          "byThermometryUnit",
          "byDataType",
          "byRes1",
          "bySpecialPointThermType",
          "fCenterPointTemperature",
          "fHighestPointTemperature",
          "fLowestPointTemperature",
          "struHighestPoint",
          "struLowestPoint",
          "byIsFreezedata",
          "byFaceSnapThermometryEnabled",
          "byRes2",
          "dwChan",
          "struFaceRect",
          "dwTimestamp",
          "byRes");
    }
  }

  final class NET_DVR_FIREDETECTION_ALARM extends Structure {
    public int dwSize;
    public int dwRelativeTime;
    public int dwAbsTime;
    public NET_VCA_DEV_INFO struDevInfo = new NET_VCA_DEV_INFO();
    public short wPanPos;
    public short wTiltPos;
    public short wZoomPos;
    public byte byPicTransType;
    public byte byRes1;
    public int dwPicDataLen;
    public Pointer pBuffer;
    public NET_VCA_RECT struRect = new NET_VCA_RECT();
    public NET_VCA_POINT struPoint = new NET_VCA_POINT();
    public short wFireMaxTemperature;
    public short wTargetDistance;
    public byte byStrategyType;
    public byte byAlarmSubType;
    public byte byPTZPosExEnable;
    public byte byRes2;
    public NET_PTZ_INFO struPtzPosEx = new NET_PTZ_INFO();
    public int dwVisiblePicLen;
    public Pointer pVisiblePicBuf;
    public Pointer pSmokeBuf;
    public short wDevInfoIvmsChannelEx;
    public byte byRes3;
    public byte byFireScanWaitMode;
    public int dwVisibleChannel;
    public byte byTimeDiffFlag;
    public byte cTimeDifferenceH;
    public byte cTimeDifferenceM;
    public byte[] byRes = new byte[49];

    public NET_DVR_FIREDETECTION_ALARM() {
      dwSize = size();
    }

    public NET_DVR_FIREDETECTION_ALARM(Pointer pointer) {
      super(pointer);
      read();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "dwSize",
          "dwRelativeTime",
          "dwAbsTime",
          "struDevInfo",
          "wPanPos",
          "wTiltPos",
          "wZoomPos",
          "byPicTransType",
          "byRes1",
          "dwPicDataLen",
          "pBuffer",
          "struRect",
          "struPoint",
          "wFireMaxTemperature",
          "wTargetDistance",
          "byStrategyType",
          "byAlarmSubType",
          "byPTZPosExEnable",
          "byRes2",
          "struPtzPosEx",
          "dwVisiblePicLen",
          "pVisiblePicBuf",
          "pSmokeBuf",
          "wDevInfoIvmsChannelEx",
          "byRes3",
          "byFireScanWaitMode",
          "dwVisibleChannel",
          "byTimeDiffFlag",
          "cTimeDifferenceH",
          "cTimeDifferenceM",
          "byRes");
    }
  }

  private static void copyAscii(String value, byte[] target) {
    Arrays.fill(target, (byte) 0);
    byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(bytes, 0, target, 0, Math.min(bytes.length, target.length - 1));
  }

  private static String readCString(byte[] bytes) {
    int length = 0;
    while (length < bytes.length && bytes[length] != 0) {
      length++;
    }
    return new String(bytes, 0, length, StandardCharsets.US_ASCII);
  }
}
