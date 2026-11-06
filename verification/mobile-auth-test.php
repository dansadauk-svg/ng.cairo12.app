<?php
// Isolated checks for opaque sessions, namespace scope, password reset invalidation and PIN lockout.
define('ABSPATH',__DIR__);define('DAY_IN_SECONDS',86400);define('MINUTE_IN_SECONDS',60);
class WP_Error {function __construct(public $code,public $message='',public $data=[]){}function get_error_code(){return $this->code;}}
class WP_User {public $ID=3;public $display_name='Test buyer';public $user_email='test@example.com';public $user_login='test';public $user_pass='hash-before-reset';}
class WP_REST_Response {function __construct(public $data){}function header($k,$v){}}
class WP_REST_Request {function __construct(public $data){}function get_param($key){return $this->data[$key]??null;}}
class FakeDB {function prepare($s,...$args){return $s;}function get_var($s){return 1;}}
$wpdb=new FakeDB();$user=new WP_User();$meta=[];$transients=[];$checks=0;
function add_filter(...$args){}function add_action(...$args){}function is_wp_error($v){return $v instanceof WP_Error;}
function get_userdata($id){global $user;return $id===$user->ID?$user:false;}
function get_user_by($field,$value){global $user;return $value===$user->user_email?$user:false;}
function wp_parse_url($value,$component){return parse_url($value,$component);}function rest_url($path){return 'https://cairo12.ng/wp-json/'.$path;}
function wp_salt($type){return 'test-salt';}function sanitize_email($e){return (string)$e;}function is_email($e){return filter_var($e,FILTER_VALIDATE_EMAIL);}function sanitize_text_field($s){return (string)$s;}
function wp_authenticate($login,$secret){global $user;return hash_equals(hash_hmac('sha256',$user->user_email.':0123',wp_salt('auth')),$secret)?$user:new WP_Error('bad');}
function get_transient($key){global $transients;return $transients[$key]??0;}function set_transient($key,$value,$time){global $transients;$transients[$key]=$value;}function delete_transient($key){global $transients;unset($transients[$key]);}
function get_user_meta($id,$key=null,$single=false){global $meta;if($key===null){$all=[];foreach($meta as $k=>$v)$all[$k]=[$v];return $all;}return $meta[$key]??'';}
function add_user_meta($id,$key,$value,$unique){global $meta;$meta[$key]=$value;return true;}function delete_user_meta($id,$key){global $meta;unset($meta[$key]);}
function maybe_unserialize($v){return $v;}
function check($result,$name){global $checks;if(!$result)throw new RuntimeException($name);$checks++;echo "PASS $name\n";}
$meta['_c12_pin']=1;
require dirname($argv[1] ?? __DIR__.'/../../android-backend/cairo12-core/includes/mobile.php').'/locations.php';
require ($argv[1] ?? __DIR__.'/../../android-backend/cairo12-core/includes/mobile.php');
$request=new WP_REST_Request(['email'=>'test@example.com','pin'=>'0123']);
$response=C12_Mobile::login($request);check($response instanceof WP_REST_Response,'Leading-zero four-digit PIN signs in');
$token=$response->data['token'];$key='_c12_mobile_'.hash('sha256',$token);
check(isset($meta[$key]),'Only token hash is used as the stored session key');
check(!str_contains(json_encode($meta),$token),'Raw bearer token is not stored server-side');
$_SERVER['REQUEST_URI']='/wp-json/cairo12/v1/orders';$_SERVER['HTTP_AUTHORIZATION']='Bearer '.$token;
check(C12_Mobile::identify(0)===3,'Opaque session authenticates Cairo12 orders');
check(C12_Mobile::authenticate(null)===true,'Mobile session works without a browser nonce');
check($response->data['expires']===0&&$meta[$key]['expires']===0,'New app sessions have no automatic expiry');
$meta[$key]['expires']=time()+3600;C12_Mobile::identify(0);check($meta[$key]['expires']===0,'Existing valid app session migrates to persistent sign-in');
for($i=0;$i<7;$i++)C12_Mobile::login($request);
check(isset($meta[$key]),'Signing in on more devices does not evict earlier sessions');

$_SERVER['REQUEST_URI']='/wp-json/wp/v2/users';check(C12_Mobile::identify(0)===0,'Session is not accepted for unrelated WordPress routes');
$_SERVER['REQUEST_URI']='/wp-json/wp/v2/users?redirect=/wp-json/cairo12/v1/orders';check(C12_Mobile::identify(0)===0,'Query text cannot expand token scope');
$_SERVER['REQUEST_URI']='/wp-json/cairo12/v1/orders';$user->user_pass='hash-after-reset';check(C12_Mobile::identify(0)===0,'Password reset invalidates existing mobile session');
$user->user_pass='hash-before-reset';$transients=[];
for($i=0;$i<5;$i++)C12_Mobile::login(new WP_REST_Request(['email'=>'test@example.com','pin'=>'9999']));
$result=C12_Mobile::login($request);check(is_wp_error($result)&&$result->data['status']===429,'Account locks after five wrong attempts');
function update_user_meta($id,$key,$value){global $meta;$meta[$key]=$value;return true;}
function wp_mail($email,$subject,$body){global $mail;$mail=$body;return true;}
function reset_password($target,$secret){$target->user_pass=hash('sha256',$secret);}
$transients=[];
$response=C12_Mobile::reset_request(new WP_REST_Request(['email'=>$user->user_email]));
check($response instanceof WP_REST_Response,'Reset request gives a neutral response');
preg_match('/code is: ([0-9]{6})/',$mail,$match);$code=$match[1];
check(!str_contains(json_encode($meta),$code),'Recovery code is stored hashed');
$result=C12_Mobile::reset_confirm(new WP_REST_Request(['email'=>$user->user_email,'code'=>'000000','pin'=>'0456']));
check(is_wp_error($result),'Incorrect recovery code is rejected');
$oldPass=$user->user_pass;
$result=C12_Mobile::reset_confirm(new WP_REST_Request(['email'=>$user->user_email,'code'=>$code,'pin'=>'0456']));
check($result instanceof WP_REST_Response && $user->user_pass!==$oldPass,'Valid recovery code updates PIN');
check($user->user_pass===hash('sha256',hash_hmac('sha256',$user->user_email.':0456',wp_salt('auth'))),'Reset preserves the PIN sign-in format');
$result=C12_Mobile::reset_confirm(new WP_REST_Request(['email'=>$user->user_email,'code'=>$code,'pin'=>'0456']));
check(is_wp_error($result),'Recovery code cannot be reused');
$transients=[];C12_Mobile::reset_request(new WP_REST_Request(['email'=>$user->user_email]));
$meta['_c12_reset_code']['expires']=time()-1;preg_match('/code is: ([0-9]{6})/',$mail,$match);
check(is_wp_error(C12_Mobile::reset_confirm(new WP_REST_Request(['email'=>$user->user_email,'code'=>$match[1],'pin'=>'0456']))),'Expired recovery code is rejected');
function get_current_user_id(){return 3;}
function wp_update_user($values){global $user;if($values['ID']!==3)return new WP_Error('bad');$user->display_name=$values['display_name'];return 3;}
check(is_wp_error(C12_Mobile::update_profile(new WP_REST_Request(['name'=>'Buyer']))),'Incomplete profile is rejected');
$details=['name'=>'Buyer Name','phone'=>'+2348012345678','state'=>'Kaduna','town'=>'Zaria','address'=>'10 Market Road'];
check(is_wp_error(C12_Mobile::update_profile(new WP_REST_Request(array_merge($details,['phone'=>'bad'])))),'Invalid phone is rejected');
$result=C12_Mobile::update_profile(new WP_REST_Request(array_merge($details,['ID'=>99,'email'=>'other@example.com','role'=>'administrator'])));
check($result instanceof WP_REST_Response&&$meta['_c12_delivery']===$details,'Delivery details are saved to the authenticated account');
check($user->user_email==='test@example.com'&&$user->ID===3,'Profile input cannot change account identity');
check((array)$result->data['delivery']===$details&&$user->display_name==='Buyer Name','Updated profile returns saved delivery and name');
$all=C12_Locations::all();check(count($all)===37&&array_sum(array_map('count',$all))===774,'All 36 states, FCT and 774 local areas included');
check(C12_Locations::pair('FCT','Bwari')===['state'=>'FCT (Abuja)','town'=>'Bwari'],'FCT aliases resolve to area councils');
check(C12_Locations::pair('Lagos','Zaria')===null,'Local area must belong to selected state');
check(is_wp_error(C12_Mobile::update_profile(new WP_REST_Request(array_merge($details,['state'=>'Lagos','town'=>'Zaria'])))),'Profile rejects mismatched state and LGA');
echo "$checks checks passed.\n";
