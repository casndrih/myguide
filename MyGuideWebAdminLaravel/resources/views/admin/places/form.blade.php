@extends('layouts.admin')
@section('title',$place ? 'Edit destination' : 'Add destination')
@section('heading',$place ? 'Edit destination' : 'Add destination')
@section('content')
<section class="panel form-panel">
<form method="post" enctype="multipart/form-data" action="{{ $place ? route('admin.places.update',$place->place_id) : route('admin.places.store') }}">
    @csrf @if($place) @method('put') @endif
    <div class="form-grid">
        <div class="field"><label>Destination name *</label><input name="name" required maxlength="190" value="{{ old('name',$place->name ?? '') }}"></div>
        <div class="field"><label>Address</label><input name="address" maxlength="255" value="{{ old('address',$place->address ?? '') }}"></div>
        <div class="field"><label>Entry fee</label><input name="fee" maxlength="100" value="{{ old('fee',$place->fee ?? '') }}"></div>
        <div class="field"><label>Safety / status information</label><input name="status" maxlength="50" value="{{ old('status',$place->status ?? '') }}"></div>
        <div class="field"><label>Phone</label><input name="phone" maxlength="80" value="{{ old('phone',$place->phone ?? '') }}"></div>
        <div class="field"><label>Website</label><input type="url" name="website" maxlength="255" value="{{ old('website',$place->website ?? '') }}"></div>
        <div class="field"><label>Latitude</label><input type="number" step="any" min="-90" max="90" name="lat" value="{{ old('lat',$place->lat ?? '') }}"></div>
        <div class="field"><label>Longitude</label><input type="number" step="any" min="-180" max="180" name="lng" value="{{ old('lng',$place->lng ?? '') }}"></div>
        <div class="field wide"><label>Primary image (JPG, PNG or WebP; max 4 MB)</label><input type="file" name="image_file" accept="image/jpeg,image/png,image/webp">
            <input type="hidden" name="image" value="{{ old('image',$place->image ?? '') }}">
            @if($place && $place->image)<small>Current file: {{ $place->image }}</small>@endif
        </div>
        <div class="field wide"><label>Additional images (up to 4)</label><input type="file" name="gallery_images[]" accept="image/jpeg,image/png,image/webp" multiple>
            @if(count($images))<small>Existing gallery: @foreach($images as $image) {{ $image->name }}@if(!$loop->last), @endif @endforeach</small>@endif
        </div>
        <div class="field wide"><label>Description</label><textarea name="description" rows="9">{{ old('description',$place->description ?? '') }}</textarea></div>
        <div class="field wide"><label>Categories</label><div class="check-grid">
            @foreach($categories as $category)
                <label class="check"><input type="checkbox" name="categories[]" value="{{ $category->cat_id }}" @checked(in_array($category->cat_id, old('categories',$selectedCategories)))> {{ $category->name }}</label>
            @endforeach
        </div></div>
    </div>
    <div class="form-actions"><a class="secondary" href="{{ route('admin.places.index') }}">Cancel</a><button type="submit">Save destination</button></div>
</form>
@if($place)
<form method="post" action="{{ route('admin.places.destroy',$place->place_id) }}" onsubmit="return confirm('Delete this destination and its category/image records?')">@csrf @method('delete')<button class="danger">Delete destination</button></form>
@endif
</section>
@endsection
