(function(){
 'use strict';
 const key='smarthire.settings.v1'; const defaults={emailNotifications:true,interviewReminders:true,aiInsights:true,defaultDifficulty:'medium',defaultDuration:'15',defaultInterview:'technical',reducedMotion:false,autoTheme:false,privacyLocalOnly:false};
 const state=Object.assign({},defaults,JSON.parse(localStorage.getItem(key)||'{}'));
 const save=()=>localStorage.setItem(key,JSON.stringify(state));
 const apply=()=>{document.body.classList.toggle('reduce-motion',!!state.reducedMotion);};
 function bind(){
  const email=document.getElementById('settingsEmail'); if(email) email.textContent=localStorage.getItem('userEmail')||'your candidate account';
  document.querySelectorAll('[data-setting-toggle]').forEach(el=>{const k=el.dataset.settingToggle;el.checked=!!state[k];el.addEventListener('change',()=>{state[k]=el.checked;save();apply();});});
  document.querySelectorAll('[data-setting-select]').forEach(el=>{const k=el.dataset.settingSelect;el.value=state[k]||defaults[k];el.addEventListener('change',()=>{state[k]=el.value;save();});});
  document.getElementById('savePreferences')?.addEventListener('click',()=>{save();apply();window.smartHireToast?.('Settings','Your SmartHire preferences were saved.','success');});
  document.getElementById('clearLocalData')?.addEventListener('click',()=>{if(!confirm('Clear saved SmartHire preferences and local dashboard cache? Your server account and database data will not be deleted.'))return;Object.keys(localStorage).filter(k=>k.startsWith('smarthire.')).forEach(k=>localStorage.removeItem(k));Object.assign(state,defaults);location.reload();});
  document.getElementById('requestMediaTest')?.addEventListener('click',async()=>{try{const s=await navigator.mediaDevices.getUserMedia({video:true,audio:true});s.getTracks().forEach(t=>t.stop());window.smartHireToast?.('Device access','Camera and microphone access is available.','success');}catch(e){window.smartHireToast?.('Device access','Browser blocked camera or microphone access. Check site permissions.','error');}});
  document.getElementById('settingsDefaultInterview')?.value && null;
  apply();
 }
 document.addEventListener('DOMContentLoaded',bind);
})();
