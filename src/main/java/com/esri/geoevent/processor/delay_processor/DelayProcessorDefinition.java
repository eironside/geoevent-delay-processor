package com.esri.geoevent.processor.delay_processor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.esri.ges.core.property.LabeledValue;
import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.processor.GeoEventProcessorDefinitionBase;

public class DelayProcessorDefinition extends GeoEventProcessorDefinitionBase implements DelayProperties
{
  private static final BundleLogger LOGGER                     = BundleLoggerFactory.getLogger(DelayProcessorDefinition.class);

  private static final String       PROCESSOR_LABEL            = STRINGS_PATH + "PROCESSOR_LABEL}";
  private static final String       PROCESSOR_DESC             = STRINGS_PATH + "PROCESSOR_DESC}";

  private static final String       CLEAR_CACHE_DESC           = STRINGS_PATH + "CLEAR_CACHE_DESC}";
  private static final String       CLEAR_CACHE_LABEL          = STRINGS_PATH + "CLEAR_CACHE_LABEL}";
  private static final String       USE_LOCATION_DESC          = STRINGS_PATH + "USE_LOCATION_DESC}";
  private static final String       USE_LOCATION_LABEL         = STRINGS_PATH + "USE_LOCATION_LABEL}";
  private static final String       USE_TRACKID_DESC           = STRINGS_PATH + "USE_TRACKID_DESC}";
  private static final String       USE_TRACKID_LABEL          = STRINGS_PATH + "USE_TRACKID_LABEL}";
  private static final String       ALLOW_DUPLICATES_DESC      = STRINGS_PATH + "ALLOW_DUPLICATES_DESC}";
  private static final String       ALLOW_DUPLICATES_LABEL     = STRINGS_PATH + "ALLOW_DUPLICATES_LABEL}";
  private static final String       ENFORCE_DELAY_THRESH_DESC  = STRINGS_PATH + "ENFORCE_DELAY_THRESH_DESC}";
  private static final String       ENFORCE_DELAY_THRESH_LABEL = STRINGS_PATH + "ENFORCE_DELAY_THRESH_LABEL}";
  private static final String       DELAY_TIME_FIELD_DESC      = STRINGS_PATH + "DELAY_TIME_FIELD_DESC}";
  private static final String       DELAY_TIME_FIELD_LABEL     = STRINGS_PATH + "DELAY_TIME_FIELD_LABEL}";
  private static final String       DELAY_TIME_UNIT_DESC       = STRINGS_PATH + "DELAY_TIME_UNIT_DESC}";
  private static final String       DELAY_TIME_UNIT_LABEL      = STRINGS_PATH + "DELAY_TIME_UNIT_LABEL}";
  private static final String       DELAY_TIME_DESC            = STRINGS_PATH + "DELAY_TIME_DESC}";
  private static final String       DELAY_TIME_LABEL           = STRINGS_PATH + "DELAY_TIME_LABEL}";

  public DelayProcessorDefinition()
  {
    LOGGER.info("Creating definition for delay processor");

    final List<LabeledValue> allowedTimeUnits = new ArrayList<>();
    for (TimeUnit value : TimeUnit.values())
    {
      allowedTimeUnits.add(new LabeledValue(value.name(), value.name()));
    }

    final List<LabeledValue> allowedTimeFields = new ArrayList<>();
    allowedTimeFields.add(new LabeledValue(TIME_START, TIME_START));
    allowedTimeFields.add(new LabeledValue(TIME_END, TIME_END));
    allowedTimeFields.add(new LabeledValue(RECEIVED_TIME, RECEIVED_TIME));

    try
    {
      propertyDefinitions.put(DELAY_VALUE, new PropertyDefinition(DELAY_VALUE, PropertyType.Long, "0", DELAY_TIME_LABEL, DELAY_TIME_DESC, true, false));

      propertyDefinitions.put(DELAY_VALUE_UNITS, new PropertyDefinition(DELAY_VALUE_UNITS, PropertyType.String, TimeUnit.SECONDS.name(), DELAY_TIME_UNIT_LABEL, DELAY_TIME_UNIT_DESC, true, false, allowedTimeUnits));

      propertyDefinitions.put(DELAY_FIELD, new PropertyDefinition(DELAY_FIELD, PropertyType.String, RECEIVED_TIME, DELAY_TIME_FIELD_LABEL, DELAY_TIME_FIELD_DESC, true, false, allowedTimeFields));

      propertyDefinitions.put(ENFORCE_DELAY_THRESHOLD, new PropertyDefinition(ENFORCE_DELAY_THRESHOLD, PropertyType.Boolean, false, ENFORCE_DELAY_THRESH_LABEL, ENFORCE_DELAY_THRESH_DESC, true, false));

      propertyDefinitions.put(ALLOW_DUPLICATES, new PropertyDefinition(ALLOW_DUPLICATES, PropertyType.Boolean, true, ALLOW_DUPLICATES_LABEL, ALLOW_DUPLICATES_DESC, true, false));

      propertyDefinitions.put(USE_TRACK_ID, new PropertyDefinition(USE_TRACK_ID, PropertyType.Boolean, true, USE_TRACKID_LABEL, USE_TRACKID_DESC, ALLOW_DUPLICATES + "=false", true, false));

      propertyDefinitions.put(USE_LOCATION, new PropertyDefinition(USE_LOCATION, PropertyType.Boolean, false, USE_LOCATION_LABEL, USE_LOCATION_DESC, ALLOW_DUPLICATES + "=false", true, false));

      propertyDefinitions.put(CLEAR_CACHE, new PropertyDefinition(CLEAR_CACHE, PropertyType.Boolean, false, CLEAR_CACHE_LABEL, CLEAR_CACHE_DESC, true, false));
    }
    catch (Exception e)
    {
      LOGGER.warn("Failed to construct definition.", e);
    }
  }

  @Override
  public String getDescription()
  {
    return PROCESSOR_DESC;
  }

  @Override
  public String getLabel()
  {
    return PROCESSOR_LABEL;
  }
}
