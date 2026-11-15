<?php
namespace App\Models;
use Illuminate\Foundation\Auth\User as Authenticatable;
class AdminUser extends Authenticatable {
 protected $table='users'; protected $primaryKey='id'; public $timestamps=false;
 protected $fillable=['name','username','email','password']; protected $hidden=['password','remember_token'];
}
