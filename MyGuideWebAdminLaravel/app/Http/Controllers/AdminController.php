<?php
namespace App\Http\Controllers;
use App\Models\AdminUser;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;
use Illuminate\View\View;
use Illuminate\Http\RedirectResponse;
class AdminController extends Controller {
 public function login(): View { return view('auth.login'); }
 public function authenticate(Request $r): RedirectResponse {
  $d=$r->validate(['identity'=>['required','string','max:190'],'password'=>['required','string','max:255']]);
  $u=AdminUser::query()->where('username',$d['identity'])->orWhere('email',$d['identity'])->first();
  if(!$u || !$this->verifyLegacyPassword($u,$d['password'])) return back()->withErrors(['identity'=>'Invalid username/email or password.'])->onlyInput('identity');
  Auth::login($u); $r->session()->regenerate(); return redirect()->intended(route('admin.dashboard'));
 }
 private function verifyLegacyPassword(AdminUser $u,string $plain): bool {
  $stored=(string)$u->getAuthPassword();
  if(preg_match('/^[a-f0-9]{32}$/i',$stored)) { if(!hash_equals(strtolower($stored),md5($plain))) return false; $u->forceFill(['password'=>Hash::make($plain)])->save(); return true; }
  return Hash::check($plain,$stored);
 }
 public function logout(Request $r): RedirectResponse { Auth::logout(); $r->session()->invalidate(); $r->session()->regenerateToken(); return redirect()->route('login'); }
 public function dashboard(): View { return view('admin.dashboard',['stats'=>['places'=>DB::table('place')->count(),'categories'=>DB::table('category')->count(),'news'=>DB::table('news_info')->count(),'devices'=>DB::table('gcm')->distinct('regid')->count('regid')],'recentPlaces'=>DB::table('place')->orderByDesc('last_update')->limit(8)->get(),'recentNews'=>DB::table('news_info')->orderByDesc('last_update')->limit(8)->get()]); }
 public function places(Request $r): View { $places=DB::table('place')->when($r->filled('q'),fn($q)=>$q->where(function($w)use($r){$term='%'.$r->string('q')->trim().'%';$w->where('name','like',$term)->orWhere('address','like',$term);}))->orderByDesc('last_update')->paginate(20)->withQueryString(); return view('admin.places.index',compact('places')); }
 public function placeCreate(): View { return view('admin.places.form',['place'=>null,'categories'=>DB::table('category')->orderBy('name')->get(),'selectedCategories'=>[]]); }
 public function placeStore(Request $r): RedirectResponse { $d=$this->placeInput($r);$d['last_update']=now()->timestamp;$id=DB::table('place')->insertGetId($d,'place_id');$this->syncPlaceCategories($id,$r->input('categories',[]));return redirect()->route('admin.places.edit',$id)->with('status','Destination created.'); }
 public function placeEdit(int $id): View { $place=DB::table('place')->where('place_id',$id)->firstOrFail();$selectedCategories=DB::table('place_category')->where('place_id',$id)->pluck('cat_id')->all();$categories=DB::table('category')->orderBy('name')->get();return view('admin.places.form',compact('place','categories','selectedCategories')); }
 public function updatePlace(Request $r,int $id): RedirectResponse { DB::table('place')->where('place_id',$id)->firstOrFail();$d=$this->placeInput($r);$d['last_update']=now()->timestamp;DB::table('place')->where('place_id',$id)->update($d);$this->syncPlaceCategories($id,$r->input('categories',[]));return back()->with('status','Destination updated.'); }
 public function placeDelete(int $id): RedirectResponse { DB::transaction(function()use($id){DB::table('place_category')->where('place_id',$id)->delete();DB::table('images')->where('place_id',$id)->delete();DB::table('place')->where('place_id',$id)->delete();});return redirect()->route('admin.places.index')->with('status','Destination deleted.'); }
 private function placeInput(Request $r): array { return $r->validate(['name'=>['required','string','max:190'],'image'=>['nullable','string','max:255'],'address'=>['nullable','string','max:255'],'fee'=>['nullable','string','max:100'],'status'=>['nullable','string','max:50'],'phone'=>['nullable','string','max:80'],'website'=>['nullable','url','max:255'],'description'=>['nullable','string'],'lat'=>['nullable','numeric','between:-90,90'],'lng'=>['nullable','numeric','between:-180,180'],'categories'=>['sometimes','array'],'categories.*'=>['integer','exists:category,cat_id']]); }
 private function syncPlaceCategories(int $id,array $ids): void { DB::table('place_category')->where('place_id',$id)->delete();$rows=collect($ids)->unique()->map(fn($v)=>['place_id'=>$id,'cat_id'=>(int)$v])->all();if($rows)DB::table('place_category')->insert($rows); }
 public function news(Request $r): View { $news=DB::table('news_info')->when($r->filled('q'),fn($q)=>$q->where('title','like','%'.$r->string('q')->trim().'%'))->orderByDesc('last_update')->paginate(20)->withQueryString();return view('admin.news.index',compact('news')); }
 public function newsCreate(): View { return view('admin.news.form',['newsItem'=>null]); }
 public function newsStore(Request $r): RedirectResponse { $d=$this->newsInput($r);$d['last_update']=now()->timestamp;$id=DB::table('news_info')->insertGetId($d);return redirect()->route('admin.news.edit',$id)->with('status','News item created.'); }
 public function newsEdit(int $id): View { return view('admin.news.form',['newsItem'=>DB::table('news_info')->where('id',$id)->firstOrFail()]); }
 public function newsUpdate(Request $r,int $id): RedirectResponse { DB::table('news_info')->where('id',$id)->firstOrFail();$d=$this->newsInput($r);$d['last_update']=now()->timestamp;DB::table('news_info')->where('id',$id)->update($d);return back()->with('status','News item updated.'); }
 public function newsDelete(int $id): RedirectResponse { DB::table('news_info')->where('id',$id)->delete();return redirect()->route('admin.news.index')->with('status','News item deleted.'); }
 private function newsInput(Request $r): array { return $r->validate(['title'=>['required','string','max:255'],'brief_content'=>['nullable','string','max:1000'],'full_content'=>['nullable','string'],'image'=>['nullable','string','max:255']]); }
}
