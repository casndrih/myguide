<?php

namespace App\Http\Controllers;

use App\Models\AdminUser;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Illuminate\View\View;

class AdminController extends Controller
{
    public function login(): View { return view('auth.login'); }

    public function authenticate(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'identity' => ['required', 'string', 'max:190'],
            'password' => ['required', 'string', 'max:255'],
        ]);

        $user = AdminUser::query()
            ->where('username', $data['identity'])
            ->orWhere('email', $data['identity'])
            ->first();

        if (!$user || !$this->verifyLegacyPassword($user, $data['password'])) {
            return back()->withErrors(['identity' => 'Invalid username/email or password.'])->onlyInput('identity');
        }

        Auth::login($user);
        $request->session()->regenerate();

        return redirect()->intended(route('admin.dashboard'));
    }

    private function verifyLegacyPassword(AdminUser $user, string $plain): bool
    {
        $stored = (string) $user->getAuthPassword();
        if (preg_match('/^[a-f0-9]{32}$/i', $stored)) {
            if (!hash_equals(strtolower($stored), md5($plain))) return false;
            $user->forceFill(['password' => Hash::make($plain)])->save();
            return true;
        }
        return Hash::check($plain, $stored);
    }

    public function logout(Request $request): RedirectResponse
    {
        Auth::logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();
        return redirect()->route('login');
    }

    public function dashboard(): View
    {
        return view('admin.dashboard', [
            'stats' => [
                'places' => DB::table('place')->count(),
                'categories' => DB::table('category')->count(),
                'news' => DB::table('news_info')->count(),
                'devices' => DB::table('gcm')->distinct()->count('regid'),
            ],
            'recentPlaces' => DB::table('place')->orderByDesc('last_update')->limit(8)->get(),
            'recentNews' => DB::table('news_info')->orderByDesc('last_update')->limit(8)->get(),
        ]);
    }

    public function places(Request $request): View
    {
        $places = DB::table('place')
            ->when($request->filled('q'), function ($query) use ($request): void {
                $term = '%' . trim((string) $request->input('q')) . '%';
                $query->where(fn ($where) => $where->where('name', 'like', $term)->orWhere('address', 'like', $term));
            })
            ->orderByDesc('last_update')->paginate(20)->withQueryString();

        return view('admin.places.index', compact('places'));
    }

    public function placeCreate(): View
    {
        return view('admin.places.form', [
            'place' => null,
            'categories' => DB::table('category')->orderBy('name')->get(),
            'selectedCategories' => [],
            'images' => [],
        ]);
    }

    public function placeStore(Request $request): RedirectResponse
    {
        [$data, $categoryIds] = $this->placeInput($request);
        $data['last_update'] = now()->timestamp;

        $id = DB::transaction(function () use ($data, $categoryIds): int {
            $id = (int) DB::table('place')->insertGetId($data, 'place_id');
            $this->syncPlaceCategories($id, $categoryIds);
            return $id;
        });
        $this->storePlaceGallery($request, $id);

        return redirect()->route('admin.places.edit', $id)->with('status', 'Destination created.');
    }

    public function placeEdit(int $id): View
    {
        $place = DB::table('place')->where('place_id', $id)->firstOrFail();

        return view('admin.places.form', [
            'place' => $place,
            'categories' => DB::table('category')->orderBy('name')->get(),
            'selectedCategories' => DB::table('place_category')->where('place_id', $id)->pluck('cat_id')->all(),
            'images' => DB::table('images')->where('place_id', $id)->get(),
        ]);
    }

    public function updatePlace(Request $request, int $id): RedirectResponse
    {
        DB::table('place')->where('place_id', $id)->firstOrFail();
        [$data, $categoryIds] = $this->placeInput($request);
        $data['last_update'] = now()->timestamp;

        DB::transaction(function () use ($id, $data, $categoryIds): void {
            DB::table('place')->where('place_id', $id)->update($data);
            $this->syncPlaceCategories($id, $categoryIds);
        });
        $this->storePlaceGallery($request, $id);

        return back()->with('status', 'Destination updated.');
    }

    private function placeInput(Request $request): array
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:50'],
            'image' => ['nullable', 'string', 'max:255'],
            'image_file' => ['nullable', 'image', 'mimes:jpg,jpeg,png,webp', 'max:4096'],
            'gallery_images' => ['sometimes', 'array', 'max:4'],
            'gallery_images.*' => ['image', 'mimes:jpg,jpeg,png,webp', 'max:4096'],
            'address' => ['nullable', 'string', 'max:100'],
            'fee' => ['nullable', 'string', 'max:10'],
            'status' => ['nullable', 'string', 'max:200'],
            'phone' => ['nullable', 'string', 'max:25'],
            'website' => ['nullable', 'string', 'max:100'],
            'description' => ['nullable', 'string'],
            'lat' => ['nullable', 'numeric', 'between:-90,90'],
            'lng' => ['nullable', 'numeric', 'between:-180,180'],
            'categories' => ['sometimes', 'array'],
            'categories.*' => ['integer', 'exists:category,cat_id'],
        ]);

        $categoryIds = $data['categories'] ?? [];
        unset($data['categories'], $data['image_file'], $data['gallery_images']);
        if ($request->hasFile('image_file')) {
            $data['image'] = $this->storeUpload($request->file('image_file'), 'place', 'place');
        }

        return [$data, $categoryIds];
    }

    private function syncPlaceCategories(int $placeId, array $categoryIds): void
    {
        DB::table('place_category')->where('place_id', $placeId)->delete();
        $rows = collect($categoryIds)->unique()->map(static fn ($id): array => [
            'place_id' => $placeId, 'cat_id' => (int) $id,
        ])->all();
        if ($rows !== []) DB::table('place_category')->insert($rows);
    }

    private function storePlaceGallery(Request $request, int $placeId): void
    {
        foreach ($request->file('gallery_images', []) as $file) {
            if (!$file) continue;
            $name = $this->storeUpload($file, 'place', 'place_' . $placeId);
            DB::table('images')->insert(['place_id' => $placeId, 'name' => $name]);
        }
    }

    private function storeUpload($file, string $folder, string $prefix): string
    {
        $directory = public_path('uploads/' . $folder);
        if (!is_dir($directory)) mkdir($directory, 0755, true);
        $extension = strtolower($file->guessExtension() ?: 'jpg');
        $filename = $prefix . '_' . Str::uuid()->toString() . '.' . $extension;
        $file->move($directory, $filename);
        return $filename;
    }

    public function placeDelete(int $id): RedirectResponse
    {
        DB::transaction(function () use ($id): void {
            DB::table('place_category')->where('place_id', $id)->delete();
            DB::table('images')->where('place_id', $id)->delete();
            DB::table('place')->where('place_id', $id)->delete();
        });
        return redirect()->route('admin.places.index')->with('status', 'Destination deleted.');
    }

    public function categories(Request $request): View
    {
        $categories = DB::table('category')
            ->when($request->filled('q'), fn ($query) => $query->where('name', 'like', '%' . trim((string) $request->input('q')) . '%'))
            ->orderBy('cat_id')->paginate(30)->withQueryString();
        return view('admin.categories.index', compact('categories'));
    }

    public function categoryStore(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100', 'unique:category,name'],
            'icon' => ['nullable', 'integer', 'min:0'],
        ]);
        $data['icon'] = $data['icon'] ?? 0;
        DB::table('category')->insert($data);
        return redirect()->route('admin.categories.index')->with('status', 'Category created.');
    }

    public function categoryUpdate(Request $request, int $id): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100', 'unique:category,name,' . $id . ',cat_id'],
            'icon' => ['nullable', 'integer', 'min:0'],
        ]);
        $data['icon'] = $data['icon'] ?? 0;
        DB::table('category')->where('cat_id', $id)->update($data);
        return redirect()->route('admin.categories.index')->with('status', 'Category updated.');
    }

    public function categoryDelete(int $id): RedirectResponse
    {
        DB::transaction(function () use ($id): void {
            DB::table('place_category')->where('cat_id', $id)->delete();
            DB::table('category')->where('cat_id', $id)->delete();
        });
        return redirect()->route('admin.categories.index')->with('status', 'Category deleted.');
    }

    public function news(Request $request): View
    {
        $news = DB::table('news_info')
            ->when($request->filled('q'), fn ($query) => $query->where('title', 'like', '%' . trim((string) $request->input('q')) . '%'))
            ->orderByDesc('last_update')->paginate(20)->withQueryString();
        return view('admin.news.index', compact('news'));
    }

    public function newsCreate(): View { return view('admin.news.form', ['newsItem' => null]); }

    public function newsStore(Request $request): RedirectResponse
    {
        $data = $this->newsInput($request);
        $data['last_update'] = now()->timestamp;
        $id = DB::table('news_info')->insertGetId($data);
        return redirect()->route('admin.news.edit', $id)->with('status', 'News item created.');
    }

    public function newsEdit(int $id): View
    {
        return view('admin.news.form', ['newsItem' => DB::table('news_info')->where('id', $id)->firstOrFail()]);
    }

    public function newsUpdate(Request $request, int $id): RedirectResponse
    {
        DB::table('news_info')->where('id', $id)->firstOrFail();
        $data = $this->newsInput($request);
        $data['last_update'] = now()->timestamp;
        DB::table('news_info')->where('id', $id)->update($data);
        return back()->with('status', 'News item updated.');
    }

    private function newsInput(Request $request): array
    {
        $data = $request->validate([
            'title' => ['required', 'string', 'max:100'],
            'brief_content' => ['nullable', 'string', 'max:200'],
            'full_content' => ['nullable', 'string'],
            'image' => ['nullable', 'string', 'max:255'],
            'image_file' => ['nullable', 'image', 'mimes:jpg,jpeg,png,webp', 'max:4096'],
        ]);
        unset($data['image_file']);
        if ($request->hasFile('image_file')) {
            $data['image'] = $this->storeUpload($request->file('image_file'), 'news', 'news_info');
        }
        return $data;
    }

    public function newsDelete(int $id): RedirectResponse
    {
        DB::table('news_info')->where('id', $id)->delete();
        return redirect()->route('admin.news.index')->with('status', 'News item deleted.');
    }

    public function devices(Request $request): View
    {
        $devices = DB::table('gcm')
            ->when($request->filled('q'), function ($query) use ($request): void {
                $term = '%' . trim((string) $request->input('q')) . '%';
                $query->where(function ($where) use ($term): void {
                    $where->where('device', 'like', $term)->orWhere('email', 'like', $term);
                });
            })
            ->orderByDesc('id')->paginate(30)->withQueryString();
        return view('admin.devices.index', compact('devices'));
    }
}
