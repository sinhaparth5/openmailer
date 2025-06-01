@if($showModal && $list)
    <div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div class="relative top-10 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
            <div class="flex justify-between items-center mb-6">
                <div>
                    <h3 class="text-xl font-medium text-gray-900">{{ $list->name }}</h3>
                    <p class="text-gray-600">{{ $list->description }}</p>
                </div>
                <button wire:click="close" class="text-gray-400 hover:text-gray-600">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <!-- List Info -->
                <div class="bg-gray-50 rounded-lg p-4">
                    <h4 class="font-medium text-gray-900 mb-3">List Information</h4>
                    <dl class="space-y-2">
                        <div class="flex justify-between">
                            <dt class="text-sm text-gray-600">Type:</dt>
                            <dd class="text-sm font-medium text-gray-900">{{ ucfirst($list->type) }}</dd>
                        </div>
                        <div class="flex justify-between">
                            <dt class="text-sm text-gray-600">Status:</dt>
                            <dd class="text-sm font-medium {{ $list->is_active ? 'text-green-600' : 'text-red-600' }}">
                                {{ $list->is_active ? 'Active' : 'Inactive' }}
                            </dd>
                        </div>
                        <div class="flex justify-between">
                            <dt class="text-sm text-gray-600">Total Contacts:</dt>
                            <dd class="text-sm font-medium text-gray-900">{{ number_format($list->contacts_count) }}</dd>
                        </div>
                        <div class="flex justify-between">
                            <dt class="text-sm text-gray-600">Created:</dt>
                            <dd class="text-sm font-medium text-gray-900">{{ $list->created_at->format('M j, Y') }}</dd>
                        </div>
                    </dl>
                </div>

                <!-- Recent Contacts -->
                <div class="bg-gray-50 rounded-lg p-4">
                    <h4 class="font-medium text-gray-900 mb-3">Recent Contacts</h4>
                    <div class="space-y-2 max-h-40 overflow-y-auto">
                        @forelse($contacts as $contact)
                            <div class="flex items-center justify-between text-sm">
                                <div>
                                    <div class="font-medium text-gray-900">{{ $contact->display_name }}</div>
                                    <div class="text-gray-500">{{ $contact->email }}</div>
                                </div>
                                <div class="text-gray-400">
                                    {{ $contact->created_at->diffForHumans() }}
                                </div>
                            </div>
                        @empty
                            <p class="text-gray-500 text-sm">No contacts yet</p>
                        @endforelse
                    </div>
                </div>
            </div>

            <!-- Recent Activity -->
            @if($recentActivities->count() > 0)
                <div class="mt-6">
                    <h4 class="font-medium text-gray-900 mb-3">Recent Activity</h4>
                    <div class="bg-gray-50 rounded-lg p-4">
                        <div class="space-y-3 max-h-60 overflow-y-auto">
                            @foreach($recentActivities as $activity)
                                <div class="flex items-start space-x-3">
                                    <span class="text-lg">{{ $activity->activity_icon }}</span>
                                    <div class="flex-1">
                                        <div class="text-sm">
                                            <span class="font-medium">{{ $activity->contact->display_name }}</span>
                                            <span class="text-gray-600">{{ $activity->description }}</span>
                                        </div>
                                        <div class="text-xs text-gray-500">{{ $activity->created_at->diffForHumans() }}</div>
                                    </div>
                                </div>
                            @endforeach
                        </div>
                    </div>
                </div>
            @endif

            <div class="mt-6 flex justify-end">
                <button wire:click="close" 
                        class="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
                    Close
                </button>
            </div>
        </div>
    </div>
@endif