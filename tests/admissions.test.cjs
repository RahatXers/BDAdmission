const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.join(__dirname, '..');
const data = JSON.parse(fs.readFileSync(path.join(root,'data/admissions.json')));
let context = {window:{ADMISSIONS:data},document:{},Date};
vm.createContext(context);
const js = fs.readFileSync(path.join(root,'assets/app.js'),'utf8');
vm.runInContext(js.slice(0, js.indexOf('  var currentUni'))+'})();',context);
const time = context.window.AdmissionTime;
assert.equal(time.stamp('2027-02-12','11:00'),'2027-02-12T11:00:00+06:00');
assert.equal(Date.parse(time.stamp('2027-02-12','11:00')),Date.parse('2027-02-12T05:00:00Z'));
assert.equal(time.phase(null,new Date()),'unknown');
assert.equal(time.applicationState(data.DU,new Date('2026-11-11T05:59:59Z')).status,'শুরু হয়নি');
assert.equal(time.applicationState(data.DU,new Date('2026-11-11T06:00:00Z')).status,'আবেদন চলছে');
assert.equal(time.applicationState(data.DU,new Date('2026-11-25T17:58:59Z')).status,'আবেদন চলছে');
assert.equal(time.applicationState(data.DU,new Date('2026-11-25T17:59:00Z')).status,'শেষ');
assert.equal(time.applicationState(data.CU,new Date('2026-12-10T02:00:00Z')).status,'আজ শুরু');
assert.equal(time.applicationState(data.CU,new Date('2027-01-10T16:00:00Z')).status,'শেষ দিন');
assert.equal(time.applicationState(data.CU,new Date('2027-01-10T18:00:00Z')).status,'শেষ');
assert.equal(time.applicationState(data.KU,new Date()).status,'অপেক্ষমাণ');
assert.equal(time.dayDistance('2027-01-10',new Date('2027-01-09T18:00:00Z')),0);
assert.equal(time.applicationState(data.BUET,new Date('2026-11-22T00:00:00Z')).status,'অপেক্ষমাণ');
for (const [key,u] of Object.entries(data)) {
 assert.ok(u.sources.length,key+' has sources');assert.ok(/^https:\/\//.test(u.applicationUrl));
 for(const s of u.sources)assert.ok(/^https:\/\//.test(s.url));
 const ids=new Set();for(const item of u.units){assert.ok(!ids.has(item.id));ids.add(item.id);if(item.date)assert.match(item.date,/^\d{4}-\d{2}-\d{2}$/);if(item.time)assert.ok(item.date);if(item.dateEnd)assert.ok(item.dateEnd>=item.date);if(item.shifts)assert.equal(item.time,null);}
 for(const field of ['applyFrom','applyTo','paymentTo'])if(u[field]&&u[field].includes('T'))assert.ok(u[field].endsWith('+06:00'));
}
assert.equal(data.RU.units[0].date,'2027-02-12');assert.equal(data.RU.units[0].shifts[1].time,'15:00');
assert.equal(data.KU.units[0].date,'2027-02-04');assert.equal(data.JNU.applyFrom,'2026-11-25T12:00:00+06:00');
assert.equal(data.CU.applyTo,'2027-01-10');assert.equal(data.CU.paymentTo,'2027-01-12');assert.equal(data.CUET.units[0].date,null);
assert.equal(data.BUP.units.filter(u=>u.id.startsWith('fbs')).length,2);
const bundle={window:{}};vm.runInNewContext(fs.readFileSync(path.join(root,'data/admissions.js'),'utf8'),bundle);assert.equal(JSON.stringify(bundle.window.ADMISSIONS),JSON.stringify(data));
const html=fs.readFileSync(path.join(root,'index.html'),'utf8');for(const m of js.matchAll(/\$\('([^']+)'\)/g))assert.ok(html.includes('id="'+m[1]+'"'),'DOM id '+m[1]);
const config=JSON.parse(fs.readFileSync(path.join(root,'vercel.json')));assert.ok(config.rewrites[0].source.includes('data/'),'data files bypass SPA rewrite');
console.log('PASS: Bangladesh timezone, start/deadline boundaries, date-only precision, unknown schedules, updated dates, schema, bundle parity, DOM references and Vercel data routing.');
