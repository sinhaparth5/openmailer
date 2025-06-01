<div class="relative">
    <div class="relative">
        <input wire:model.live="search" type="text" 
               placeholder="{{ $placeholder }}"
               class="w-full border border-gray-300 rounded-lg px-3 py-2 pr-10 focus:outline-none focus:ring-2 focus:ring-blue-500">
        
        @if(count($selectedLists) > 0)
            <button wire:click="clearSelection" 
                    class="absolute right-2 top-2 text-gray-400 hover:text-gray-600">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
            </button>
        @endif
    </div>

    <!-- Selected Lists -->
    @if(count($selectedLists) > 0)
        <div class="mt-2 flex flex-wrap gap-1">
            @foreach($lists->whereIn('id', $selectedLists) as $list)
                <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    {{ $list->name }}
                    <button wire:click="selectList({{ $list->id }})" class="ml-1 text-blue-600 hover:text-blue-800">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                </span>
            @endforeach
        </div>
    @endif

    <!-- Dropdown List -->
    @if($search || count($selectedLists) === 0)
        <div class="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto">
            @forelse($lists as $list)
                <button wire:click="selectList({{ $list->id }})" 
                        class="w-full text-left px-3 py-2 hover:bg-gray-50 flex items-center justify-between
                               {{ in_array($list->id, $selectedLists) ? 'bg-blue-50' : '' }}">
                    <div>
                        <div class="font-medium text-gray-900">{{ $list->name }}</div>
                        <div class="text-sm text-gray-500">{{ number_format($list->contacts_count) }} contacts</div>
                    </div>
                    @if(in_array($list->id, $selectedLists))
                        <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                        </svg>
                    @endif
                </button>
            @empty
                <div class="px-3 py-2 text-gray-500 text-sm">No lists found</div>
            @endforelse
        </div>
    @endif
</div>