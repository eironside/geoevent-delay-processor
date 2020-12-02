package com.esri.geoevent.processor.delay_processor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.Point;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;

public class DelayedGeoEvent implements Delayed
{
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(DelayProcessor.class);

  private final GeoEvent            geoEvent;
  private final long                time;
  private final boolean             useTrackID;
  private final String              timeKey;
  private final String              locationKey;
  private final String              locationKeyValue;

  public DelayedGeoEvent(GeoEvent geoEvent, long delayTimeMs, String delayTimeField, boolean useTrackID)
  {
    this.geoEvent = geoEvent;
    this.useTrackID = useTrackID;
    String definitionGuid = geoEvent.getGeoEventDefinition().getGuid();
    long keyTime = geoEvent.getReceivedTime().getTime();

    if (DelayProperties.TIME_END.equals(delayTimeField))
      keyTime = geoEvent.getEndTime().getTime();
    else if (DelayProperties.TIME_START.equals(delayTimeField))
      keyTime = geoEvent.getStartTime().getTime();

    this.time = keyTime + delayTimeMs;

    // timeKey starts with time to preserve queue time order
    if (useTrackID)
      this.timeKey = keyTime + "_" + definitionGuid + "_" + geoEvent.getTrackId();
    else
      this.timeKey = keyTime + "_" + definitionGuid;

    if (geoEvent.getGeometry() != null && geoEvent.getGeometry().getGeometry() != null && geoEvent.getGeometry().getGeometry().getType().equals(Type.Point))
    {
      // only point geometries are supported
      Point geometry = (Point) geoEvent.getGeometry().getGeometry();
      this.locationKey = definitionGuid + "_" + geoEvent.getTrackId();
      this.locationKeyValue = geometry.getX() + "_" + geometry.getY() + "_" + definitionGuid + "_" + geoEvent.getTrackId();

      // else
      // {
      // Point point = ((Polygon) geometry.getBoundary()).getPoint(0);
      // this.locationKey = point.getX() + "_" + point.getY() + "_" + geoEvent.getTrackId();
      // }
    }
    else
    {
      this.locationKey = null;
      this.locationKeyValue = null;
    }

    LOGGER.trace("Created delayed event with timeKey {0} using {1} field time {2}. Location key is {5}. Delay time is {3} ms so release time is {4}.", timeKey, delayTimeField, keyTime, delayTimeMs, time, locationKey);
  }

  public String getTimeKey()
  {
    return this.timeKey;
  }

  public String getLocationKey()
  {
    return this.locationKey;
  }

  public String getLocationKeyValue()
  {
    return this.locationKeyValue;
  }

  @Override
  public long getDelay(TimeUnit unit)
  {
    long diff = this.time - System.currentTimeMillis();
    return unit.convert(diff, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed obj)
  {
    int result = Long.compare(this.time, ((DelayedGeoEvent) obj).time);
    if (useTrackID)
    {
      result = this.timeKey.compareTo(((DelayedGeoEvent) obj).timeKey);
    }

    return result;
  }

  public GeoEvent getGeoEvent()
  {
    return this.geoEvent;
  }
}
