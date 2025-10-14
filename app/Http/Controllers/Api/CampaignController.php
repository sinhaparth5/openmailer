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

    public function store(Request $request) {
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
            'campaign' => $campaign->load(['domain', 'template'])
        ], 201);
    }

    
}
