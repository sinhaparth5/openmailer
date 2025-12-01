<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Domain;
use App\Services\DomainService;
use Illuminate\Http\Request;

class DomainController extends Controller
{
    protected DomainService $domainService;

    public function __construct(DomainService $domainService)
    {
        $this->domainService = $domainService;
    }

    public function index(Request $request)
    {
        $domains = Domain::where('user_id', $request->user()->id)
            ->latest()
            ->get();

        return response()->json($domains);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'domain' => 'required|string|unique:domains,domain',
        ]);

        $domain = $this->domainService->addDomain(
            $request->user()->id,
            $validated['domain']
        );

        return response()->json([
            'message' => 'Domain added successfully',
            'domain' => $domain,
            'dns_instructions' => $this->domainService->getDNSInstructions($domain),
        ], 201);
    }

    public function show(Request $request, Domain $domain)
    {
        if ($domain->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json([
            'domain' => $domain,
            'dns_instructions' => $this->domainService->getDNSInstructions($domain),
        ]);
    }

    public function verify(Request $request, Domain $domain)
    {
        if ($domain->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $verified = $this->domainService->verifyDomain($domain);

        return response()->json([
            'verified' => $verified,
            'domain' => $domain->fresh(),
            'message' => $verified ? 'Domain verified successfully' : 'Domain verification failed. Please check DNS records.',
        ]);
    }

    public function destroy(Request $request, Domain $domain)
    {
        if ($domain->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $domain->delete();

        return response()->json(['message' => 'Domain deleted successfully']);
    }

    public function dnsInstructions(Request $request, Domain $domain)
    {
        if ($domain->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($this->domainService->getDNSInstructions($domain));
    }
}
