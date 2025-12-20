package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.CampaignLink;
import com.openmailer.openmailer.service.campaign.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * Controller for handling email tracking.
 * Provides endpoints for tracking email opens and link clicks.
 * These endpoints do NOT require authentication as they are accessed from emails.
 */
@RestController
@RequestMapping("/track")
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);

    // 1x1 transparent GIF pixel (base64 encoded)
    private static final byte[] TRACKING_PIXEL = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
    );

    private final TrackingService trackingService;

    @Autowired
    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * Tracks email opens by serving a 1x1 transparent pixel.
     * When an email client loads the HTML, it requests this image, triggering the tracking.
     *
     * GET /track/open/{trackingId}
     *
     * @param trackingId the unique tracking ID for the recipient
     * @return 1x1 transparent GIF image
     */
    @GetMapping("/open/{trackingId}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable String trackingId) {
        log.debug("Tracking pixel requested for: {}", trackingId);

        try {
            // Record the open event
            trackingService.recordOpen(trackingId);

            // Return 1x1 transparent pixel
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_GIF);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(TRACKING_PIXEL, headers, HttpStatus.OK);

        } catch (ResourceNotFoundException e) {
            log.warn("Tracking ID not found: {}", trackingId);
            // Still return the pixel to avoid broken images in emails
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_GIF);
            return new ResponseEntity<>(TRACKING_PIXEL, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error tracking open for {}: {}", trackingId, e.getMessage());
            // Still return the pixel
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_GIF);
            return new ResponseEntity<>(TRACKING_PIXEL, headers, HttpStatus.OK);
        }
    }

    /**
     * Tracks link clicks and redirects to the original URL.
     *
     * GET /track/click/{shortCode}?tid={trackingId}
     *
     * @param shortCode the short code for the tracked link
     * @param trackingId optional tracking ID to associate click with recipient
     * @return redirect to the original URL
     */
    @GetMapping("/click/{shortCode}")
    public ResponseEntity<Void> trackClick(
            @PathVariable String shortCode,
            @RequestParam(value = "tid", required = false) String trackingId) {

        log.debug("Link click tracked - Short code: {}, Tracking ID: {}", shortCode, trackingId);

        try {
            CampaignLink link;

            if (trackingId != null && !trackingId.isEmpty()) {
                // Record click with recipient tracking
                link = trackingService.recordClick(shortCode, trackingId);
            } else {
                // Record anonymous click (no recipient tracking)
                link = trackingService.recordAnonymousClick(shortCode);
            }

            // Redirect to original URL
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, link.getOriginalUrl());

            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (ResourceNotFoundException e) {
            log.warn("Short code not found: {}", shortCode);
            // Redirect to home page or error page
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, "/");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            log.error("Error tracking click for {}: {}", shortCode, e.getMessage(), e);
            // Redirect to home page on error
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, "/");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    /**
     * Health check endpoint for tracking service.
     *
     * GET /track/health
     *
     * @return simple OK response
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Tracking service is running");
    }
}
