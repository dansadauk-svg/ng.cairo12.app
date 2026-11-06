<?php
// Isolated gateway tests: no external requests and no real payments.
define('ABSPATH',__DIR__);define('MINUTE_IN_SECONDS',60);
class WP_Error {function __construct(public $code,public $message='',public $data=[]) {}}
class WP_REST_Response {function __construct(public $data){}function header($k,$v){}}
class Request {function __construct(public $data=[],public $body='',public $signature=''){}function get_param($k){return $this->data[$k]??null;}function get_json_params(){return $this->data;}function get_body(){return $this->body;}function get_header($k){return $this->signature;}}
class C12_API {static function error($m,$s=400){return new WP_Error('error',$m,['status'=>$s]);}}
class C12_Core {static $row;static $changes=0;static function order($id){return self::$row&&self::$row['id']==$id?self::$row:null;}static function change($row,$data,$event){self::$row['data']=$data;self::$changes++;return true;}}
class DB {function prepare($s,...$a){return $s;}function get_var($s){return 1;}}
$wpdb=new DB();$options=[];$transients=[];$uid=3;$transaction=[];$mail=[];$checks=0;
function get_current_user_id(){global $uid;return $uid;}function wp_get_current_user(){return (object)['user_email'=>'buyer@example.com'];}function wp_salt($t){return 'salt';}
function get_option($k,$d=false){global $options;return $options[$k]??$d;}function update_option($k,$v,$a=false){global $options;$options[$k]=$v;return true;}function add_option($k,$v,$d='',$a=false){global $options;if(isset($options[$k]))return false;$options[$k]=$v;return true;}
function get_transient($k){global $transients;return $transients[$k]??0;}function set_transient($k,$v,$t){global $transients;$transients[$k]=$v;}
function is_wp_error($v){return $v instanceof WP_Error;}function wp_remote_get(...$a){return 'response';}function wp_remote_retrieve_response_code($r){return 200;}function wp_remote_retrieve_body($r){global $transaction;return json_encode(['status'=>true,'data'=>$transaction]);}function wp_mail(...$args){global $mail;$mail[]=$args;}
function check($b,$name){global $checks;if(!$b)throw new Exception($name);$checks++;echo "PASS $name\n";}
require $argv[1];
C12_Paystack::save_key('sk_test_123');update_option('c12_paystack_public','pk_test_123');update_option('c12_paystack_enabled',true);
check(C12_Paystack::enabled(),'Matching keys enable checkout');update_option('c12_paystack_public','pk_live_123');check(!C12_Paystack::enabled(),'Mixed test and live keys disable checkout');update_option('c12_paystack_public','pk_test_123');
function fresh(){C12_Core::$row=['id'=>1,'user_id'=>3,'reference'=>'ORDER1','data'=>['product_payment'=>['status'=>'unpaid','amount'=>10000,'issued'=>time()-10,'expires'=>time()+600]]];}
fresh();$uid=4;check(is_wp_error(C12_Paystack::start(new Request(['id'=>1,'kind'=>'product']))),'Cannot pay another customer order');$uid=3;
$start=C12_Paystack::start(new Request(['id'=>1,'kind'=>'product']));check($start instanceof WP_REST_Response,'Start returns payment options');$ref=$start->data['reference'];check(!str_contains(json_encode($start->data),'sk_test'),'Secret key never returned');
$transaction=['status'=>'success','reference'=>$ref,'currency'=>'NGN','amount'=>10000,'customer'=>['email'=>'buyer@example.com'],'domain'=>'test','paid_at'=>gmdate('c'),'id'=>42];
$uid=4;check(is_wp_error(C12_Paystack::verify(new Request(['reference'=>$ref]))),'Cannot verify another customer reference');$uid=3;
$transaction['amount']=1;check(is_wp_error(C12_Paystack::verify(new Request(['reference'=>$ref]))),'Wrong amount rejected');$transaction['amount']=10000;$transaction['currency']='USD';check(is_wp_error(C12_Paystack::verify(new Request(['reference'=>$ref]))),'Wrong currency rejected');$transaction['currency']='NGN';
check(C12_Paystack::verify(new Request(['reference'=>$ref]))===['ok'=>true]&&C12_Core::$row['data']['product_payment']['status']==='paid','Verified payment marks order paid');$changes=C12_Core::$changes;check(C12_Paystack::verify(new Request(['reference'=>$ref]))===['ok'=>true]&&C12_Core::$changes===$changes,'Repeated verification is idempotent');
check(is_wp_error(C12_Paystack::webhook(new Request([], '{}','invalid'))),'Forged webhook rejected');
fresh();C12_Core::$row['data']['product_payment']['issued']=1;check(is_wp_error(C12_Paystack::verify(new Request(['reference'=>$ref])))&&C12_Core::$row['data']['product_payment']['status']==='unpaid','Changed quote held for review');
fresh();$body=json_encode(['event'=>'charge.success','data'=>['reference'=>$ref]]);$hook=new Request(json_decode($body,true),$body,hash_hmac('sha512',$body,'sk_test_123'));check(C12_Paystack::webhook($hook)===['ok'=>true],'Signed webhook verifies and settles payment');
echo "$checks checks passed.\n";
