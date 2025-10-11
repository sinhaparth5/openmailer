<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('email_links', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('campaign_id')->constrained()->onDelete('cascade');
            $table->text('original_url');
            $table->string('short_code', 20)->unique();
            $table->integer('total_clicks')->default(0);
            $table->integer('unique_clicks')->default(0);
            $table->timestamps();

            $table->index('short_code');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('email_links');
    }
};
