(function () {
  'use strict';
  var data = window.ADMISSIONS;
  var $ = function (id) { return document.getElementById(id); };
  var bn = function (n) { return String(n).replace(/\d/g, function (d) { return '০১২৩৪৫৬৭৮৯'[d]; }); };
  var escape = function (s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); };
  function get(key, fallback) { try { return localStorage.getItem(key) || fallback; } catch (e) { return fallback; } }
  function set(key, val) { try { localStorage.setItem(key, val); } catch (e) {} }
  function exact(s) { return !!s && s.indexOf('T') !== -1; }
  function stamp(day, time) { return day && time ? day + 'T' + time + ':00+06:00' : null; }
  function dayOf(now) { return new Date(now.getTime() + 21600000).toISOString().slice(0, 10); }
  function date(s, withTime) {
    if (!s) return 'এখনও নিশ্চিত নয়';
    var d = new Date(exact(s) ? s : s + 'T12:00:00+06:00');
    if (!isFinite(d.getTime())) return 'এখনও নিশ্চিত নয়';
    var out = d.toLocaleDateString('bn-BD', {timeZone:'Asia/Dhaka', day:'numeric',month:'long',year:'numeric'});
    if (withTime && exact(s)) out += ', ' + d.toLocaleTimeString('bn-BD', {timeZone:'Asia/Dhaka',hour:'numeric',minute:'2-digit',hour12:true});
    return out;
  }
  function time(t) { if (!t) return 'সময় এখনও নিশ্চিত নয়'; return new Date('2026-11-14T'+t+':00+06:00').toLocaleTimeString('bn-BD',{timeZone:'Asia/Dhaka',hour:'numeric',minute:'2-digit',hour12:true}); }
  function dayDistance(day, now) { return Math.round((Date.parse(day+'T00:00:00+06:00')-Date.parse(dayOf(now)+'T00:00:00+06:00'))/86400000); }
  function phase(value, now) { if (!value) return 'unknown'; if (exact(value)) return Date.parse(value)>now.getTime()?'future':'past'; var d=dayDistance(value,now);return d>0?'future':d===0?'today':'past'; }
  function applicationState(u, now) {
    var from=phase(u.applyFrom,now), to=phase(u.applyTo,now);
    if (from==='future') return {label:'আবেদন শুরু হতে',target:u.applyFrom,status:'শুরু হয়নি'};
    if (from==='today') return {label:'আজ আবেদন শুরুর দিন',target:null,status:'আজ শুরু',hint:'শুরুর নির্দিষ্ট সময় নিশ্চিত নয়। পোর্টাল দেখুন।'};
    if (to==='past') return {label:'আবেদনের সময় শেষ',target:null,status:'শেষ'};
    if (to==='today') return {label:'আজ আবেদনের শেষ দিন',target:null,status:'শেষ দিন',hint:'শেষের নির্দিষ্ট সময় নিশ্চিত নয়। এখনই অফিসিয়াল পোর্টাল দেখুন।'};
    if (to==='future') return {label:'আবেদনের শেষ সময় পর্যন্ত',target:u.applyTo,status:from==='past'?'আবেদন চলছে':'শেষ তারিখ ঘোষিত'};
    return {label:'আবেদনের সময় এখনও নিশ্চিত নয়',target:null,status:'অপেক্ষমাণ'};
  }
  // Exposed pure helpers for regression tests; all instants use explicit Bangladesh offsets.
  window.AdmissionTime = {phase:phase, applicationState:applicationState, dayDistance:dayDistance, stamp:stamp};
  var currentUni, currentUnit, shift='', widgetMode=get('bd_widget_mode','exam');
  function readRoute() {
    var key=location.pathname.split('/').filter(Boolean)[0];key=key?key.toUpperCase():get('vc_uni','DU').toUpperCase();
    currentUni=data[key]?key:'DU';
    var hash='';try{hash=decodeURIComponent(location.hash.slice(1)).toLowerCase();}catch(e){}
    var units=data[currentUni].units;var saved=hash||get('vc_unit_'+currentUni,'');
    currentUnit=(units.find(function(u){return u.id===saved||u.code.toLowerCase()===saved;})||units[0]).id;
    shift=get('bd_shift_'+currentUni+'_'+currentUnit,'');
  }
  function selected(){return data[currentUni].units.find(function(u){return u.id===currentUnit;});}
  function chosenTime(u){if(u.shifts){var s=u.shifts.find(function(s){return s.time===shift;});return s||{};}return {time:u.time,endTime:u.endTime};}
  function targetExam(){var u=selected();if(data[currentUni].status==='tentative'||data[currentUni].status==='pending'||u.dateEnd)return null;return stamp(u.date,chosenTime(u).time);}
  function save(updateRoute) {set('vc_uni',currentUni);set('vc_unit_'+currentUni,currentUnit);set('bd_shift_'+currentUni+'_'+currentUnit,shift);if(updateRoute){var url='/'+currentUni+'#'+selected().code;if(location.pathname+location.hash!==url)history.pushState(null,'',url);}notifyWidget();}
  function theme(){var light=document.documentElement.getAttribute('data-theme')==='light';var t=data[currentUni].theme[light?'light':'dark'];var style=document.documentElement.style;style.setProperty('--header-from',t.headerFrom);style.setProperty('--header-to',t.headerTo);style.setProperty('--accent',t.accentLight);style.setProperty('--glow',t.glow);$('themeToggle').textContent=light?'☾':'☀';$('themeToggle').setAttribute('aria-label',light?'ডার্ক মোড চালু করুন':'লাইট মোড চালু করুন');document.querySelector('meta[name=theme-color]').content=light?'#f3f6fb':'#0b1325';}
  function filterOptions(){var q=$('search').value.trim().toLowerCase();var keys=Object.keys(data).filter(function(k){return (k+' '+data[k].name).toLowerCase().indexOf(q)!==-1;});$('uniSelect').innerHTML=keys.map(function(k){return '<option value="'+k+'">'+escape(data[k].name)+'</option>';}).join('');$('resultCount').textContent=bn(keys.length)+'টি প্রতিষ্ঠান';$('emptySearch').hidden=keys.length>0;$('mainContent').hidden=!keys.length;$('uniSelect').disabled=!keys.length;if(keys.length){if(keys.indexOf(currentUni)<0){currentUni=keys[0];currentUnit=data[currentUni].units[0].id;shift='';render();save(true);}$('uniSelect').value=currentUni;}}
  var statusLabels={official:'অফিসিয়াল সূত্র পাওয়া',reported:'সংবাদসূত্র • পুনরায় মিলিয়ে নিন',tentative:'সম্ভাব্য • চূড়ান্ত নয়',pending:'যাচাই অপেক্ষমাণ'};
  function range(u){return date(u.date)+(u.dateEnd?' – '+date(u.dateEnd):'');}
  function render(){var u=data[currentUni],item=selected();theme();$('uniCode').textContent=currentUni+' / '+u.session;$('uniName').textContent=u.name;$('sourceBadge').textContent=statusLabels[u.status];$('uniLogo').textContent=currentUni;
    if(u.logo){var img=document.createElement('img');img.src=u.logo;img.alt=u.name+' লোগো';img.onerror=function(){this.remove();$('uniLogo').textContent=currentUni;};$('uniLogo').textContent='';$('uniLogo').appendChild(img);}
    $('campus').style.backgroundImage=u.campusBg?'url("'+u.campusBg+'")':'none';
    $('unitSelect').innerHTML=u.units.map(function(x){return '<option value="'+x.id+'">'+escape(x.name)+'</option>';}).join('');$('unitSelect').value=currentUnit;
    $('shiftWrap').hidden=!item.shifts;if(item.shifts){$('shiftSelect').innerHTML='<option value="">নিজের শিফট নির্বাচন করুন</option>'+item.shifts.map(function(s){return '<option value="'+s.time+'">'+escape(s.label)+' · '+time(s.time)+'–'+time(s.endTime)+'</option>';}).join('');$('shiftSelect').value=shift;}
    $('notice').textContent=u.note;$('notice').hidden=!u.note;
    $('unitCount').textContent=bn(u.units.length)+'টি বিভাগ';
    var times=item.shifts?item.shifts.map(function(s){return s.label+': '+time(s.time)+' – '+time(s.endTime);}).join(' / '):time(item.time)+(item.endTime?' – '+time(item.endTime):'');
    function fact(label,value,wide){return '<div class="fact'+(wide?' wide':'')+'"><span class="fact-label">'+label+'</span><div class="fact-value'+(!value?' unknown':'')+'">'+escape(value||'এখনও যাচাই করা যায়নি — নতুন বিজ্ঞপ্তি দেখুন')+'</div></div>';}
    $('selectedDetail').innerHTML='<h3 class="detail-title">'+escape(item.name)+'</h3><div class="facts">'+fact('পরীক্ষার দিন',range(item))+fact('সময় · বাংলাদেশ (UTC+৬)',times)+fact('আবেদন ফি',item.fee)+fact('তথ্যের অবস্থা',statusLabels[u.status])+fact('যেভাবে পরীক্ষা হবে',item.method,true)+(item.note?fact('এই ইউনিটের নির্দেশনা',item.note,true):'')+'</div>';
    $('unitsGrid').innerHTML=u.units.map(function(x,i){return '<button type="button" class="unit-card" data-unit="'+x.id+'" aria-pressed="'+(x.id===currentUnit)+'" style="animation-delay:'+Math.min(i*30,180)+'ms"><span class="card-code">'+escape(x.code)+'</span><span class="card-name">'+escape(x.name)+'</span><span class="card-date">'+escape(range(x))+'</span><span class="card-fee">ফি: '+escape(x.fee||'যাচাই অপেক্ষমাণ')+'</span></button>';}).join('');
    $('applicationDates').innerHTML='<div class="date-row"><span>আবেদন শুরু</span><strong>'+date(u.applyFrom,true)+'</strong></div><div class="date-row"><span>আবেদন শেষ</span><strong>'+date(u.applyTo,true)+'</strong></div>';
    $('paymentInfo').innerHTML=u.paymentTo?'বকেয়া ফি দেওয়ার শেষ দিন: <strong>'+date(u.paymentTo,true)+'</strong> (নতুন আবেদন নয়)':'';
    $('applyLink').href=u.applicationUrl;
    $('sources').innerHTML='<ul>'+u.sources.map(function(s){return '<li><a href="'+escape(s.url)+'" target="_blank" rel="noopener noreferrer">'+escape(s.title)+' ↗</a><span class="source-kind">'+(s.kind==='official'?'অফিসিয়াল':'সংবাদসূত্র')+'</span></li>';}).join('')+(u.brandSource?'<li><a href="'+escape(u.brandSource)+'" target="_blank" rel="noopener noreferrer">বিশ্ববিদ্যালয়ের পরিচিতি · Wikipedia ↗</a></li>':'')+'</ul>';
    $('examDate').textContent=range(item)+(item.time?' · '+times:'')+' · বাংলাদেশ সময়';
    $('examWidget').hidden=!window.Android;$('applicationWidget').hidden=!window.Android;
    tick();
  }
  function message(el,main,hint){var html='<div class="clock-message">'+escape(main)+(hint?'<small>'+escape(hint)+'</small>':'')+'</div>';if(el.innerHTML!==html)el.innerHTML=html;}
  function countdown(el,target,now,label){if(!exact(target)){var days=dayDistance(target,now);message(el,days>0?bn(days)+' দিন বাকি':days===0?'আজ নির্ধারিত দিন':'নির্ধারিত দিন পেরিয়েছে','শুধু দিন ঘোষিত; ঘড়ির সময় এখনও নিশ্চিত নয়।');return;}
    var seconds=Math.max(0,Math.floor((Date.parse(target)-now.getTime())/1000));var values=[Math.floor(seconds/86400),Math.floor(seconds%86400/3600),Math.floor(seconds%3600/60),seconds%60];
    if(!el.querySelector('.clock'))el.innerHTML='<div class="clock" role="timer" aria-live="off">'+['দিন','ঘণ্টা','মিনিট','সেকেন্ড'].map(function(l){return '<div class="clock-box"><span class="clock-number"></span><span class="clock-label">'+l+'</span></div>';}).join('')+'</div>';
    el.querySelector('.clock').setAttribute('aria-label',label);var numbers=el.querySelectorAll('.clock-number');values.forEach(function(v,i){var s=bn(String(v).padStart(2,'0'));if(numbers[i].textContent!==s)numbers[i].textContent=s;});
  }
  function tick(){var now=new Date(),u=data[currentUni],item=selected(),target=targetExam(),el=$('examCountdown');
    if(u.status==='tentative')message(el,'চূড়ান্ত সূচির অপেক্ষা','সম্ভাব্য তারিখের জন্য নির্ভুল কাউন্টডাউন দেখানো হচ্ছে না।');
    else if(!item.date)message(el,'তারিখ এখনও নিশ্চিত নয়','সর্বশেষ অফিসিয়াল বিজ্ঞপ্তি দেখুন।');
    else if(item.dateEnd)message(el,'ইউনিটভিত্তিক সূচির অপেক্ষা','ঘোষিত পরীক্ষার সময়সীমা নিচে দেওয়া আছে।');
    else if(item.shifts&&!target)message(el,'নিজের শিফট বেছে নিন','প্রবেশপত্রের শিফট অনুযায়ী কাউন্টডাউন দেখাবে।');
    else if(target&&phase(target,now)==='past'){var end=stamp(item.date,chosenTime(item).endTime);message(el,end&&Date.parse(end)>now.getTime()?'নির্ধারিত পরীক্ষার সময় চলছে':'নির্ধারিত শুরুর সময় পেরিয়েছে','সর্বশেষ অবস্থা অফিসিয়াল পোর্টালে দেখুন।');}
    else countdown(el,target||item.date,now,'পরীক্ষা শুরু হতে বাকি');
    var a=applicationState(u,now);$('applicationBadge').textContent=a.status;$('applicationPhase').textContent=a.target?a.label:'';
    if(a.target)countdown($('applicationCountdown'),a.target,now,a.label);else message($('applicationCountdown'),a.label,a.hint||'');
    // Date transitions also refresh a selected native application widget target.
    var signature=widgetMode+'|'+(a.target||'')+'|'+target;if(signature!==lastWidgetTarget){lastWidgetTarget=signature;notifyWidget();}
  }
  var lastWidgetTarget='',toastTimer;
  function notifyWidget(){if(!window.Android||!Android.saveSelection)return;var u=data[currentUni],item=selected(),a=applicationState(u,new Date());var application=widgetMode==='application';var target=application?(exact(a.target)?a.target:null):targetExam();var mode=document.documentElement.getAttribute('data-theme');var payload={uniName:u.name,uniLogo:u.logo?new URL(u.logo,location.href).href:'',campusBg:u.campusBg?new URL(u.campusBg,location.href).href:'',unitName:application?'আবেদন · '+a.status:item.name,unitSub:application?a.label:(item.note||''),examDateIso:target||'',examNote:target?'বাংলাদেশ সময় · যাচাই: ১৪ নভেম্বর ২০২৬':(application?a.label:'নির্দিষ্ট সময় নিশ্চিত নয়; অ্যাপে বিস্তারিত দেখুন'),eventKind:application?'application':'exam',theme:u.theme[mode==='light'?'light':'dark']};try{Android.saveSelection(JSON.stringify(payload));}catch(e){}}
  function chooseUnit(id){currentUnit=id;shift=get('bd_shift_'+currentUni+'_'+currentUnit,'');render();save(true);}
  $('uniSelect').addEventListener('change',function(){currentUni=this.value;var units=data[currentUni].units,saved=get('vc_unit_'+currentUni,'');currentUnit=(units.find(function(u){return u.id===saved;})||units[0]).id;shift=get('bd_shift_'+currentUni+'_'+currentUnit,'');render();save(true);});
  $('unitSelect').addEventListener('change',function(){chooseUnit(this.value);});$('shiftSelect').addEventListener('change',function(){shift=this.value;render();save(false);});
  $('unitsGrid').addEventListener('click',function(e){var card=e.target.closest('[data-unit]');if(card){var id=card.getAttribute('data-unit');chooseUnit(id);var replacement=$('unitsGrid').querySelector('[data-unit="'+id+'"]');replacement.focus({preventScroll:true});}});
  $('search').addEventListener('input',filterOptions);
  $('themeToggle').addEventListener('click',function(){var mode=document.documentElement.getAttribute('data-theme')==='dark'?'light':'dark';document.documentElement.setAttribute('data-theme',mode);set('vc_theme',mode);theme();notifyWidget();});
  function widgetClick(mode){widgetMode=mode;set('bd_widget_mode',mode);notifyWidget();$('toast').textContent='উইজেটের পছন্দ সংরক্ষিত হয়েছে';$('toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(function(){$('toast').hidden=true;},2600);}
  $('examWidget').addEventListener('click',function(){widgetClick('exam');});$('applicationWidget').addEventListener('click',function(){widgetClick('application');});
  function routeChanged(){readRoute();$('search').value='';filterOptions();render();save(false);}
  window.addEventListener('popstate',routeChanged);window.addEventListener('hashchange',routeChanged);
  document.addEventListener('visibilitychange',function(){if(!document.hidden)tick();});
  readRoute();document.documentElement.setAttribute('data-theme',get('vc_theme','dark')==='light'?'light':'dark');filterOptions();render();save(false);setInterval(tick,1000);
  if(!window.Android)fetch('/api/apk-url').then(function(r){return r.ok?r.json():null;}).then(function(r){if(r&&/^https:\/\//i.test(r.url||'')){$('installLink').href=r.url;$('installLink').hidden=false;}}).catch(function(){});
})();
