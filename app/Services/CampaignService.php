<?php

namespace App\Services;

use App\Jobs\SendCampaignEmail;
use App\Models\Campaign;
use App\Models\Contact;
use App\Models\EmailQueue;
use App\Models\SuppressionList;
use Illuminate\Support\Str;

class CampaignService
{
    public function createCampaign(array $data): Campaign
    {
        return Campaign::create([
            'user_id' => $data['user_id'],
            'domain_id' => $data['domain_id'],
            'template_id' => $data['template_id'],
            'name' => $data['name'],
            'subject' => $data['subject'],
            'from_name' => $data['from_name'],
            'from_email' => $data['from_email'],
            'reply_to' => $data['reply_to'],
            'html_content' => $data['html_content'],
            'status' => 'draft',
        ]);
    }

    public function addRecipients(Campaign $campaign, array $contactListIds): void
    {
        $contacts = Contact::whereHas('contactLists', function ($query) use ($contactListIds) {
            $query->whereIn('contact_list_id', $contactListIds);
        })
            ->where('user_id', $campaign->user_id)
            ->where('status', 'subscribed')
            ->get();

        $suppressedEmails = SuppressionList::where('user_id', $campaign->user_id)
            ->pluck('email')
            ->toArray();

        foreach ($contacts as $contact) {
            if (! in_array($contact->email, $suppressedEmails)) {
                $campaign->contacts()->syncWithoutDetaching([
                    $contact->id => ['contact_list_id' => $contactListIds[0]],
                ]);
            }
        }

        // Update total recipients
        $campaign->update([
            'total_recipients' => $campaign->contacts()->count(),
        ]);
    }

    public function scheduleCampaign(Campaign $campaign, ?\DateTime $scheduledAt = null): void
    {
        if ($scheduledAt) {
            $campaign->update([
                'status' => 'scheduled',
                'scheduled_at' => $scheduledAt,
            ]);
        } else {
            $this->sendCampaign($campaign);
        }
    }

    public function sendCampaign(Campaign $campaign): void
    {
        $campaign->update([
            'status' => 'sending',
            'started_at' => now(),
        ]);

        // Get all recipients
        $recipients = $campaign->contacts;

        foreach ($recipients as $contact) {
            $emailQueue = EmailQueue::create([
                'campaign_id' => $campaign->id,
                'contact_id' => $contact->id,
                'email' => $contact->email,
                'status' => 'pending',
                'tracking_id' => Str::uuid(),
            ]);

            // Dispatch job to queue
            SendCampaignEmail::dispatch($emailQueue)->onQueue('emails');
        }
    }

    public function pauseCampaign(Campaign $campaign): void
    {
        $campaign->update(['status' => 'paused']);
    }

    public function resumeCampaign(Campaign $campaign): void
    {
        $campaign->update(['status' => 'sending']);

        // Get pending emails
        $pendingEmails = EmailQueue::where('campaign_id', $campaign->id)
            ->where('status', 'pending')
            ->get();

        foreach ($pendingEmails as $emailQueue) {
            SendCampaignEmail::dispatch($emailQueue)->onQueue('emails');
        }
    }

    public function getCampaignStats(Campaign $campaign): array
    {
        return [
            'total_recipients' => $campaign->total_recipients,
            'total_sent' => $campaign->total_sent,
            'total_delivered' => $campaign->total_delivered,
            'total_opens' => $campaign->total_opens,
            'total_clicks' => $campaign->total_clicks,
            'total_bounces' => $campaign->total_bounces,
            'total_complaints' => $campaign->total_complaints,
            'open_rate' => $campaign->total_sent > 0 ? round($campaign->total_opens / $campaign->total_sent * 100, 2) : 0,
            'click_rate' => $campaign->total_sent > 0 ? round($campaign->total_clicks / $campaign->total_sent * 100, 2) : 0,
            'bounce_rate' => $campaign->total_sent > 0 ? round($campaign->total_bounces / $campaign->total_sent * 100, 2) : 0,
        ];
    }
}
