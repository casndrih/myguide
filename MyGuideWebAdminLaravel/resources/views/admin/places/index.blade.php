@extends('layouts.admin')
@section('title','Places') @section('heading','Places')
@section('content')
<section class="panel"><div class="panel-head"><h2>Destination directory</h2><span>{{ $places->total() }} destinations</span></div>
<table><thead><tr><th>Name</th><th>Address</th><th>Status</th><th>Updated</th></tr></thead><tbody>
@forelse($places as $place)<tr><td>{{ $place->name }}</td><td>{{ $place->address ?: '—' }}</td><td>{{ $place->status ?: 'Active' }}</td><td>{{ $place->last_update ? date('d M Y',(int)$place->last_update) : '—' }}</td></tr>@empty<tr><td colspan="4">No destinations found.</td></tr>@endforelse
</tbody></table>{{ $places->links() }}</section>
@endsection
