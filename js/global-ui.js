(function(){
  'use strict';
  function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  const routes=[
    ['Dashboard','candidate.html','fa-house'],['Mock Interviews','interview-setup.html','fa-microphone-lines'],['Resume Analyzer','resume.html','fa-file-circle-check'],
    ['Interview History','interview-history.html','fa-clock-rotate-left'],['Practice Assessment','practice-assessment.html','fa-list-check'],
    ['Performance Analytics','performance-analytics.html','fa-chart-column'],['Improvement Progress','improvement-progress.html','fa-arrow-trend-up'],
    ['Reports','reports.html','fa-file-arrow-down'],['Settings','settings.html','fa-gear']
  ];
  let panel=null;
  function ensureSearchPanel(){
    if(panel)return panel;
    panel=document.createElement('div'); panel.className='sh-search-panel'; panel.hidden=true; document.body.appendChild(panel); return panel;
  }
  function showResults(input){
    const q=(input.value||'').trim().toLowerCase(); const p=ensureSearchPanel();
    const matches=q?routes.filter(r=>r[0].toLowerCase().includes(q)):routes.slice(0,6);
    p.innerHTML=matches.length?matches.map(r=>`<a href="${r[1]}" class="sh-search-result"><span class="sh-search-result-icon"><i class="fa-solid ${r[2]}"></i></span><span><strong>${esc(r[0])}</strong><small>Open ${esc(r[0])}</small></span></a>`).join(''):'<div class="sh-search-empty">No SmartHire section matches that search.</div>';
    const rect=input.getBoundingClientRect(); p.style.top=(rect.bottom+8+window.scrollY)+'px'; p.style.left=Math.max(16,Math.min(window.innerWidth-356,rect.left+window.scrollX))+'px'; p.hidden=false;
  }
  function bindSearch(){
    document.querySelectorAll('.search-box input,.sidebar-search input').forEach(input=>{
      if(input.dataset.shSearchBound)return; input.dataset.shSearchBound='1';
      input.addEventListener('focus',()=>showResults(input)); input.addEventListener('input',()=>showResults(input));
      input.addEventListener('keydown',e=>{if(e.key==='Escape'){ensureSearchPanel().hidden=true;input.blur();}else if(e.key==='Enter'){const first=ensureSearchPanel().querySelector('a');if(first)first.click();}});
    });
    document.querySelectorAll('.header-icon[aria-label="Search"]').forEach(btn=>{
      if(btn.dataset.shSearchBound)return; btn.dataset.shSearchBound='1'; btn.addEventListener('click',()=>{const target=document.querySelector('.sidebar-search input,.search-box input');target?.focus();});
    });
    document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();document.querySelector('.sidebar-search input,.search-box input')?.focus();}});
    document.addEventListener('click',e=>{if(panel && !panel.hidden && !e.target.closest('.sh-search-panel') && !e.target.closest('.search-box') && !e.target.closest('.sidebar-search'))panel.hidden=true;});
  }
  function ensureNotificationButton(){
    const holders=document.querySelectorAll('.top-icons,.header-actions');
    holders.forEach(holder=>{
      if(holder.querySelector('.notification-button'))return;
      const btn=document.createElement('button'); btn.type='button'; btn.className=holder.classList.contains('header-actions')?'header-icon notification-button':'secondary-btn notification-button'; btn.setAttribute('aria-label','Notifications'); btn.title='Notifications'; btn.innerHTML='<i class="fa-regular fa-bell"></i><span data-notification-count="true" style="display:none">0</span>';
      const theme=holder.querySelector('#candidateThemeToggle,.header-theme'); if(theme) holder.insertBefore(btn,theme); else holder.prepend(btn);
    });
  }
  function init(){ensureNotificationButton();bindSearch();window.smartHireNotificationsInit?.();}
  document.addEventListener('DOMContentLoaded',init);
})();
