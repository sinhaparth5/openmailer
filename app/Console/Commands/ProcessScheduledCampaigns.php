<?php

namespace App\Console\Commands;

use App\Models\Campaign;
use App\Services\CampaignService;
use Illuminate\Console\Command;

class ProcessScheduledCampaigns extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'campaigns:process-scheduled';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Process scheduled campaigns';

    /**
     * Execute the console command.
     */
    public function handle(CampaignService $campaignService)
    {
        $campaigns = Campaign::where('status', 'scheduled')
            ->where('scheduled_at', '<=', now())
            ->get();

        foreach ($campaigns as $campaign) {
            $this->info('Processing campaign: {$campaign->name}');
            $campaignService->sendCampaign($campaign);
        }

        $this->info('Processed {$campaigns->count()} scheduled campaigns');

        return 0;
    }
}
