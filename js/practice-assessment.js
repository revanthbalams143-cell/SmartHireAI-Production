(function(){
  'use strict';
  const api=window.smartHireApi; if(!api)return;

  async function resolveUserId(){
    const cached=Number(localStorage.getItem('userId')||0);
    if(cached>0)return cached;
    try{
      const me=await api.requestJson('/api/profile/me');
      const uid=Number(me?.userId||me?.id||0);
      if(uid>0){localStorage.setItem('userId',String(uid));return uid;}
    }catch(e){}
    return 0;
  }

  const coding=[
    {title:'Two Sum',prompt:'Given an integer array nums and integer target, return indices of the two numbers that add up to target. Aim for O(n).',starter:'function twoSum(nums, target) {\n  // write your solution\n}',keywords:['map','hash','index','target','return']},
    {title:'Valid Parentheses',prompt:'Given a string containing (), {}, [], determine whether brackets are balanced and correctly nested.',starter:'function isValid(s) {\n  // write your solution\n}',keywords:['stack','push','pop','map','return']},
    {title:'Merge Intervals',prompt:'Merge overlapping intervals and return the non-overlapping set of intervals that covers all input ranges.',starter:'function merge(intervals) {\n  // write your solution\n}',keywords:['sort','interval','overlap','merge','push']},
    {title:'Longest Substring Without Repeating Characters',prompt:'Return the length of the longest substring without repeating characters.',starter:'function lengthOfLongestSubstring(s) {\n  // write your solution\n}',keywords:['window','set','map','left','right']},
    {title:'Binary Search',prompt:'Given a sorted integer array and a target, return the index of the target or -1 using O(log n) time.',starter:'function binarySearch(nums, target) {\n  // write your solution\n}',keywords:['left','right','mid','while','return']},
    {title:'Maximum Subarray',prompt:'Find the contiguous subarray with the largest sum and return the maximum sum.',starter:'function maxSubArray(nums) {\n  // write your solution\n}',keywords:['current','max','sum','kadane','return']}
  ];
  const aptitude=[
    ['Aptitude 1','If a train covers 180 km in 3 hours, its average speed is:',['40 km/h','50 km/h','60 km/h','90 km/h'],2],
    ['Aptitude 2','A ratio 2:3 is equivalent to:',['4:6','6:8','8:9','10:12'],0],
    ['Aptitude 3','If all developers are problem-solvers and Priya is a developer, which follows?',['Priya is a manager','Priya is a problem-solver','All problem-solvers are developers','None'],1],
    ['Aptitude 4','Find the next number: 2, 6, 12, 20, 30, ?',['36','40','42','44'],2],
    ['Aptitude 5','A shop gives 20% discount on ₹500. Sale price is:',['₹380','₹400','₹420','₹450'],1],
    ['Aptitude 6','If a task takes 8 hours for 4 people equally, how long for 2 people at same rate?',['2 hours','4 hours','8 hours','16 hours'],3],
    ['Aptitude 7','Which word is closest in meaning to “concise”?',['Brief','Complex','Loud','Slow'],0],
    ['Aptitude 8','A clock gains 5 minutes every hour. How much does it gain in 6 hours?',['10 min','20 min','30 min','35 min'],2],
    ['Aptitude 9','A bag has 3 red and 2 blue balls. Probability of red is:',['2/5','3/5','1/2','2/3'],1],
    ['Aptitude 10','Which data structure follows FIFO?',['Stack','Queue','Tree','Graph'],1]
  ];
  const qs=id=>document.getElementById(id); let codingState={i:0,answers:[],timer:1800,id:null}; let codingTicker=null; let aptitudeState={i:0,answers:[],timer:900}; let aptitudeTicker=null;
  function toast(m,type='success'){window.smartHireToast?window.smartHireToast('Practice',m,type):alert(m)}
  function setText(id,v){const e=qs(id);if(e)e.textContent=v}
  function renderCoding(){const q=coding[codingState.i]; setText('codingStep',`Question ${codingState.i+1} of ${coding.length}`); const box=qs('codingPrompt'); if(box)box.innerHTML=`<h3>${q.title}</h3><p>${q.prompt}</p><pre>${q.starter}</pre>`; const editor=qs('codingEditor'); if(editor)editor.value=codingState.answers[codingState.i]||q.starter; qs('codingPrev').disabled=codingState.i===0; qs('codingNext').disabled=codingState.i===coding.length-1;}
  function tick(state,id,onFinish){const el=qs(id); if(!el)return; const m=Math.floor(state.timer/60),s=state.timer%60;el.textContent=`${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;if(state.timer<=0)onFinish();}
  async function save(type,score,total,duration,insights){const uid=await resolveUserId();if(!uid)return;try{await api.requestJson(`/api/interviews/candidate/${uid}/assessments`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({assessmentType:type,score,total,durationSeconds:duration,insights})});}catch(e){console.warn('assessment save failed',e);}}
  async function finishCoding(){if(!codingState.id)return;codingState.answers[codingState.i]=qs('codingEditor')?.value||'';clearInterval(codingTicker);let total=0,ins=[];coding.forEach((q,idx)=>{const a=(codingState.answers[idx]||'').toLowerCase();const hits=q.keywords.filter(k=>a.includes(k)).length;const sc=Math.round((hits/q.keywords.length)*100);total+=sc;ins.push(`${q.title}: ${sc}% keyword/structure coverage`)});const score=Math.round(total/coding.length);setText('codingResultScore',`${score}%`);const resEl=qs('codingResult');if(resEl)resEl.hidden=false;setText('codingResultDetail',score>=70?'Good structural coverage. Review complexity and edge cases before coding interviews.':'Focus on algorithm structure, data structures and edge cases.');await save('coding',score,100,1800-codingState.timer,ins);codingState.id=null;toast('Coding practice saved');loadHistory();}
  function renderAptitude(){const q=aptitude[aptitudeState.i];setText('aptitudeStep',`Question ${aptitudeState.i+1} of ${aptitude.length}`);const box=qs('aptitudePrompt');if(box)box.innerHTML=`<h3>${q[0]}</h3><p>${q[1]}</p>${q[2].map((o,i)=>`<label class="practice-option"><input type="radio" name="aptitudeAnswer" value="${i}" ${aptitudeState.answers[aptitudeState.i]===i?'checked':''}> <span>${o}</span></label>`).join('')}`;qs('aptitudePrev').disabled=aptitudeState.i===0;qs('aptitudeNext').disabled=aptitudeState.i===aptitude.length-1;}
  async function finishAptitude(){const selected=aptitudeState.answers.reduce((n,v,i)=>n+(Number(v)===Number(aptitude[i][3])?1:0),0);clearInterval(aptitudeTicker);const score=Math.round((selected/aptitude.length)*100);setText('aptitudeResultScore',`${score}%`);setText('aptitudeResultDetail',`${selected}/${aptitude.length} correct`);const resEl=qs('aptitudeResult');if(resEl)resEl.hidden=false;await save('aptitude',score,100,900-aptitudeState.timer,[`${selected} correct answers out of ${aptitude.length}`,score>=70?'Strong quantitative/reasoning practice result.':'Review the missed concepts and retry.']);aptitudeState.finished=true;toast('Aptitude assessment saved');loadHistory();}
  async function loadHistory(){const uid=await resolveUserId();if(!uid)return;try{const d=await api.requestJson(`/api/interviews/candidate/${uid}/enhancements`);const rows=d?.assessments||[];const host=qs('practiceHistory');if(!host)return;host.innerHTML=rows.length?rows.slice(-8).reverse().map(x=>`<div class="practice-result"><strong>${String(x.assessmentType||'Practice').toUpperCase()}</strong><div style="font-size:24px;font-weight:800;color:#6d35e8">${Number(x.score)||0}%</div><small>${Number(x.durationSeconds)||0}s · ${Array.isArray(x.insights)?x.insights[0]||'Saved practice result':''}</small></div>`).join(''):'<div class="module8-empty">No practice attempts yet. Start Coding or Aptitude practice above.</div>';}catch(e){}}
  document.addEventListener('DOMContentLoaded',()=>{
    qs('startCoding')?.addEventListener('click',()=>{codingState={i:0,answers:[],timer:1800,id:Date.now()};qs('codingSession')?.classList.add('active');if(qs('codingResult'))qs('codingResult').hidden=true;renderCoding();clearInterval(codingTicker);codingTicker=setInterval(()=>tick(codingState,'codingTimer',finishCoding),1000);qs('codingSession')?.scrollIntoView({behavior:'smooth',block:'start'});});
    qs('codingPrev')?.addEventListener('click',()=>{codingState.answers[codingState.i]=qs('codingEditor')?.value||'';if(codingState.i>0){codingState.i--;renderCoding();}});
    qs('codingNext')?.addEventListener('click',()=>{codingState.answers[codingState.i]=qs('codingEditor')?.value||'';if(codingState.i<coding.length-1){codingState.i++;renderCoding();}});
    qs('codingSubmit')?.addEventListener('click',finishCoding);
    qs('startAptitude')?.addEventListener('click',()=>{aptitudeState={i:0,answers:[],timer:900};qs('aptitudeSession')?.classList.add('active');if(qs('aptitudeResult'))qs('aptitudeResult').hidden=true;renderAptitude();clearInterval(aptitudeTicker);aptitudeTicker=setInterval(()=>tick(aptitudeState,'aptitudeTimer',finishAptitude),1000);qs('aptitudeSession')?.scrollIntoView({behavior:'smooth',block:'start'});});
    qs('aptitudePrev')?.addEventListener('click',()=>{const r=document.querySelector('input[name="aptitudeAnswer"]:checked');if(r)aptitudeState.answers[aptitudeState.i]=Number(r.value);if(aptitudeState.i>0){aptitudeState.i--;renderAptitude();}});
    qs('aptitudeNext')?.addEventListener('click',()=>{const r=document.querySelector('input[name="aptitudeAnswer"]:checked');if(r)aptitudeState.answers[aptitudeState.i]=Number(r.value);if(aptitudeState.i<aptitude.length-1){aptitudeState.i++;renderAptitude();}});
    qs('aptitudeSubmit')?.addEventListener('click',()=>{const r=document.querySelector('input[name="aptitudeAnswer"]:checked');if(r)aptitudeState.answers[aptitudeState.i]=Number(r.value);finishAptitude();});
    loadHistory();
  });
})();
