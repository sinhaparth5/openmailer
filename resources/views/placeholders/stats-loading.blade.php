<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-6">
    @for($i = 0; $i < 5; $i++)
        <div class="bg-white overflow-hidden shadow rounded-lg animate-pulse">
            <div class="p-5">
                <div class="flex items-center">
                    <div class="flex-shrink-0">
                        <div class="h-6 w-6 bg-gray-300 rounded"></div>
                    </div>
                    <div class="ml-5 w-0 flex-1">
                        <div class="h-4 bg-gray-300 rounded w-3/4 mb-2"></div>
                        <div class="h-6 bg-gray-300 rounded w-1/2"></div>
                    </div>
                </div>
            </div>
        </div>
    @endfor
</div>