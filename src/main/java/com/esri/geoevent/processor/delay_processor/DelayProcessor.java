package com.esri.geoevent.processor.delay_processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventPropertyName;
import com.esri.ges.core.property.Property;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.messaging.EventDestination;
import com.esri.ges.messaging.EventUpdatable;
import com.esri.ges.messaging.GeoEventProducer;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.processor.GeoEventProcessorBase;
import com.esri.ges.processor.GeoEventProcessorDefinition;

/**
 * The DelayProcessor class processes incoming GeoEvents and delaying their execution by a specified time. Delayed
 * events are placed in a private queue and processed based on a combination of the delay time and the time they were
 * received by GeoEvent Server (the RECEIVED_TIME property).
 */
public class DelayProcessor extends GeoEventProcessorBase implements GeoEventProducer, EventUpdatable, DelayProperties
{
  private static final BundleLogger            LOGGER                 = BundleLoggerFactory.getLogger(DelayProcessor.class);
  private static final int                     MAX_ENTRIES            = 20000;

  private ExecutorService                      executorService        = Executors.newSingleThreadExecutor();
  private final BlockingQueue<DelayedGeoEvent> geoEventQueue          = new DelayQueue<DelayedGeoEvent>();

  private final Map<String, String>            geoEventTimeKeySet     = Collections.synchronizedMap(new LinkedHashMap<String, String>()
                                                                        {
                                                                          private static final long serialVersionUID = 3497816525702669924L;

                                                                          protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
                                                                          {
                                                                            return size() > MAX_ENTRIES;
                                                                          }
                                                                        });

  private final Map<String, String>            geoEventLocationKeySet = Collections.synchronizedMap(new LinkedHashMap<String, String>()
                                                                        {
                                                                          private static final long serialVersionUID = 3497816525702669925L;

                                                                          protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
                                                                          {
                                                                            return size() > MAX_ENTRIES;
                                                                          }
                                                                        });

  private final Messaging                      messaging;
  private GeoEventProducer                     geoEventProducer;
  private long                                 delayValue;
  private TimeUnit                             delayValueUnit;
  private long                                 delayMilliseconds      = 0;

  private String                               delayField             = RECEIVED_TIME;
  private boolean                              enforceDelayThreshold  = false;
  private boolean                              allowDuplicates        = true;
  private boolean                              useTrackID             = false;
  private boolean                              useLocation            = false;

  private Future<?>                            eventLoop;

  public DelayProcessor(GeoEventProcessorDefinition definition, Messaging messaging) throws ComponentException
  {
    super(definition);
    this.messaging = messaging;
  }

  @Override
  public void setId(String id)
  {
    super.setId(id);
    LOGGER.trace("Set delay processor ID: {0}", id);
    geoEventProducer = messaging.createGeoEventProducer(new EventDestination(id + ":event"));
  }

  @Override
  public GeoEvent process(GeoEvent geoEvent) throws Exception
  {
    LOGGER.trace("Processing event with Delay Field={0}, Allow Duplicates={1}, Use TRACK_ID={2}, Use Location={3}, Enforece Delay Threshold={4}, Delay(ms)={5}: {6}", delayField, allowDuplicates, useTrackID, useLocation, enforceDelayThreshold, delayMilliseconds, geoEvent);

    if (geoEvent != null)
    {
      final DelayedGeoEvent delayedGeoEvent = new DelayedGeoEvent(geoEvent, delayMilliseconds, delayField, useTrackID);
      long eventDelay = delayedGeoEvent.getDelay(TimeUnit.SECONDS);
      if (enforceDelayThreshold && eventDelay <= 0)
      {
        LOGGER.debug("Discarding expired GeoEvent (delay of {1} is already expired): {0}", geoEvent, eventDelay);
      }
      else
      {
        String timeKey = delayedGeoEvent.getTimeKey();
        String locationKey = delayedGeoEvent.getLocationKey();
        String locationKeyValue = delayedGeoEvent.getLocationKeyValue();

        if (LOGGER.isTraceEnabled())
        {
          LOGGER.trace("Allow Duplicates is {2} Use TRACK_ID is {3}. Existing time key {0} = {1} and existing location key {4} ( {6} ) = {5} (non-null value for either means we've seen this point before)", timeKey, this.geoEventTimeKeySet.get(timeKey), allowDuplicates, useTrackID, locationKey, this.geoEventLocationKeySet.get(locationKey), locationKeyValue);
        }

        if (allowDuplicates || ((this.geoEventTimeKeySet.put(timeKey, timeKey) == null) && (locationKey == null || this.geoEventLocationKeySet.put(locationKey, locationKeyValue) != locationKeyValue)))
        {
          this.geoEventQueue.put(delayedGeoEvent);
          if (LOGGER.isDebugEnabled())
            LOGGER.debug("Added GeoEvent with key {3} to Delay Queue with delay {1} ms and queue size {2}: {0}", geoEvent, delayMilliseconds, this.geoEventQueue.size(), delayedGeoEvent.getTimeKey());
        }
        else if (LOGGER.isDebugEnabled())
        {
          LOGGER.debug("Discarding duplicate GeoEvent with key {1}: {0}", geoEvent, delayedGeoEvent.getTimeKey());
        }
      }
    }
    return null;
  }

  @Override
  public void onServiceStart()
  {
    super.onServiceStart();
    LOGGER.trace("Enter OnServiceStart() of delay processor.");

    try
    {
      if (executorService == null || executorService.isShutdown() || executorService.isTerminated())
      {
        executorService = Executors.newSingleThreadExecutor();
      }
      if (eventLoop != null)
      {
        this.eventLoop.cancel(true);
        LOGGER.trace("Previous Event Loop Canceled");
      }
      this.eventLoop = executorService.submit(constructEventLoop(this.geoEventQueue));
      LOGGER.trace("Event Loop Started");
    }
    catch (Exception ex)
    {
      LOGGER.error("Failed to start processor.", ex);
    }
    LOGGER.trace("Exit OnServiceStart() of delay processor.");
  }

  @Override
  public void onServiceStop()
  {
    LOGGER.trace("Enter OnServiceStop() of delay processor.");
    try
    {
      if (eventLoop != null)
      {
        this.eventLoop.cancel(true);
      }

      this.geoEventQueue.clear(); // TODO: Make this configurable
      LOGGER.debug("Cleared GeoEvent Queue");
    }
    catch (Exception ex)
    {
      LOGGER.error("Failed to stop processor.", ex);
    }
    super.onServiceStop();
    LOGGER.trace("Exit OnServiceStop() of delay processor.");
  }

  protected Runnable constructEventLoop(final BlockingQueue<DelayedGeoEvent> inboundQueue)
  {
    LOGGER.trace("Enter constructEventLoop() of delay processor.");
    return () ->
      {
        try
        {
          while (!Thread.currentThread().isInterrupted())
          {
            if (LOGGER.isTraceEnabled())
            {
              final DelayedGeoEvent peek = inboundQueue.peek();
              if (peek != null)
              {
                LOGGER.trace("Time to next event {0} ms: {1}", peek.getTimeKey(), peek.getDelay(TimeUnit.MILLISECONDS));
              }
            }

            final DelayedGeoEvent take = inboundQueue.take();
            final GeoEvent geoEvent = take.getGeoEvent();

            if (LOGGER.isTraceEnabled())
            {
              final DelayedGeoEvent peek = inboundQueue.peek();
              LOGGER.trace("Sending Delayed GeoEvent with key {3} from queue with size {1}. Time to next event {2} ms: {0}", geoEvent, inboundQueue.size(), peek == null ? 0 : peek.getDelay(TimeUnit.MILLISECONDS), take.getTimeKey());
            }

            try
            {
              send(geoEvent);
            }
            catch (MessagingException e)
            {
              LOGGER.error("Error sending geoevent: {0}", e, geoEvent);
            }
          }
        }
        catch (InterruptedException ie)
        {
          // Not usually an error, this is expected to happen
          LOGGER.trace("Event consumer loop has stopped.");
        }
        catch (Exception e)
        {
          // This is unexpected
          LOGGER.trace("Event consumer loop has stopped due to unknown error:", e);
        }
      };
  }

  @Override
  public void afterPropertiesSet()
  {
    LOGGER.trace("Enter afterPropertiesSet() of delay processor.");
    try
    {
      delayValue = Long.parseLong(this.properties.get(DELAY_VALUE).getValueAsString());

      delayValueUnit = TimeUnit.valueOf(this.properties.get(DELAY_VALUE_UNITS).getValueAsString());

      delayField = this.properties.get(DELAY_FIELD).getValueAsString();

      enforceDelayThreshold = Boolean.parseBoolean(this.properties.get(ENFORCE_DELAY_THRESHOLD).getValueAsString());

      allowDuplicates = Boolean.parseBoolean(this.properties.get(ALLOW_DUPLICATES).getValueAsString());

      useTrackID = Boolean.parseBoolean(this.properties.get(USE_TRACK_ID).getValueAsString());

      useLocation = Boolean.parseBoolean(this.properties.get(USE_LOCATION).getValueAsString());

      Property clearCacheProperty = this.properties.get(CLEAR_CACHE);
      boolean clearCache = Boolean.parseBoolean(clearCacheProperty.getValueAsString());

      if (LOGGER.isTraceEnabled())
      {
        LOGGER.trace("Delay Value: {0} {1}", delayValue, delayValueUnit);
        LOGGER.trace("Delay Field: {0}", delayField);
        LOGGER.trace("Enforce Delay Threshold: {0}", enforceDelayThreshold);
        LOGGER.trace("Allow Duplicates: {0}", allowDuplicates);
        LOGGER.trace("Use TRACK_ID: {0}", useTrackID);
        LOGGER.trace("Use Location: {0}", useLocation);
        LOGGER.trace("Clear Cache: {0}", clearCache);
      }
    }
    catch (Exception ex)
    {
      LOGGER.error("Failed to get processor properties.", ex);
    }
    delayMilliseconds = delayValueUnit.toMillis(delayValue);
  }

  @Override
  public void send(GeoEvent geoEvent) throws MessagingException
  {
    LOGGER.trace("Enter send() of delay processor.");
    if (geoEventProducer != null && geoEvent != null)
    {
      geoEvent.setProperty(GeoEventPropertyName.TYPE, "event");
      geoEvent.setProperty(GeoEventPropertyName.OWNER_ID, getId());
      geoEvent.setProperty(GeoEventPropertyName.OWNER_URI, definition.getUri());
      geoEventProducer.send(geoEvent);
      LOGGER.debug("Sent GeoEvent to consumers: {0}", geoEvent);
    }
  }

  // public void setMessaging(Messaging messaging)
  // {
  // LOGGER.trace("Enter setMessaging() of delay processor.");
  // this.messaging = messaging;
  // }

  @Override
  public String getStatusDetails()
  {
    LOGGER.trace("Enter getStatusDetails() of delay processor.");
    return (geoEventProducer != null) ? geoEventProducer.getStatusDetails() : "";
  }

  @Override
  public boolean isGeoEventMutator()
  {
    LOGGER.trace("Enter isGeoEventMutator() of delay processor.");
    return true;
  }

  @Override
  public EventDestination getEventDestination()
  {
    LOGGER.trace("Enter getEventDestination() of delay processor.");
    return (geoEventProducer != null) ? geoEventProducer.getEventDestination() : null;
  }

  @Override
  public List<EventDestination> getEventDestinations()
  {
    LOGGER.trace("Enter getEventDestinations() of delay processor.");
    return (geoEventProducer != null) ? Collections.singletonList(geoEventProducer.getEventDestination()) : new ArrayList<>();
  }

  @Override
  public void disconnect()
  {
    LOGGER.trace("Enter disconnect() of delay processor.");
    if (geoEventProducer != null)
      geoEventProducer.disconnect();
  }

  @Override
  public boolean isConnected()
  {
    LOGGER.trace("Enter isConnected() of delay processor.");
    return (geoEventProducer != null) && geoEventProducer.isConnected();
  }

  @Override
  public void setup() throws MessagingException
  {
    ;
  }

  @Override
  public void init() throws MessagingException
  {
    ;
  }

  @Override
  public void update(Observable o, Object arg)
  {
    ;
  }

  @Override
  public void validate() throws ValidationException
  {
    LOGGER.trace("Enter validate() of delay processor.");
    Property clearCacheProperty = this.properties.get(CLEAR_CACHE);
    boolean clearCache = Boolean.parseBoolean(clearCacheProperty.getValueAsString());

    if (clearCache)
    {
      geoEventTimeKeySet.clear();
      geoEventLocationKeySet.clear();
      LOGGER.debug("Clear cache requested, cleared key caches.");
    }
    getProperty(CLEAR_CACHE).setValue(false);

    clearCacheProperty = this.properties.get(CLEAR_CACHE);
    clearCache = Boolean.parseBoolean(clearCacheProperty.getValueAsString());

    LOGGER.trace("Clear Cache: {0}", clearCache);

    super.validate();
  }
}
