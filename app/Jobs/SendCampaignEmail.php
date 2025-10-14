<?php

namespace App\Jobs;

use App\Models\EmailQueue;
use App\Services\EmailService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Foundation\Queue\Queueable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;

class SendCampaignEmail implements ShouldQueue
{
    use Queueable, Dispatchable, InteractsWithQueue, SerializesModels;

    public $tries = 3;
    public $timeout = 120;

    protected EmailQueue $emailQueue;

    public function __construct(EmailQueue $emailQueue) {
        $this->emailQueue = $emailQueue;
    }

    /**
     * Execute the job.
     */
    public function handle(EmailService $emailService): void {
        // Update status to processing
        $this->emailQueue->update(['status' => 'processing']);
        // Send the email
        $emailService->sendEmail($this->emailQueue);
    }

    public function failed(\Throwable $exception): void {
        $this->emailQueue->update([
            'status' => 'failed',
            'error_message' => $exception->getMessage(),
        ]);
    }
}
