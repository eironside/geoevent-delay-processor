package com.esri.geoevent.processor.delay_processor;

public interface DelayProperties
{
  static final String STRINGS_PATH            = "${com.esri.geoevent.processor.delay_processor.";

  static final String DELAY_VALUE             = "delayValue";
  static final String DELAY_VALUE_UNITS       = "delayValueUnits";
  static final String DELAY_FIELD             = "delayField";
  static final String ENFORCE_DELAY_THRESHOLD = "enforceDelayThreshold";
  static final String ALLOW_DUPLICATES        = "allowDuplicates";
  static final String USE_LOCATION            = "useLocation";
  static final String USE_TRACK_ID            = "useTrackID";
  static final String CLEAR_CACHE             = "clearCache";
  static final String TIME_START              = "TIME_START";
  static final String TIME_END                = "TIME_END";
  static final String RECEIVED_TIME           = "RECEIVED_TIME";
}
