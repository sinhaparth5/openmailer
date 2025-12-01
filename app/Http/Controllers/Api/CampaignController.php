<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Campaign;
use App\Services\CampaignService;
use Illuminate\Http\Request;

class CampaignController extends Controller
{
    protected CampaignService $campaignService;

    public function __construct(CampaignService $campaignService)
    {
        $this->campaignService = $campaignService;
    }

    public function index(Request $request)
    {
        $campaigns = Campaign::where('user_id', $request->user()->id)
            ->with(['domain', 'template'])
            ->latest()
            ->paginate(20);

        return response()->json($campaigns);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'subject' => 'required|string|max:255',
            'from_name' => 'required|string|max:255',
            'from_email' => 'required|email',
            'reply_to' => 'nullable|email',
            'html_content' => 'required|string',
            'domain_id' => 'nullable|exists:domains,id',
            'template_id' => 'nullable|exists:email_templates,id',
            'contact_list_ids' => 'required|array',
            'contact_list_ids.*' => 'exists:contact_lists,id',
        ]);
        $validated['user_id'] = $request->user()->id;

        $campaign = $this->campaignService->createCampaign($validated);

        // Add recipients
        $this->campaignService->addRecipients($campaign, $validated['contact_list_ids']);

        return response()->json([
            'message' => 'Campaign created successfully.',
            'campaign' => $campaign->load(['domain', 'template']),
        ], 201);
    }

    public function show(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        return response()->json($campaign->load(['domain', 'template', 'contacts']));
    }

    public function update(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $validated = $request->validate([
            'name' => 'sometimes|string|max:255',
            'subject' => 'sometimes|string|max:255',
            'from_name' => 'sometimes|string|max:255',
            'from_email' => 'sometimes|email',
            'reply_to' => 'nullable|email',
            'html_content' => 'sometimes|string',
        ]);

        $campaign->update($validated);

        return response()->json([
            'message' => 'Campaign updated successfully.',
            'campaign' => $campaign,
        ]);
    }

    public function destroy(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $campaign->delete();

        return response()->json(['message' => 'Campaign deleted successfully.']);
    }

    public function send(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $validated = $request->validate([
            'scheduled_at' => 'nullable|date|after:now',
        ]);

        $scheduledAt = isset($validated['scheduled_at']) ? new \DateTime($validated['scheduled_at']) : null;

        $this->campaignService->scheduleCampaign($campaign, $scheduledAt);

        return response()->json([
            'message' => $scheduledAt ? 'Campaign scheduled successfully.' : 'Campaign is being sent',
            'campaign' => $campaign->fresh(),
        ]);
    }

    public function pause(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $this->campaignService->pauseCampaign($campaign);

        return response()->json([
            'message' => 'Campaign pause successfully.',
            'campaign' => $campaign->fresh(),
        ]);
    }

    public function resume(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $this->campaignService->resumeCampaign($campaign);

        return response()->json([
            'message' => 'Campaign resumed successfully.',
            'campaign' => $campaign->fresh(),
        ]);
    }

    public function stats(Request $request, Campaign $campaign)
    {
        if ($campaign->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized.'], 403);
        }

        $stats = $this->campaignService->getCampaignStats($campaign);

        return response()->json($stats);
    }
}
