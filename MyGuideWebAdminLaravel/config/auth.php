<?php
use App\Models\AdminUser;
return ['defaults'=>['guard'=>'web','passwords'=>'admin_users'],'guards'=>['web'=>['driver'=>'session','provider'=>'admin_users']],'providers'=>['admin_users'=>['driver'=>'eloquent','model'=>AdminUser::class]],'passwords'=>['admin_users'=>['provider'=>'admin_users','table'=>'password_reset_tokens','expire'=>60,'throttle'=>60]],'password_timeout'=>10800];
