<?php
/**
 * Plugin Name: CrackTarget PWA
 * Description: Installable study launcher with network-only account, payment and exam navigation.
 * Version: 1.2.0
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

#ctpwa .ct-focus-title{margin-top:20px}#ctpwa .ct-focus-grid{display:grid;grid-template-columns:1fr;gap:12px}#ctpwa .ct-focus-grid .hero{display:flex;flex-direction:column;align-items:flex-start;min-width:0}#ctpwa .ct-focus-grid .hero a{margin-top:auto}#ctpwa .ct-focus-grid .ct-sao{background:linear-gradient(135deg,#124c43,#16765f)}@media(min-width:600px){#ctpwa .ct-focus-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}

/* Native-style typography and contrast; override inherited WordPress theme colours. */
#ctpwa,#ctpwa-global-nav{font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Arial,sans-serif!important;-webkit-font-smoothing:antialiased}
#ctpwa{--ct-ink:#14233b;--ct-muted:#52647b;background:radial-gradient(ellipse at 0 0,#e9f2ff 0,transparent 48%),#f5f7fb;color:var(--ct-ink);font-size:16px!important;line-height:1.5;min-height:100vh;min-height:100dvh;padding:18px clamp(16px,4vw,32px) calc(94px + env(safe-area-inset-bottom))}
#ctpwa .wrap{max-width:850px}#ctpwa header{padding:6px 0 22px;gap:12px;align-items:center}#ctpwa header img{width:50px;height:50px;object-fit:contain;flex:none;background:#fff;border:1px solid #e0e7ef;padding:4px;border-radius:15px}#ctpwa header strong{font-size:22px!important;line-height:1.2;font-weight:800;letter-spacing:-.6px;color:#14233b!important}#ctpwa header small{font-size:12px!important;line-height:1.45}
#ctpwa .ct-eyebrow{font-size:12px!important;font-weight:750;letter-spacing:1.3px;color:#176b53!important}#ctpwa h1{font-size:clamp(28px,5vw,36px)!important;line-height:1.14!important;font-weight:800!important;letter-spacing:-.85px!important;color:#14233b!important;margin:8px 0!important}#ctpwa h2{font-size:21px!important;line-height:1.3!important;font-weight:750!important;letter-spacing:-.45px!important;color:#14233b!important;margin:25px 0 12px!important}#ctpwa p{font-size:15px!important;line-height:1.55!important;color:#52647b;margin:8px 0 14px}#ctpwa small{font-size:13px!important;line-height:1.45;color:#52647b}
#ctpwa .ct-search{margin:20px 0 24px;gap:8px;align-items:stretch}#ctpwa .ct-search input{font-family:inherit!important;font-size:16px!important;line-height:1.4;min-height:52px;border:1px solid #d3deeb;border-radius:16px;box-shadow:0 3px 14px #122b5010;padding:13px 14px;background:#fff;color:#14233b!important}#ctpwa .ct-search input::placeholder{color:#65758a;opacity:1}#ctpwa .ct-search button{min-height:52px;border-radius:16px;padding:12px 17px;font-weight:750!important;font-size:14px!important;background:#14694c!important;color:#fff!important}
#ctpwa .ct-focus-grid{grid-template-columns:1fr;gap:14px}#ctpwa .ct-focus-grid .hero{position:relative;isolation:isolate;overflow:hidden;min-height:186px;padding:21px 22px;border-radius:23px;background:linear-gradient(125deg,#132b53,#22538c)!important;box-shadow:0 9px 24px #13335b26;border:1px solid #ffffff26;color:#fff!important}#ctpwa .ct-focus-grid .ct-sao{background:linear-gradient(125deg,#10473d,#127c65)!important;box-shadow:0 9px 24px #124c4325}
#ctpwa .hero:after{content:"";position:absolute;z-index:-1;right:-45px;top:-74px;width:190px;height:190px;border-radius:50%;border:30px solid #ffffff0e;pointer-events:none}#ctpwa .hero .ct-tag{display:inline-flex;align-items:center;gap:5px;background:#ffffff20;border:1px solid #ffffff32;padding:5px 10px;border-radius:30px;font-size:11px!important;line-height:1.3!important;font-weight:750;letter-spacing:.65px;color:#fff!important}
#ctpwa .hero h2,#ctpwa .hero h2 a{font-family:inherit!important;color:#fff!important;font-size:clamp(24px,4.5vw,31px)!important;line-height:1.15!important;font-weight:800!important;letter-spacing:-.55px!important;margin:13px 0 5px!important;opacity:1!important;text-shadow:0 1px 2px #00000024}#ctpwa .hero p{font-size:14px!important;color:#f0f6ff!important;line-height:1.45!important;margin:5px 0 16px!important;max-width:35ch}#ctpwa .hero a{background:#fff!important;color:#14345a!important;border:1px solid #fff!important;min-height:46px;border-radius:13px;padding:10px 15px;font-size:14px!important;font-weight:750!important;box-shadow:0 2px 9px #00000015}#ctpwa .ct-sao a{color:#105b47!important}
#ctpwa .grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}#ctpwa .tile{min-height:132px;gap:7px;border:1px solid #e0e7ef;padding:15px;border-radius:20px;background:#fff!important;box-shadow:0 4px 14px #182b4510;color:#14233b!important;position:relative;justify-content:flex-start}#ctpwa .tile b{font-size:16px!important;line-height:1.3;font-weight:750!important;letter-spacing:-.15px;color:#14233b!important}#ctpwa .tile small{font-size:12.5px!important;line-height:1.4;color:#52647b!important}#ctpwa .sym{width:42px;height:42px;flex:none;margin-bottom:3px;border-radius:13px}#ctpwa .sym svg{width:23px;height:23px;display:block}#ctpwa .tile:nth-child(3n+2) .sym{background:#e7efff;color:#2554a4}#ctpwa .tile:nth-child(3n) .sym{background:#fff0dc;color:#925100}
#ctpwa .install{padding:17px 18px;border-radius:19px;background:#fff;border-color:#dfe7f0;margin-top:20px}#ctpwa .install b{font-size:16px;font-weight:750;color:#14233b}#ctpwa .install p{font-size:14px!important}#ctpwa .install small{font-size:13px!important}#ctpwa .install button{min-height:46px;font-weight:750}#ctpwa .wrap>p:last-child small{font-size:12px!important}
#ctpwa nav,#ctpwa-global-nav{background:rgba(255,255,255,.98)!important;border-top:1px solid #dce5ee!important;box-shadow:0 -6px 24px #102b5010!important;padding:7px 7px calc(7px + env(safe-area-inset-bottom))!important}#ctpwa nav a,#ctpwa-global-nav a{font-family:inherit!important;font-size:12px!important;font-weight:650!important;line-height:1.2!important;min-height:52px;gap:5px;color:#52647b!important;justify-content:center;text-decoration:none!important}#ctpwa nav a:first-child,#ctpwa-global-nav a[aria-current=page]{color:#126b4c!important}#ctpwa nav a span,#ctpwa-global-nav a span{display:grid;place-items:center;width:36px;height:28px;border-radius:12px}#ctpwa nav a:first-child span,#ctpwa-global-nav a[aria-current=page] span{background:#e2f3eb}#ctpwa nav svg,#ctpwa-global-nav svg{width:23px;height:23px;display:block}
@media(min-width:760px) and (pointer:fine){#ctpwa .ct-focus-grid{grid-template-columns:repeat(2,minmax(0,1fr))}#ctpwa .grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(min-width:768px) and (pointer:coarse){#ctpwa .ct-focus-grid{grid-template-columns:repeat(2,minmax(0,1fr))}#ctpwa .grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:480px){#ctpwa{padding-left:16px;padding-right:16px}#ctpwa .hero h2{font-size:25px!important}#ctpwa .tile{min-height:126px}}
@media(prefers-reduced-motion:no-preference){#ctpwa .tile,#ctpwa .hero a{transition:transform .16s,box-shadow .16s}#ctpwa .tile:active,#ctpwa .hero a:active{transform:scale(.98)}}

@media(max-width:1023px){#ctpwa-mobile-home.ctpwa-phone #ctpwa .ct-focus-grid{grid-template-columns:1fr}#ctpwa-mobile-home.ctpwa-phone #ctpwa .grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
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
function ctpwa_svg($symbol){
 $icons=array(
 '⌂'=>'<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"/>',
 '✓'=>'<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2h6v2M9 13l2 2 4-5"/>',
 '▤'=>'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h4v4H7zM14 8h3M14 12h3M7 16h10"/>',
 '▥'=>'<path d="M12 6v14M3 5c4-1 6 0 9 2 3-2 5-3 9-2v14c-4-1-6 0-9 2-3-2-5-3-9-2z"/>',
 '≡'=>'<rect x="7" y="3" width="13" height="17" rx="2"/><path d="M4 7v14M10 8h7M10 12h7M10 16h5"/>',
 '◈'=>'<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V4h8v3M3 12h18M10 12v2h4v-2"/>',
 '▣'=>'<path d="M4 4h4v16H4zM10 4h4v16h-4zM16 5l3-1 4 15-3 1z"/>',
 '◉'=>'<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>'
 );
 return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'.($icons[$symbol]??$icons['⌂']).'</svg>';
}
function ctpwa_tile($path,$symbol,$title,$desc){echo '<a class="tile" href="'.esc_url(ctpwa_url($path)).'"><span class="sym" aria-hidden="true">'.ctpwa_svg($symbol).'</span><b>'.esc_html($title).'</b><small>'.esc_html($desc).'</small></a>';}
function ctpwa_dashboard(){ ?><main id="ctpwa"><div class="wrap"><header><?php if(ctpwa_icon(192)): ?><img src="<?php echo esc_url(ctpwa_icon(192)); ?>" alt=""><?php endif; ?><div><strong>CrackTarget</strong><small>Every Question Has a Purpose.</small></div></header><small class="ct-eyebrow">YOUR STUDY SPACE</small><h1>Hello, Aspirant!</h1><p>Pick your next step. Learn, practise and revise.</p><form class="ct-search" action="<?php echo esc_url(home_url('/')); ?>" method="get" role="search"><label for="ct-app-search" class="ct-sr">Search CrackTarget</label><input id="ct-app-search" name="s" type="search" placeholder="Search exams, notes, tests &amp; books" required><button type="submit">Search</button></form><h2 class="ct-focus-title">Exam Focus</h2><div class="ct-focus-grid"><section class="hero"><small class="ct-tag">UPSC RECRUITMENT</small><h2>UPSC EPFO APFC</h2><p>Exam-focused study notes, PYQs and mock tests.</p><a href="<?php echo esc_url(ctpwa_url('upsc-epfo/')); ?>">Explore APFC &rarr;</a></section><section class="hero ct-sao"><small class="ct-tag">UPSC RECRUITMENT</small><h2>DRDO SAO Grade-II</h2><p>Senior Administrative Officer preparation, study materials, PYQs and mock tests.</p><a href="<?php echo esc_url(ctpwa_url('senior-administrative-officer-grade-ii-in-defence-research-development-organization-drdo/')); ?>">Explore DRDO SAO &rarr;</a></section></div><h2>Explore &amp; learn</h2><div class="grid"><?php ctpwa_tile('mocktest/','✓','Mock Tests','Practice by exam');ctpwa_tile('current-affairs/','▤','Current Affairs','Daily updates');ctpwa_tile('study-directory/','▥','All Exams & Study Notes','Subjects and exams');ctpwa_tile('previous-years-questions/','≡','Previous Papers','Solved PYQs');ctpwa_tile('vacancies/','◈','Latest Vacancies','Recruitment updates');ctpwa_tile('shop/','▣','Digital Books','Explore the store'); ?></div><h2>Your account</h2><div class="grid"><?php ctpwa_tile('my-account/','◉','My Account','Sign in, orders and downloads');ctpwa_tile('?ct_full_site=1','⌂','Full Website','Open the original website'); ?></div><section id="ctpwa-install-panel" class="install"><b>Keep CrackTarget on your home screen</b><p>Open your study space in one tap.</p><button id="ctpwa-install" type="button" hidden>Install CrackTarget</button><p id="ctpwa-install-note"><small>Android: Chrome menu &rarr; Add to Home screen / Install app. iPhone: Safari Share &rarr; Add to Home Screen.</small></p></section><p><small>Internet is required for learning, sign-in, tests and payments. <a href="<?php echo esc_url(ctpwa_url('privacy-policy/')); ?>">Privacy policy</a>.</small></p></div><nav aria-label="Study app navigation"><?php foreach(array('ct-app/'=>array('⌂','Home'),'mocktest/'=>array('✓','Tests'),'current-affairs/'=>array('▤','Daily CA'),'study-directory/'=>array('▥','Library'),'my-account/'=>array('◉','Account')) as $p=>$v){echo '<a href="'.esc_url(ctpwa_url($p)).'"><span aria-hidden="true">'.ctpwa_svg($v[0]).'</span>'.esc_html($v[1]).'</a>';} ?></nav></main><?php }
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
add_action('wp_head',function(){if(!ctpwa_home_enabled())return;ctpwa_head();echo '<style>#ctpwa-mobile-home{display:none}@media(max-width:1023px){#ctpwa-mobile-home{display:block}body.ctpwa-clean-home > .ctpwa-legacy-shell{display:none!important}body.ctpwa-clean-home{margin:0!important;min-height:100vh;overflow-x:hidden;background:#f5f7fb}body.ctpwa-clean-home #ctpwa-mobile-home{isolation:auto!important;position:relative;z-index:auto!important}body.ctpwa-clean-home #ctpwa-mobile-home #ctpwa{min-height:100vh;padding-bottom:calc(92px + env(safe-area-inset-bottom))}body.ctpwa-clean-home #ctpwa-mobile-home #ctpwa nav{position:fixed!important;z-index:9999!important;bottom:0!important;left:0!important;right:0!important}body.ctpwa-clean-home #ctpwa-mobile-home #ctpwa nav a{white-space:nowrap}body.ctpwa-clean-home #ctpwa-mobile-home #ctpwa .hero h2{line-height:1.2}}</style>';},20);
add_action('wp_body_open','ctpwa_home_markup',1);
add_action('wp_footer',function(){
 if(ctpwa_home_enabled()){
   ctpwa_home_markup();ctpwa_assets();?>
<script>(function(){
 const el=document.getElementById('ctpwa-mobile-home');if(!el)return;
 if(/; wv\)|CrackTargetAndroid/i.test(navigator.userAgent)){el.hidden=true;return;}
 const bar=document.getElementById('wpadminbar');
 if(bar&&bar.parentNode===document.body)bar.after(el);else document.body.prepend(el);
 const mq=matchMedia('(max-width: 1023px)');
 const phone=matchMedia('(pointer: coarse)').matches&&Math.min(screen.width,screen.height)<=600;
 if(phone)el.classList.add('ctpwa-phone');
 const shellSelector='main,header,footer,#page,#site,#content,#masthead,#colophon,.site,.site-content,.wp-site-blocks,.site-header,.site-footer';
 const excluded='script,style,link,template,noscript,dialog,[role="dialog"],[aria-live],#wpadminbar,[id*="cookie" i],[class*="cookie" i],[id*="consent" i],[class*="consent" i]';
 function sync(){
   const mobile=mq.matches;
   for(const child of Array.from(document.body.children)){
     if(child===el||child.matches(excluded))continue;
     const containsOldHeading=Array.from(child.querySelectorAll('h1,h2')).some(h=>/Exam guides, books and mock tests|Your preparation starts with a clear destination|Featured products/.test(h.textContent));
     const legacy=child.matches(shellSelector)||containsOldHeading||!!child.querySelector('main, #masthead, .site-header, .wp-site-blocks');
     if(legacy)child.classList.add('ctpwa-legacy-shell');
   }
   document.body.classList.toggle('ctpwa-clean-home',mobile);
   for(const child of document.querySelectorAll('body > .ctpwa-legacy-shell')){
     if(mobile&&!child.hasAttribute('inert')){child.setAttribute('inert','');child.dataset.ctpwaInert='1';}
     if(!mobile&&child.dataset.ctpwaInert==='1'){child.removeAttribute('inert');delete child.dataset.ctpwaInert;}
   }
 }
 sync();mq.addEventListener?mq.addEventListener('change',sync):mq.addListener(sync);
 window.addEventListener('pageshow',sync);window.addEventListener('load',sync);
})();</script>
<?php return;
 }
 if(!ctpwa_nav_enabled())return;
 ctpwa_assets();
 echo '<nav id="ctpwa-global-nav" aria-label="CrackTarget quick navigation" hidden>';
 foreach(array(''=>array('⌂','Home'),'mocktest/'=>array('✓','Tests'),'current-affairs/'=>array('▤','Daily CA'),'study-directory/'=>array('▥','Library'),'my-account/'=>array('◉','Account')) as $p=>$v){
  $current=ctpwa_path()===untrailingslashit((string)wp_parse_url(ctpwa_url($p),PHP_URL_PATH));
  echo '<a href="'.esc_url(ctpwa_url($p)).'"'.($current?' aria-current="page"':'').'><span aria-hidden="true">'.ctpwa_svg($v[0]).'</span>'.esc_html($v[1]).'</a>';
 }
 echo '</nav>';?>
<script>(function(){const n=document.getElementById('ctpwa-global-nav');if(!n)return;const media=matchMedia('(max-width: 1023px)');const sync=()=>{const active=media.matches&&!document.fullscreenElement&&!document.webkitFullscreenElement&&!document.querySelector('body.test-active,body.quiz-active,body.exam-active,[aria-modal="true"]');n.hidden=!active;document.body.classList.toggle('ctpwa-nav-visible',active);};sync();media.addEventListener?media.addEventListener('change',sync):media.addListener(sync);document.addEventListener('fullscreenchange',sync);document.addEventListener('webkitfullscreenchange',sync);})();</script>
<?php
},30);
