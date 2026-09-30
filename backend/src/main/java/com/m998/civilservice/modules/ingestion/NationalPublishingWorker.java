package com.m998.civilservice.modules.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NationalPublishingWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(NationalPublishingWorker.class);
    private final NationalIngestionService service;

    public NationalPublishingWorker(NationalIngestionService service) { this.service = service; }

    @Async
    public void publishAsync(long runId) {
        try {
            service.publishQueued(runId);
        } catch (Exception e) {
            LOGGER.warn("Publication of ingestion run {} failed: {}", runId, e.toString());
            service.publicationFailed(runId, e.getMessage());
        }
    }
}
