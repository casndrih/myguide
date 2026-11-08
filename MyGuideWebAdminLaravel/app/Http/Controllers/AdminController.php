<?php
namespace App\Http\Controllers;
use App\Models\AdminUser;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
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
 public function places(): View { $places=DB::table('place')->orderByDesc('last_update')->paginate(20); return view('admin.places.index',compact('places')); }
 public function updatePlace(Request $r,int $id): RedirectResponse { $d=$r->validate(['name'=>['required','string','max:190'],'address'=>['nullable','string','max:255'],'fee'=>['nullable','string','max:100'],'status'=>['nullable','string','max:50'],'phone'=>['nullable','string','max:80'],'website'=>['nullable','url','max:255'],'description'=>['nullable','string'],'lat'=>['nullable','numeric','between:-90,90'],'lng'=>['nullable','numeric','between:-180,180']]); $d['last_update']=now()->timestamp; DB::table('place')->where('place_id',$id)->update($d); return back()->with('status','Destination updated.'); }
}
