<x-layouts.app :title="__('Contact Lists')">
    <div class="flex h-full w-full flex-1 flex-col gap-6">
        <!-- Stats Row -->
        <div class="grid auto-rows-min gap-4 lg:grid-cols-5 md:grid-cols-2">
            <div class="relative overflow-hidden rounded-xl border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-800 p-6">
                <div class="flex items-center">
                    <div class="flex-shrink-0">
                        <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                        </svg>
                    </div>
                    <div class="ml-5 w-0 flex-1">
                        <dl>
                            <dt class="text-sm font-medium text-gray-500 dark:text-gray-400 truncate">Total Lists</dt>
                            <dd class="text-lg font-medium text-gray-900 dark:text-gray-100">{{ \App\Models\ContactList::count() }}</dd>
                        </dl>
                    </div>
                </div>
            </div>
            <!-- Add more stat cards as needed -->
        </div>

        <!-- Main Content Area -->
        <div class="relative h-full flex-1 overflow-hidden rounded-xl border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-800">
            <div class="h-full p-6">
                <!-- Livewire Component -->
                <livewire:contact-list-manager />
            </div>
        </div>
    </div>

    @livewireStyles
    @livewireScripts
</x-layouts.app>