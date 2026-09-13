(function(){
'use strict';
const api = window.smartHireApi;
if(!api) return;

async function resolveUserId() {
  const cached = Number(localStorage.getItem('userId') || 0);
  if (cached > 0) return cached;
  try {
    const me = await api.requestJson('/api/profile/me');
    const uid = Number(me?.userId || me?.id || 0);
    if (uid > 0) {
      localStorage.setItem('userId', String(uid));
      return uid;
    }
  } catch (e) {}
  return 0;
}

const text = (id, v) => { const e = document.getElementById(id); if (e) e.textContent = v; };
const bar = (id, v) => { const e = document.getElementById(id); if (e) e.style.width = Math.max(0, Math.min(100, Number(v) || 0)) + '%'; };

async function load() {
  const uid = await resolveUserId();
  if (!uid) {
    text('latestFeedback', 'Sign in to track your improvement progress.');
    text('trendFeedback', 'Interview evaluations will appear here.');
    return;
  }
  try {
    const d = await api.requestJson('/api/analytics/candidate/' + uid);
    const s = d?.summary || {}, im = d?.improvement || [];
    text('growthProgress', s.evaluatedInterviews ? Math.round(s.averageOverallScore) + '%' : '—');
    text('practiceStreak', String(s.evaluatedInterviews || 0) + ' evaluated interview' + (s.evaluatedInterviews === 1 ? '' : 's'));
    text('nextGoal', d?.weakAreas?.[0]?.label || 'Keep practicing');
    text('readinessLevel', s.rating || 'Not enough data');
    const by = n => im.find(x => String(x.label).toLowerCase() === n.toLowerCase());
    bar('goalCommunication', by('Communication')?.latest ?? s.averageCommunication);
    bar('goalProblem', by('Problem Solving')?.latest ?? s.averageProblemSolving);
    bar('goalInterviews', Math.min(100, (s.evaluatedInterviews || 0) * 20));
    const weak = d?.weakAreas || [];
    text('focusFeedback', weak.length ? weak.slice(0, 2).map(x => `${x.label}${x.score ? ' — ' + x.score + '%' : ''}`).join(', ') : 'No weak areas identified yet.');
    text('latestFeedback', d?.feedback?.strengths?.[0] || d?.feedback?.recommendation || (s.evaluatedInterviews ? 'Complete another mock interview to see updated AI feedback.' : 'Latest AI feedback will appear after an evaluated interview.'));
    text('trendFeedback', s.evaluatedInterviews < 2 ? 'Complete at least two evaluated interviews to measure improvement over time.' : s.improvementDelta > 0 ? `Overall score improved by ${s.improvementDelta}% from your first evaluated session.` : s.improvementDelta < 0 ? `Overall score is ${Math.abs(s.improvementDelta)}% below your first evaluated session.` : 'Overall score is stable compared with your first evaluated session.');
  } catch (e) {
    console.warn('Progress analytics failed', e);
    text('latestFeedback', 'No completed evaluations found. Start an interview to begin tracking progress.');
    text('trendFeedback', 'Complete interviews to view improvement metrics.');
  }
}

document.addEventListener('DOMContentLoaded', load);
})();
