<?php
/**
 * Plugin Name: CrackTarget PWA
 * Description: Installable study launcher with network-only account, payment and exam navigation.
 * Version: 1.1.0
 * Author: CrackTarget
 */
defined('ABSPATH') || exit;
function ctpwa_url($path='') { return home_url('/'.$path); }
function ctpwa_path() { return untrailingslashit((string)wp_parse_url($_SERVER['REQUEST_URI'] ?? '/',PHP_URL_PATH)); }
function ctpwa_endpoint($name) { return add_query_arg('ct_pwa',$name,home_url('/')); }
function ctpwa_icon($size) { return get_site_icon_url($size); }
function ctpwa_head() {
  echo '<link rel="manifest" href="'.esc_url(ctpwa_endpoint('manifest')).'"><meta name="theme-color" content="#102b50"><meta name="apple-mobile-web-app-capable" content="yes">';
  if(ctpwa_icon(192)) echo '<link rel="apple-touch-icon" href="'.esc_url(ctpwa_icon(192)).'">';
}
function ctpwa_assets() { ?>
<style>
#ctpwa{font:16px/1.45 system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#102b50;background:#f5f7fb;min-height:100vh;box-sizing:border-box;padding:16px 16px calc(92px + env(safe-area-inset-bottom));}#ctpwa *{box-sizing:border-box}#ctpwa a{text-decoration:none;color:inherit}#ctpwa .wrap{max-width:860px;margin:auto}#ctpwa header{display:flex;align-items:center;gap:12px;padding:8px 0 22px}#ctpwa header img{width:46px;height:46px;border-radius:12px}#ctpwa header strong{font-size:21px;display:block}#ctpwa small{font-size:12px;color:#52647d}#ctpwa h1{font-size:27px;letter-spacing:-.6px;margin:8px 0}#ctpwa h2{font-size:18px;margin:24px 0 12px}#ctpwa p{margin:7px 0 14px}#ctpwa .hero{padding:23px;border-radius:24px;background:linear-gradient(135deg,#102b50,#154b78);color:#fff;box-shadow:0 12px 28px #102b5025}#ctpwa .hero small{color:#fed39b;font-weight:700;letter-spacing:.08em}#ctpwa .hero h2{font-size:25px;margin:8px 0}#ctpwa .hero a,#ctpwa button{display:inline-flex;align-items:center;justify-content:center;border:0;border-radius:14px;padding:12px 16px;min-height:44px;font:inherit;font-weight:700;background:#fff;color:#102b50;cursor:pointer}#ctpwa .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}#ctpwa .tile{display:flex;flex-direction:column;gap:7px;padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:20px;box-shadow:0 5px 18px #102b500a;min-height:116px}#ctpwa .tile b{font-size:15px}#ctpwa .sym{width:40px;height:40px;display:grid;place-items:center;border-radius:12px;background:#e2f3eb;color:#126c49;font-size:21px}#ctpwa .tile:nth-child(3n+2) .sym{background:#e6effe;color:#2355a5}#ctpwa .tile:nth-child(3n) .sym{background:#fff0dc;color:#9a5300}#ctpwa .install{margin-top:18px;border:1px solid #d9e6df;background:#fff;border-radius:18px;padding:15px}#ctpwa .install button{background:#146c47;color:#fff}#ctpwa nav{position:fixed;bottom:0;left:0;right:0;z-index:10;display:flex;justify-content:center;background:rgba(255,255,255,.97);border-top:1px solid #e2e8f0;box-shadow:0 -6px 20px #102b500b;padding:8px 4px calc(8px + env(safe-area-inset-bottom));}#ctpwa nav a{flex:1;max-width:130px;display:flex;flex-direction:column;align-items:center;gap:3px;font-size:11px;font-weight:650;color:#52647d;min-height:44px}#ctpwa nav a:first-child{color:#146c47}#ctpwa nav span{font-size:20px}#ctpwa a:focus-visible,#ctpwa button:focus-visible{outline:3px solid #eb961e;outline-offset:3px}#ctpwa [hidden]{display:none!important}@media(min-width:700px){#ctpwa .grid{grid-template-columns:repeat(3,minmax(0,1fr))}#ctpwa{padding:28px 28px 100px}}@media(prefers-reduced-motion:no-preference){#ctpwa .tile{transition:transform .15s}#ctpwa .tile:active{transform:scale(.98)}}#ctpwa-launch{display:none}@media(max-width:767px){#ctpwa-launch{display:block;margin:20px auto;padding:16px;max-width:420px;text-align:center;background:#f5f7fb;border-radius:16px;border:1px solid #dce5ef}#ctpwa-launch a{display:inline-block;padding:12px 18px;background:#146c47;color:#fff;border-radius:12px;font-weight:700;text-decoration:none}}

#ctpwa .ct-search{display:flex;gap:8px;margin:16px 0 18px}#ctpwa .ct-search input{min-width:0;flex:1;border:1px solid #cbd5e1;border-radius:13px;background:white;padding:12px;color:#102b50;font:inherit}#ctpwa .ct-search button{background:#146c47;color:#fff}#ctpwa .ct-sr{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}#ctpwa-mobile-home,#ctpwa-global-nav{display:none}
@media(max-width:1023px){#ctpwa-mobile-home{display:block;position:relative;isolation:isolate;width:100%;margin:0;padding:0}#ctpwa-mobile-home #ctpwa{min-height:0;padding-bottom:calc(84px + env(safe-area-inset-bottom))}#ctpwa-mobile-home #ctpwa nav{z-index:9990}#ctpwa-global-nav{display:flex;position:fixed;bottom:0;left:0;right:0;justify-content:center;z-index:9990;background:rgba(255,255,255,.98);border-top:1px solid #dbe4ef;box-shadow:0 -5px 24px #102b5012;padding:7px 4px calc(7px + env(safe-area-inset-bottom));font:600 11px/1.35 system-ui,sans-serif}#ctpwa-global-nav a{display:flex;flex:1;max-width:135px;min-height:44px;flex-direction:column;align-items:center;justify-content:center;gap:3px;color:#43556e;text-decoration:none}#ctpwa-global-nav a[aria-current=page]{color:#146c47}#ctpwa-global-nav span{font-size:20px;line-height:1}#ctpwa-global-nav a:focus-visible{outline:3px solid #e49b31;outline-offset:-3px}body.ctpwa-nav-visible{padding-bottom:calc(70px + env(safe-area-inset-bottom))}#ctpwa-global-nav[hidden],#ctpwa-mobile-home[hidden]{display:none!important}}
@media print{#ctpwa-mobile-home,#ctpwa-global-nav{display:none!important}}

</style>
<script>
(function(){
 let promptEvent=null;
 const isNative=/; wv\)|CrackTargetAndroid/i.test(navigator.userAgent);
 const installed=()=>matchMedia('(display-mode: standalone)').matches || navigator.standalone===true;
 const sync=()=>{const b=document.getElementById('ctpwa-install');const note=document.getElementById('ctpwa-install-note');const panel=document.getElementById('ctpwa-install-panel');if(panel)panel.hidden=installed()||isNative;if(b)b.hidden=!promptEvent;if(note)note.hidden=!!promptEvent;};
 window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();promptEvent=e;sync();});
 window.addEventListener('appinstalled',()=>{promptEvent=null;sync();});
 document.addEventListener('DOMContentLoaded',()=>{sync();const b=document.getElementById('ctpwa-install');if(b)b.addEventListener('click',async()=>{if(!promptEvent)return;try{await promptEvent.prompt();}finally{promptEvent=null;sync();}});});
 if(!isNative&&'serviceWorker' in navigator&&window.isSecureContext&&(location.pathname==='/'||location.pathname==='/ct-app/')){
   window.addEventListener('load',async()=>{try{const old=await navigator.serviceWorker.getRegistration(<?php echo wp_json_encode(ctpwa_url('ct-app/')); ?>);const worker=old&&(old.active||old.waiting||old.installing);const url=<?php echo wp_json_encode(ctpwa_endpoint('worker')); ?>;if(worker&&worker.scriptURL!==url)return;await navigator.serviceWorker.register(url,{scope:<?php echo wp_json_encode(ctpwa_url()); ?>});}catch(e){console.warn('CrackTarget install unavailable',e);}});
 }
})();
</script>
<?php }
function ctpwa_tile($path,$symbol,$title,$desc){echo '<a class="tile" href="'.esc_url(ctpwa_url($path)).'"><span class="sym" aria-hidden="true">'.esc_html($symbol).'</span><b>'.esc_html($title).'</b><small>'.esc_html($desc).'</small></a>';}
function ctpwa_dashboard(){ ?><main id="ctpwa"><div class="wrap"><header><?php if(ctpwa_icon(192)): ?><img src="<?php echo esc_url(ctpwa_icon(192)); ?>" alt=""><?php endif; ?><div><strong>CrackTarget</strong><small>Every Question Has a Purpose.</small></div></header><small>YOUR STUDY SPACE</small><h1>Hello, Aspirant!</h1><p>Pick your next step. Learn, practise and revise.</p><form class="ct-search" action="<?php echo esc_url(home_url('/')); ?>" method="get" role="search"><label for="ct-app-search" class="ct-sr">Search CrackTarget</label><input id="ct-app-search" name="s" type="search" placeholder="Search exams, notes, tests &amp; books" required><button type="submit">Search</button></form><section class="hero"><small>EXAM FOCUS</small><h2>UPSC EPFO APFC</h2><p>Prepare with exam-focused notes and practice.</p><a href="<?php echo esc_url(ctpwa_url('upsc-epfo/')); ?>">Explore APFC preparation &rarr;</a></section><h2>Explore &amp; learn</h2><div class="grid"><?php ctpwa_tile('mocktest/','✓','Mock Tests','Practice by exam');ctpwa_tile('current-affairs/','▤','Current Affairs','Daily updates');ctpwa_tile('study-directory/','▥','All Exams & Study Notes','Subjects and exams');ctpwa_tile('previous-years-questions/','≡','Previous Papers','Solved PYQs');ctpwa_tile('vacancies/','◈','Latest Vacancies','Recruitment updates');ctpwa_tile('shop/','▣','Digital Books','Explore the store'); ?></div><h2>Your account</h2><div class="grid"><?php ctpwa_tile('my-account/','◉','My Account','Sign in, orders and downloads');ctpwa_tile('?ct_full_site=1','⌂','Full Website','Explore all resources'); ?></div><section id="ctpwa-install-panel" class="install"><b>Keep CrackTarget on your home screen</b><p>Open your study space in one tap.</p><button id="ctpwa-install" type="button" hidden>Install CrackTarget</button><p id="ctpwa-install-note"><small>Android: Chrome menu &rarr; Add to Home screen / Install app. iPhone: Safari Share &rarr; Add to Home Screen.</small></p></section><p><small>Internet is required for learning, sign-in, tests and payments. <a href="<?php echo esc_url(ctpwa_url('privacy-policy/')); ?>">Privacy policy</a>.</small></p></div><nav aria-label="Study app navigation"><?php foreach(array('ct-app/'=>array('⌂','Home'),'mocktest/'=>array('✓','Tests'),'current-affairs/'=>array('▤','Daily CA'),'study-directory/'=>array('▥','Library'),'my-account/'=>array('◉','Account')) as $p=>$v){echo '<a href="'.esc_url(ctpwa_url($p)).'"><span aria-hidden="true">'.esc_html($v[0]).'</span>'.esc_html($v[1]).'</a>';} ?></nav></main><?php }
function ctpwa_render(){
  status_header(200);nocache_headers();header('Content-Type: text/html; charset=UTF-8');header('X-Robots-Tag: noindex, follow');
  ?><!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"><title>CrackTarget | Study App</title><?php ctpwa_head();ctpwa_assets(); ?></head><body style="margin:0"><?php ctpwa_dashboard(); ?></body></html><?php exit;
}
add_action('template_redirect',function(){
 if(!in_array($_SERVER['REQUEST_METHOD']??'GET',array('GET','HEAD'),true))return;
 $endpoint=isset($_GET['ct_pwa'])&&is_string($_GET['ct_pwa'])?$_GET['ct_pwa']:'';
 if(ctpwa_path()===untrailingslashit((string)wp_parse_url(home_url('/'),PHP_URL_PATH))){
 if($endpoint==='manifest'){
  status_header(200);nocache_headers();header('Content-Type: application/manifest+json; charset=UTF-8');header('X-Content-Type-Options: nosniff');
  $icons=array();foreach(array(192,512) as $size){$url=ctpwa_icon($size);if($url)$icons[]=array('src'=>$url,'sizes'=>$size.'x'.$size,'purpose'=>'any');}
  echo wp_json_encode(array('id'=>ctpwa_url('ct-app/'),'name'=>'CrackTarget: Exam Prep','short_name'=>'CrackTarget','start_url'=>ctpwa_url('ct-app/'),'scope'=>ctpwa_url(),'display'=>'standalone','background_color'=>'#f5f7fb','theme_color'=>'#102b50','lang'=>'en-IN','icons'=>$icons),JSON_UNESCAPED_SLASHES);exit;
 }
 if($endpoint==='worker'){
  status_header(200);nocache_headers();header('Content-Type: application/javascript; charset=UTF-8');header('X-Content-Type-Options: nosniff');header('Service-Worker-Allowed: /');
  $path=wp_parse_url(ctpwa_url('ct-app/'),PHP_URL_PATH);
  echo 'const appPath='.wp_json_encode($path).';self.addEventListener("install",()=>self.skipWaiting());self.addEventListener("activate",e=>e.waitUntil(self.clients.claim()));self.addEventListener("fetch",e=>{const u=new URL(e.request.url);if(e.request.method!=="GET"||e.request.mode!=="navigate"||u.origin!==self.location.origin||u.pathname!==appPath||u.search)return;e.respondWith(fetch(e.request,{cache:"no-store"}).catch(()=>new Response("<!doctype html><meta name=viewport content=width=device-width,initial-scale=1><title>CrackTarget offline</title><main style=font-family:system-ui;padding:24px><h1>You are offline</h1><p>Reconnect to continue studying on CrackTarget.</p><a href=\\"./\\">Try again</a></main>",{headers:{"Content-Type":"text/html; charset=utf-8","Cache-Control":"no-store"}})));});';exit;
 }}
 if(ctpwa_path()===untrailingslashit((string)wp_parse_url(ctpwa_url('ct-app/'),PHP_URL_PATH)))ctpwa_render();
},0);

function ctpwa_native_request(){return (bool)preg_match('/; wv\)|CrackTargetAndroid/i',$_SERVER['HTTP_USER_AGENT']??'');}
function ctpwa_home_enabled(){
 return is_front_page()&&!is_search()&&!is_feed()&&!is_preview()&&!is_customize_preview()&&!is_404()&&!isset($_GET['ct_full_site'])&&!isset($_GET['ct_pwa'])&&!ctpwa_native_request();
}
function ctpwa_home_markup(){
 static $done=false;
 if($done||!ctpwa_home_enabled())return;
 $done=true;
 echo '<div id="ctpwa-mobile-home">';
 ctpwa_dashboard();
 echo '</div>';
}
function ctpwa_nav_enabled(){
 if(is_admin()||is_feed()||is_preview()||is_customize_preview()||is_404()||ctpwa_native_request()||isset($_GET['ct_full_site'])||is_front_page())return false;
 if(function_exists('is_cart')&&(is_cart()||is_checkout()||is_account_page()))return false;
 if(!empty($_GET)&&!is_search())return false;
 $p=ctpwa_path();
 if(preg_match('~(?:checkout|cart|my-account|payment|order|login|register|test|mock|quiz|exam-attempt|diagnostic|readiness|practice|assessment|result|download)~i',$p)&&$p!=='/mocktest')return false;
 if(is_search()||is_category()||is_tag())return true;
 if(in_array($p,array('/mocktest','/study-directory','/current-affairs','/previous-years-questions','/vacancies','/shop'),true))return true;
 if(is_singular('post')||is_page()){
  $post=get_queried_object();
  if(!$post instanceof WP_Post)return false;
  $content=(string)$post->post_content;
  if(preg_match('~\[(?!/?(?:caption|gallery|audio|video|embed)\b)[a-z][^\]]*\]|<script\b|<iframe\b|<form\b|timer|start\s+(?:the\s+)?(?:mock\s+)?test|submit\s+(?:the\s+)?(?:test|quiz)|question\s+palette~i',$content))return false;
  return is_singular('post')||strpos($p,'/study-directory/')===0;
 }
 return function_exists('is_product')&&is_product();
}
add_action('wp_head',function(){if(ctpwa_home_enabled())ctpwa_head();},20);
add_action('wp_body_open','ctpwa_home_markup',1);
add_action('wp_footer',function(){
 if(ctpwa_home_enabled()){
   ctpwa_home_markup();ctpwa_assets();?>
<script>(function(){const el=document.getElementById('ctpwa-mobile-home');if(!el)return;const native=/; wv\)|CrackTargetAndroid/i.test(navigator.userAgent);if(native){el.hidden=true;return;}if(el.parentNode!==document.body||el!==document.body.firstElementChild){const bar=document.getElementById('wpadminbar');if(bar&&bar.parentNode===document.body)bar.after(el);else document.body.prepend(el);} })();</script>
<?php return;
 }
 if(!ctpwa_nav_enabled())return;
 ctpwa_assets();
 echo '<nav id="ctpwa-global-nav" aria-label="CrackTarget quick navigation" hidden>';
 foreach(array(''=>array('⌂','Home'),'mocktest/'=>array('✓','Tests'),'current-affairs/'=>array('▤','Daily CA'),'study-directory/'=>array('▥','Library'),'my-account/'=>array('◉','Account')) as $p=>$v){
  $current=ctpwa_path()===untrailingslashit((string)wp_parse_url(ctpwa_url($p),PHP_URL_PATH));
  echo '<a href="'.esc_url(ctpwa_url($p)).'"'.($current?' aria-current="page"':'').'><span aria-hidden="true">'.esc_html($v[0]).'</span>'.esc_html($v[1]).'</a>';
 }
 echo '</nav>';?>
<script>(function(){const n=document.getElementById('ctpwa-global-nav');if(!n)return;const media=matchMedia('(max-width: 1023px)');const sync=()=>{const active=media.matches&&!document.fullscreenElement&&!document.webkitFullscreenElement&&!document.querySelector('body.test-active,body.quiz-active,body.exam-active,[aria-modal="true"]');n.hidden=!active;document.body.classList.toggle('ctpwa-nav-visible',active);};sync();media.addEventListener?media.addEventListener('change',sync):media.addListener(sync);document.addEventListener('fullscreenchange',sync);document.addEventListener('webkitfullscreenchange',sync);})();</script>
<?php
},30);
