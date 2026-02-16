package com.notifications;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.reactive.messaging.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

import com.notifications.avro.EnrichedNotification;

@ApplicationScoped
public class NotificationListener {

    private static final Logger LOG = Logger.getLogger(NotificationListener.class);

    @Incoming("enriched-notifications")
    public void consume(EnrichedNotification notification) {
        LOG.info("🔔 ¡NUEVA NOTIFICACIÓN ENRIQUECIDA RECIBIDA!");
        LOG.info(notification.toString());
    }
}
