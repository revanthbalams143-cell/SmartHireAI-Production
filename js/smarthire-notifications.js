(function(){
  'use strict';
  const api=window.smartHireApi; let host=null; let initialized=false; let observer=null;
  const esc=v=>String(v??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));
  const role=()=>String(localStorage.getItem('userRole')||'candidate').toLowerCase();
  const allowedForRole=n=>{
    const r=role(), t=String(n?.type||'').toUpperCase();
    if(r==='admin') return ['ADMIN_ALERT','SYSTEM','AI_HEALTH','USER_ACTIVITY','SECURITY'].includes(t);
    if(r==='recruiter') return ['RECRUITER','INTERVIEW','EVALUATION','REPORT','REMINDER'].includes(t);
    return ['INTERVIEW','EVALUATION','REPORT','RESUME','RECORDING','REMINDER'].includes(t);
  };
  const icon=t=>{t=String(t||'').toUpperCase(); if(['EVALUATION','AI_HEALTH'].includes(t))return 'fa-chart-line'; if(['REPORT'].includes(t))return 'fa-file-lines'; if(t==='RESUME')return 'fa-file-arrow-up'; if(t==='INTERVIEW')return 'fa-video'; if(t==='RECORDING')return 'fa-film'; if(t==='REMINDER')return 'fa-calendar-day'; if(t==='SYSTEM')return 'fa-server'; if(t==='SECURITY')return 'fa-shield-halved'; if(t==='USER_ACTIVITY')return 'fa-user-clock'; return 'fa-bell';};

  function positionPanel(btn){
    if (!host || !btn) return;
    const rect = btn.getBoundingClientRect();
    const width = Math.min(410, window.innerWidth - 24);
    host.style.width = width + 'px';
    const top = rect.bottom + 8 + window.scrollY;
    let left = rect.right - width + window.scrollX;
    if (left < 12) left = 12;
    if (left + width > window.innerWidth - 12) left = window.innerWidth - width - 12;
    host.style.top = top + 'px';
    host.style.left = left + 'px';
    host.style.right = 'auto';
  }

  function ensure(){
    if(host)return host;
    host=document.createElement('div'); host.className='sh-notification-panel'; host.setAttribute('role','dialog'); host.setAttribute('aria-label','Notifications');
    host.innerHTML='<div class="sh-notification-head"><strong>Notifications</strong><div><button type="button" class="sh-notification-readall">Mark all read</button><button type="button" class="sh-notification-close" aria-label="Close notifications">×</button></div></div><div class="sh-notification-list"><div class="sh-notification-empty">Loading...</div></div><div class="sh-notification-foot"><button type="button" class="sh-notification-reminder">+ Set interview reminder</button></div>';
    document.body.appendChild(host);
    host.querySelector('.sh-notification-close').onclick=()=>host.classList.remove('open');
    host.querySelector('.sh-notification-readall').onclick=async()=>{try{await api?.requestJson('/api/notifications/read-all',{method:'POST'});}catch(e){} await load();};
    host.querySelector('.sh-notification-reminder').onclick=createReminder;
    return host;
  }
  function setBadge(items){
    document.querySelectorAll('.notification-button').forEach(btn=>{
      let badge=btn.querySelector('[data-notification-count]');
      if(!badge){badge=document.createElement('span');badge.dataset.notificationCount='true';btn.appendChild(badge);}
      const unread=items.filter(x=>!x.readAt).length; badge.textContent=String(unread); badge.style.display=unread?'inline-grid':'none';
    });
  }
  function render(items){
    const list=host?.querySelector('.sh-notification-list'); if(!list)return;
    if(!items.length){list.innerHTML='<div class="sh-notification-empty">No notifications for this workspace.</div>';setBadge([]);return;}
    list.innerHTML=items.slice(0,20).map(n=>`<button type="button" class="sh-notification-item ${n.readAt?'read':''}" data-notification-id="${n.id}"><span class="sh-notification-icon"><i class="fa-solid ${icon(n.type)}"></i></span><span class="sh-notification-copy"><strong>${esc(n.title||'Notification')}</strong><small>${esc(n.message||'')}</small><time>${n.dueAt?'Due '+new Date(n.dueAt).toLocaleString():(n.createdAt?new Date(n.createdAt).toLocaleString():'')}</time></span></button>`).join('');
    setBadge(items);
    list.querySelectorAll('[data-notification-id]').forEach(b=>b.onclick=async()=>{const item=items.find(x=>String(x.id)===b.dataset.notificationId);try{await api?.requestJson('/api/notifications/'+b.dataset.notificationId+'/read',{method:'POST'});}catch(e){} if(item?.actionUrl){let url=item.actionUrl;if(url.startsWith('/pages/'))url='..'+url;window.location.href=url;} await load();});
  }
  async function load(){
    ensure();
    try{const items=api?await api.requestJson('/api/notifications'):[]; render((Array.isArray(items)?items:[]).filter(allowedForRole));}
    catch(e){render([]);}
  }
  async function createReminder(){const when=prompt('Reminder time (YYYY-MM-DDTHH:MM)');if(!when)return;const due=new Date(when);if(Number.isNaN(due.getTime())){window.smartHireToast?.('Reminder','Invalid date/time','error');return;}try{await api.requestJson('/api/notifications/reminders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:'Interview reminder',message:'Your SmartHire AI interview practice reminder is due.',actionUrl:'/pages/interview-setup.html',dueAt:new Date(due).toISOString().slice(0,19)})});await load();host?.classList.add('open');}catch(e){window.smartHireToast?.('Reminder',e.message||'Could not create reminder','error');}}
  function bindButtons(){
    const buttons=document.querySelectorAll('.notification-button'); if(!buttons.length)return false;
    buttons.forEach(b=>{
      if(b.dataset.shNotifBound)return; 
      b.dataset.shNotifBound='1';
      b.addEventListener('click',e=>{
        e.preventDefault();
        e.stopPropagation();
        const p=ensure();
        positionPanel(b);
        p.classList.toggle('open');
        if(p.classList.contains('open'))load();
      });
    });
    return true;
  }
  function init(){
    if(initialized){bindButtons();return;}
    if(!bindButtons()){setTimeout(init,250);return;}
    initialized=true; ensure(); load();
    if(!observer){observer=new MutationObserver(()=>bindButtons());observer.observe(document.body,{childList:true,subtree:true});}
    document.addEventListener('click',e=>{if(host?.classList.contains('open')&&!e.target.closest('.sh-notification-panel')&&!e.target.closest('.notification-button'))host.classList.remove('open');});
    document.addEventListener('keydown',e=>{if(e.key==='Escape')host?.classList.remove('open');});
    window.addEventListener('resize',()=>{if(host?.classList.contains('open')){const btn=document.querySelector('.notification-button');if(btn)positionPanel(btn);}});
  }
  window.smartHireNotificationsInit=init;
  document.addEventListener('DOMContentLoaded',init);
  setInterval(()=>{if(document.visibilityState==='visible'&&initialized)load();},30000);
})();
