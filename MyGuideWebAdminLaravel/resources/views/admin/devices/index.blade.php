@extends('layouts.admin')
@section('title','Registered devices') @section('heading','Registered devices')
@section('content')
<section class="panel">
    <p>Devices registered by My Guide Android clients. Registration tokens are intentionally not displayed.</p>
    <form method="get" class="search-form"><input name="q" value="{{ request('q') }}" placeholder="Search device or email"><button class="secondary">Search</button></form>
    <div class="table-wrap"><table><thead><tr><th>Device</th><th>Email</th><th>App version</th><th>Registered</th></tr></thead><tbody>
    @forelse($devices as $device)
        <tr><td>{{ $device->device ?: 'Unknown device' }}</td><td>{{ $device->email ?: '—' }}</td><td>{{ $device->version ?: '—' }}</td><td>{{ $device->date_create ?: '—' }}</td></tr>
    @empty <tr><td colspan="4">No devices registered.</td></tr> @endforelse
    </tbody></table></div>{{ $devices->links() }}
</section>
@endsection
