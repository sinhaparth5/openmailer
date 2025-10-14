<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\EmailEvent;
use App\Models\EmailLink;
use App\Models\EmailQueue;
use Illuminate\Http\Request;

class TrackingController extends Controller{
    public function trackOpen(Request $request, string $trackingId)
    {
        $emailQueue = EmailQueue::where('tracking_id', $trackingId)->first();

        if ($emailQueue) {
            // Check if already tracked
            $alreadyTracked = EmailEvent::where('tracking_id', $trackingId)
                ->where('event_type', 'opened')
                ->exists();

            if (!$alreadyTracked) {
                // Create event
                EmailEvent::create([
                    'campaign_id' => $emailQueue->campaign_id,
                    'contact_id' => $emailQueue->contact_id,
                    'tracking_id' => $trackingId,
                    'event_type' => 'opened',
                    'user_agent' => $request->userAgent(),
                    'ip_address' => $request->ip(),
                ]);

                // Update campaign stats
                $emailQueue->campaign->increment('total_opens');
            }
        }

        // Return 1x1 transparent pixel
        $pixel = base64_decode('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
        return response($pixel)->header('Content-Type', 'image/gif');
    }

    public function trackClick(Request $request, string $shortCode)
    {
        $trackingId = $request->query('tid');

        $link = EmailLink::where('short_code', $shortCode)->first();

        if (!$link) {
            abort(404);
        }

        if ($trackingId) {
            $emailQueue = EmailQueue::where('tracking_id', $trackingId)->first();

            if ($emailQueue) {
                // Create click event
                EmailEvent::create([
                    'campaign_id' => $emailQueue->campaign_id,
                    'contact_id' => $emailQueue->contact_id,
                    'tracking_id' => $trackingId,
                    'event_type' => 'clicked',
                    'link_url' => $link->original_url,
                    'user_agent' => $request->userAgent(),
                    'ip_address' => $request->ip(),
                ]);

                // Update stats
                $link->increment('total_clicks');

                // Check if unique click
                $uniqueClick = !EmailEvent::where('tracking_id', $trackingId)
                    ->where('event_type', 'clicked')
                    ->where('link_url', $link->original_url)
                    ->exists();

                if ($uniqueClick) {
                    $link->increment('unique_clicks');
                }

                $emailQueue->campaign->increment('total_clicks');
            }
        }

        // Redirect to original URL
        return redirect($link->original_url);
    }

    public function unsubscribe(Request $request, string $token)
    {
        $unsubscribeToken = \App\Models\UnsubscribeToken::where('token', $token)
            ->where('used', false)
            ->first();

        if (!$unsubscribeToken || $unsubscribeToken->expires_at < now()) {
            return view('unsubscribe.expired');
        }

        if ($request->isMethod('post')) {
            $contact = $unsubscribeToken->contact;

            // Update contact status
            $contact->update([
                'status' => 'unsubscribed',
                'unsubscribed_at' => now(),
            ]);

            // Add to suppression list
            \App\Models\SuppressionList::firstOrCreate([
                'user_id' => $contact->user_id,
                'email' => $contact->email,
            ], [
                'reason' => 'unsubscribed',
                'source' => 'email_link',
            ]);

            // Create unsubscribe event
            if ($unsubscribeToken->campaign_id) {
                EmailEvent::create([
                    'campaign_id' => $unsubscribeToken->campaign_id,
                    'contact_id' => $contact->id,
                    'tracking_id' => '',
                    'event_type' => 'unsubscribed',
                    'ip_address' => $request->ip(),
                ]);
            }

            // Mark token as used
            $unsubscribeToken->update(['used' => true]);

            return view('unsubscribe.success');
        }

        return view('unsubscribe.confirm', [
            'token' => $token,
            'contact' => $unsubscribeToken->contact
        ]);
    }
}
