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
        Schema::create('domains', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignUuid('user_id')->constrained()->onDelete('cascade');
            $table->string('domain')->unique();
            $table->string('verification_token');
            $table->boolean('is_verified')->default(false);
            $table->string('dkim_selector', 100)->default('mail');
            $table->text('dkim_private_key')->nullable();
            $table->text('dkim_public_key')->nullable();
            $table->text('spf_record')->nullable();
            $table->text('dmarc_record')->nullable();
            $table->enum('status', ['pending', 'verified', 'failed'])->default('pending');
            $table->timestamps();

            $table->index('user_id');
            $table->index('status');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('domains');
    }
};
