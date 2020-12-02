package com.esri.geoevent.processor.delay_processor;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.messaging.Messaging;
import com.esri.ges.processor.GeoEventProcessor;
import com.esri.ges.processor.GeoEventProcessorServiceBase;

public class DelayProcessorService extends GeoEventProcessorServiceBase
{
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(DelayProcessorService.class);

  private Messaging                 messaging;

  public DelayProcessorService()
  {
    LOGGER.trace("Creating Delay Processor Service");
    this.definition = new DelayProcessorDefinition();
    LOGGER.trace("Set Delay Processor definition on service");
  }

  @Override
  public GeoEventProcessor create() throws ComponentException
  {
    DelayProcessor delayProcessor = new DelayProcessor(definition, messaging);
    return delayProcessor;
  }

  public void setMessaging(Messaging messaging)
  {
    this.messaging = messaging;
  }
}
