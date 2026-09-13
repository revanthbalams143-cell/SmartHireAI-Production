(function(){
  'use strict';
  const api=window.smartHireApi; if(!api)return;
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const pct=v=>`${Math.round(Number(v)||0)}%`;
  const renderMetrics=(target,items)=>{const el=document.getElementById(target);if(!el)return;el.innerHTML=items.length?items.map(x=>`<div class="bar-row"><span>${esc(x.label)}</span><div class="bar"><i style="width:${Math.max(0,Math.min(100,Number(x.value)||0))}%"></i></div><b>${pct(x.value)}</b></div>`).join(''):'<div class="empty-state">Not enough evaluated sessions yet.</div>';};
  async function resolveUserId(){
    const cached=Number(localStorage.getItem('userId')||0); if(cached>0)return cached;
    try{const me=await api.requestJson('/api/profile/me');const uid=Number(me?.userId||me?.id||0);if(uid>0){localStorage.setItem('userId',String(uid));return uid;}}catch(e){}
    return 0;
  }
  async function load(){
    const uid=await resolveUserId();
    if(!uid){document.getElementById('analyticsError')?.classList.remove('hidden');return;}
    try{
      const d=await api.requestJson('/api/analytics/candidate/'+uid); const s=d?.summary||{}; const set=(id,v)=>{const e=document.getElementById(id);if(e)e.textContent=v;};
      set('aOverall',pct(s.averageOverallScore)); set('aTechnical',pct(s.averageTechnical)); set('aCommunication',pct(s.averageCommunication)); set('aProblem',pct(s.averageProblemSolving)); set('aProfessionalism',pct(s.averageProfessionalism));
      [['Technical',s.averageTechnical],['Communication',s.averageCommunication],['Confidence',s.averageConfidence],['Problem',s.averageProblemSolving],['Professionalism',s.averageProfessionalism]].forEach(([k,v])=>{const e=document.getElementById('b'+k);if(e)e.style.width=Math.max(0,Math.min(100,Number(v)||0))+'%';set('v'+k,pct(v));});
      const best=[['Technical',s.averageTechnical],['Communication',s.averageCommunication],['Confidence',s.averageConfidence],['Professionalism',s.averageProfessionalism],['Problem Solving',s.averageProblemSolving]].sort((a,b)=>Number(b[1]||0)-Number(a[1]||0))[0];
      if(best)set('strongestSkill',Number(best[1]||0)>0?best[0]+' is currently your strongest area at '+pct(best[1])+'.':'Complete an evaluated interview to unlock your strongest-skill insight.');
      set('analyticsInterviewCount',s.evaluatedInterviews||0); set('analyticsImprovement',(Number(s.improvementDelta)>=0?'+':'')+Number(s.improvementDelta||0)+'%'); set('analyticsRating',s.rating||'Not enough data');
      renderMetrics('analyticsDimensions',(d.skillAnalytics||[]).map(x=>({label:x.label,value:x.value}))); renderMetrics('analyticsResumeSkills',(d.resumeSkills||[]).map(x=>({label:x.skill,value:x.score})));
      const weak=document.getElementById('analyticsWeakAreas');if(weak)weak.innerHTML=(d.weakAreas||[]).slice(0,8).map(x=>`<div class="goal-card"><strong>${esc(x.label)}</strong><p>${esc(x.reason||'Focus area identified from stored evidence.')} ${x.score?`Current score: ${pct(x.score)}.`:''}</p><small>${esc(x.source||'Analytics')}</small></div>`).join('')||'<div class="empty-state">No weak areas have been identified yet.</div>';
      const hist=document.getElementById('analyticsHistoryBody');if(hist)hist.innerHTML=(d.history||[]).filter(Boolean).slice(0,12).map(x=>`<tr><td>${esc(x.jobRole||'Interview')}</td><td>${esc(x.interviewType||'Interview')}</td><td>${x.overallScore==null?'—':pct(x.overallScore)}</td><td>${esc(x.rating||x.status||'—')}</td><td>${x.date?new Date(x.date).toLocaleDateString():'—'}</td><td><a href="interview-report.html?interviewId=${encodeURIComponent(x.interviewId)}" class="table-btn">Report</a></td></tr>`).join('')||'<tr><td colspan="6">No interview history yet.</td></tr>';
      const trends=document.getElementById('analyticsTrends');if(trends)trends.innerHTML=(d.trends||[]).slice(-10).map(t=>`<div class="trend-card"><div><strong>${t.date?new Date(t.date).toLocaleDateString():'Session'}</strong><small>Overall</small></div><b>${pct(t.overall)}</b></div>`).join('')||'<div class="empty-state">Complete more interviews to build a performance trend.</div>';
      const imp=document.getElementById('analyticsImprovement');if(imp)imp.innerHTML=(d.improvement||[]).map(x=>`<div class="bar-row"><span>${esc(x.label)}</span><div class="bar"><i style="width:${Math.max(0,Math.min(100,x.latest||0))}%"></i></div><b>${x.delta>=0?'+':''}${x.delta}%</b></div>`).join('')||'<div class="empty-state">Improvement tracking becomes available after two evaluated sessions.</div>';
      const fb=d.feedback||{};const feedback=document.getElementById('analyticsFeedback');if(feedback)feedback.innerHTML=[['Strengths',fb.strengths],['Weaknesses',fb.weaknesses],['Improvement suggestions',fb.improvementSuggestions],['Practice recommendations',fb.practiceRecommendations],['Learning resources',fb.learningResources]].map(([title,arr])=>`<div class="goal-card"><strong>${title}</strong><ul>${(Array.isArray(arr)?arr:[]).slice(0,5).map(x=>`<li>${esc(x)}</li>`).join('')||'<li>Not available from the latest evaluation.</li>'}</ul></div>`).join('')+`<div class="goal-card"><strong>Recommendation</strong><p>${esc(fb.recommendation||'Complete an interview to receive an AI recommendation.')}</p></div>`;
      document.querySelectorAll('[data-analytics-loading]').forEach(e=>e.removeAttribute('data-analytics-loading'));
    }catch(e){
      console.warn('Candidate analytics failed',e); document.querySelectorAll('#analyticsDimensions,#analyticsWeakAreas,#analyticsTrends,#analyticsFeedback').forEach(el=>{if(el && /Loading/.test(el.textContent||''))el.innerHTML='<div class="empty-state">Unable to load live analytics. Refresh after the backend is ready.</div>';}); document.getElementById('analyticsError')?.classList.remove('hidden');
    }
  }
  document.getElementById('analyticsDownloadReport')?.addEventListener('click',async()=>{const uid=await resolveUserId();if(!uid)return;const b=document.getElementById('analyticsDownloadReport');const old=b.innerHTML;b.disabled=true;b.textContent='Preparing…';try{const r=await api.request('/api/analytics/report/'+uid);if(!r.ok)throw new Error('Report download failed');const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='smarthire-analytics-report-'+uid+'.pdf';document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),500);}catch(e){window.smartHireToast?.('Analytics',e.message||'Could not download report','error');}finally{b.disabled=false;b.innerHTML=old;}});
  document.getElementById('analyticsRefresh')?.addEventListener('click',load);
  document.addEventListener('DOMContentLoaded',()=>{load();setTimeout(load,1200);},{once:true});
})();
