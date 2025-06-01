<x-layouts.app :title="__('Contact Lists')">
    <div class="flex h-full w-full flex-1 flex-col gap-6">
        <!-- Main Content Area -->
        <div class="relative h-full flex-1 overflow-hidden rounded-xl border border-gray-200 bg-white">
            <div class="h-full p-6">
                <!-- Livewire Component -->
                <livewire:contact-list-manager />
            </div>
        </div>
    </div>

    @livewireStyles
    @livewireScripts
</x-layouts.app>