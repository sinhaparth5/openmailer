<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\EmailTemplate;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class TemplateController extends Controller {
    public function index(Request $request): JsonResponse
    {
        $templates = EmailTemplate::where('user_id', $request->user()->id)
            ->when($request->has('search'), function ($query) use ($request) {
                $query->where('name', 'like', '%' . $request->search . '%');
            })
            ->when($request->has('favorite'), function ($query) {
                $query->where('is_favorite', true);
            })
            ->latest()
            ->paginate(20);

        return response()->json($templates);
    }

    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'subject' => 'required|string|max:255',
            'html_content' => 'required|string',
            'json_design' => 'nullable|array',
            'thumbnail' => 'nullable|string|max:500',
            'is_favorite' => 'nullable|boolean',
        ]);

        $template = EmailTemplate::create([
            'user_id' => $request->user()->id,
            'name' => $validated['name'],
            'subject' => $validated['subject'],
            'html_content' => $validated['html_content'],
            'json_design' => $validated['json_design'] ?? null,
            'thumbnail' => $validated['thumbnail'] ?? null,
            'is_favorite' => $validated['is_favorite'] ?? false,
        ]);

        return response()->json([
            'message' => 'Template created successfully',
            'template' => $template
        ], 201);
    }

    public function show(Request $request, EmailTemplate $template): JsonResponse
    {
        if ($template->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        return response()->json($template);
    }

    public function update(Request $request, EmailTemplate $template): JsonResponse
    {
        if ($template->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $validated = $request->validate([
            'name' => 'sometimes|string|max:255',
            'subject' => 'sometimes|string|max:255',
            'html_content' => 'sometimes|string',
            'json_design' => 'nullable|array',
            'thumbnail' => 'nullable|string|max:500',
            'is_favorite' => 'sometimes|boolean',
        ]);

        $template->update($validated);

        return response()->json([
            'message' => 'Template updated successfully',
            'template' => $template->fresh()
        ]);
    }

    public function destroy(Request $request, EmailTemplate $template): JsonResponse
    {
        if ($template->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        // Check if template is being used by any campaigns
        $campaignCount = $template->campaigns()->count();

        if ($campaignCount > 0) {
            return response()->json([
                'message' => 'Cannot delete template. It is being used by ' . $campaignCount . ' campaign(s).'
            ], 422);
        }

        $template->delete();

        return response()->json([
            'message' => 'Template deleted successfully'
        ]);
    }

    public function duplicate(Request $request, EmailTemplate $template): JsonResponse
    {
        if ($template->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $newTemplate = $template->replicate();
        $newTemplate->name = $template->name . ' (Copy)';
        $newTemplate->is_favorite = false;
        $newTemplate->save();

        return response()->json([
            'message' => 'Template duplicated successfully',
            'template' => $newTemplate
        ], 201);
    }

    public function toggleFavorite(Request $request, EmailTemplate $template): JsonResponse
    {
        if ($template->user_id !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $template->update([
            'is_favorite' => !$template->is_favorite
        ]);

        return response()->json([
            'message' => 'Template favorite status updated',
            'template' => $template->fresh()
        ]);
    }
}
