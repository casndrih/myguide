@extends('layouts.admin')
@section('title',$newsItem ? 'Edit news' : 'Add news') @section('heading',$newsItem ? 'Edit news' : 'Add news')
@section('content')
<section class="panel form-panel">
<form method="post" enctype="multipart/form-data" action="{{ $newsItem ? route('admin.news.update',$newsItem->id) : route('admin.news.store') }}">
    @csrf @if($newsItem) @method('put') @endif
    <div class="field"><label>Headline *</label><input name="title" required maxlength="255" value="{{ old('title',$newsItem->title ?? '') }}"></div>
    <div class="field"><label>Brief summary</label><textarea name="brief_content" rows="3">{{ old('brief_content',$newsItem->brief_content ?? '') }}</textarea></div>
    <div class="field"><label>Full content</label><textarea name="full_content" rows="12">{{ old('full_content',$newsItem->full_content ?? '') }}</textarea></div>
    <div class="field"><label>Banner image (JPG, PNG or WebP; max 4 MB)</label><input type="file" name="image_file" accept="image/jpeg,image/png,image/webp"><input type="hidden" name="image" value="{{ old('image',$newsItem->image ?? '') }}">@if($newsItem && $newsItem->image)<small>Current file: {{ $newsItem->image }}</small>@endif</div>
    <div class="form-actions"><a class="secondary" href="{{ route('admin.news.index') }}">Cancel</a><button type="submit">Save news</button></div>
</form>
@if($newsItem)<form method="post" action="{{ route('admin.news.destroy',$newsItem->id) }}" onsubmit="return confirm('Delete this news item?')">@csrf @method('delete')<button class="danger">Delete news</button></form>@endif
</section>
@endsection
