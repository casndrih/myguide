@extends('layouts.admin')
@section('title','Categories') @section('heading','Categories')
@section('content')
<section class="panel">
    <h2>Add a destination category</h2>
    <form method="post" action="{{ route('admin.categories.store') }}" class="form-grid">
        @csrf
        <div class="field"><label>Category name</label><input name="name" maxlength="100" required value="{{ old('name') }}"></div>
        <div class="field"><label>Icon resource ID (optional)</label><input type="number" min="0" name="icon" value="{{ old('icon') }}"></div>
        <div class="wide"><button type="submit">Create category</button></div>
    </form>
</section>
<section class="panel">
    <form method="get" class="search-form"><input name="q" value="{{ request('q') }}" placeholder="Search categories"><button class="secondary">Search</button></form>
    <div class="table-wrap"><table><thead><tr><th>ID</th><th>Category</th><th>Icon resource</th><th>Actions</th></tr></thead><tbody>
    @forelse($categories as $category)
        <tr><td>{{ $category->cat_id }}</td><td colspan="2">
            <form method="post" action="{{ route('admin.categories.update',$category->cat_id) }}" class="form-grid">
                @csrf @method('put')
                <div class="field"><label>Name</label><input name="name" required maxlength="100" value="{{ $category->name }}"></div>
                <div class="field"><label>Icon resource ID</label><input type="number" min="0" name="icon" value="{{ $category->icon }}"></div>
                <div class="wide"><button type="submit">Save changes</button></div>
            </form>
        </td><td><form method="post" action="{{ route('admin.categories.destroy',$category->cat_id) }}" onsubmit="return confirm('Delete this category and its place assignments?')">@csrf @method('delete')<button class="danger">Delete</button></form></td></tr>
    @empty <tr><td colspan="4">No categories found.</td></tr> @endforelse
    </tbody></table></div>{{ $categories->links() }}
</section>
@endsection
