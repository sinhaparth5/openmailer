package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ResourceNotFoundException;
import com.openmailer.openmailer.model.CampaignLink;
import com.openmailer.openmailer.service.campaign.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    private static final byte[] TRACKING_PIXEL = {
        71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, 0, 0, 0,
        -1, -1, -1, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0,
        1, 0, 1, 0, 0, 2, 1, 68, 0, 59
    };

    @Mock
    private TrackingService trackingService;

    private TrackingController controller;

    @BeforeEach
    void setUp() {
        controller = new TrackingController(trackingService);
    }

    @Test
    void trackOpenReturnsPixelAndNoCacheHeaders() {
        ResponseEntity<byte[]> response = controller.trackOpen("tracking-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_GIF, response.getHeaders().getContentType());
        assertEquals("no-cache, no-store, must-revalidate", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", response.getHeaders().getFirst(HttpHeaders.EXPIRES));
        assertArrayEquals(TRACKING_PIXEL, response.getBody());
        verify(trackingService).recordOpen("tracking-1");
    }

    @Test
    void trackOpenReturnsPixelWhenTrackingIdIsMissing() {
        doThrow(new ResourceNotFoundException("Tracking id not found"))
            .when(trackingService).recordOpen("missing");

        ResponseEntity<byte[]> response = controller.trackOpen("missing");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_GIF, response.getHeaders().getContentType());
        assertArrayEquals(TRACKING_PIXEL, response.getBody());
    }

    @Test
    void trackClickRedirectsToOriginalUrlWhenTrackingIdPresent() {
        CampaignLink link = new CampaignLink();
        link.setOriginalUrl("https://example.com/landing");
        when(trackingService.recordClick("abc123", "tracking-1")).thenReturn(link);

        ResponseEntity<Void> response = controller.trackClick("abc123", "tracking-1");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("https://example.com/landing", response.getHeaders().getFirst(HttpHeaders.LOCATION));
        verify(trackingService).recordClick("abc123", "tracking-1");
    }

    @Test
    void trackClickFallsBackToAnonymousRedirectWhenTrackingIdMissing() {
        CampaignLink link = new CampaignLink();
        link.setOriginalUrl("https://example.com/newsletter");
        when(trackingService.recordAnonymousClick("shorty")).thenReturn(link);

        ResponseEntity<Void> response = controller.trackClick("shorty", "");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("https://example.com/newsletter", response.getHeaders().getFirst(HttpHeaders.LOCATION));
        verify(trackingService).recordAnonymousClick("shorty");
    }

    @Test
    void trackClickRedirectsHomeWhenShortCodeIsMissing() {
        doThrow(new ResourceNotFoundException("Link not found"))
            .when(trackingService).recordAnonymousClick("missing");

        ResponseEntity<Void> response = controller.trackClick("missing", null);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("/", response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    void healthReturnsOkMessage() {
        ResponseEntity<String> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Tracking service is running", response.getBody());
    }
}
