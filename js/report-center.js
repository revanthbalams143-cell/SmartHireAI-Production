(function () {
  'use strict';
  const api = window.smartHireApi;
  if (!api) return;

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

  const set = (id, v) => {
    const e = document.getElementById(id);
    if (e) e.textContent = v;
  };
  const esc = (v) =>
    String(v ?? '').replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])
    );
  const pct = (v) => (Number.isFinite(Number(v)) ? Math.round(Number(v)) + '%' : '—');

  async function download(interviewId, button) {
    if (!interviewId) throw new Error('This report has no interview ID.');
    const b = button || null;
    const oldText = b ? b.innerHTML : '';
    if (b) {
      b.disabled = true;
      b.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Preparing…';
    }
    try {
      const r = await api.request('/api/analytics/interview/' + encodeURIComponent(interviewId) + '/report/pdf');
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const blob = await r.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'smarthire-interview-report-' + interviewId + '.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      window.smartHireToast ? window.smartHireToast('Report', 'Interview PDF downloaded.', 'success') : null;
    } catch (e) {
      window.smartHireToast
        ? window.smartHireToast('Report', e.message || 'Could not download this report', 'error')
        : alert(e.message || 'Could not download this report');
    } finally {
      if (b) {
        b.disabled = false;
        b.innerHTML = oldText || 'Download PDF';
      }
    }
  }

  async function downloadAnalyticsPdf(button) {
    const uid = await resolveUserId();
    if (!uid) return;
    const b = button || null;
    const oldText = b ? b.innerHTML : '';
    if (b) {
      b.disabled = true;
      b.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Preparing…';
    }
    try {
      const r = await api.request('/api/analytics/report/' + encodeURIComponent(uid));
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const blob = await r.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'smarthire-analytics-report-' + uid + '.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      window.smartHireToast ? window.smartHireToast('Analytics', 'Analytics PDF downloaded.', 'success') : null;
    } catch (e) {
      window.smartHireToast
        ? window.smartHireToast('Analytics', e.message || 'Could not download analytics report', 'error')
        : alert(e.message || 'Could not download analytics report');
    } finally {
      if (b) {
        b.disabled = false;
        b.innerHTML = oldText || 'Download Analytics PDF';
      }
    }
  }

  async function sendEmail(interviewId) {
    const to = prompt('Send the interview report to which email address?');
    if (!to) return;
    try {
      const data = await api.requestJson('/api/email/interview-report/' + encodeURIComponent(interviewId), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ recipient: to })
      });
      window.smartHireToast
        ? window.smartHireToast('Email Sent', data?.message || 'Report email delivered successfully.', 'success')
        : alert(data?.message || 'Email sent');
    } catch (e) {
      window.smartHireToast
        ? window.smartHireToast('Email Failed', e.message || 'Email delivery failed.', 'error')
        : alert(e.message || 'Email delivery failed');
    }
  }

  async function load() {
    const uid = await resolveUserId();
    if (!uid) return;

    try {
      const d = await api.requestJson('/api/analytics/candidate/' + uid);
      const s = d?.summary || {};
      set('latestRole', s.latestJobRole || 'No completed report yet');
      set(
        'latestDate',
        s.latestDate ? new Date(s.latestDate).toLocaleString() : 'Complete an AI interview to generate your report.'
      );
      set('latestScore', pct(s.latestOverallScore));
      set('latestTechnical', pct(s.averageTechnical));
      set('latestCommunication', pct(s.averageCommunication));
      set('latestProblem', pct(s.averageProblemSolving));

      const rows = (d?.history || [])
        .filter((x) => x && x.overallScore != null)
        .sort((a, b) => new Date(b.date || 0) - new Date(a.date || 0));
      const latest = rows[0];

      if (latest && latest.interviewId) {
        localStorage.setItem('smarthire.latestReportInterviewId', String(latest.interviewId));
        sessionStorage.setItem('selectedInterviewId', String(latest.interviewId));
      }

      // Update all primary detailed report links on page
      document.querySelectorAll('a[href="interview-report.html"], #detailedInterviewReportLink').forEach((a) => {
        if (latest?.interviewId) {
          a.href = `interview-report.html?interviewId=${encodeURIComponent(latest.interviewId)}`;
        }
      });

      const host = document.getElementById('candidateReportArchive');
      if (!host) return;

      if (!rows.length) {
        host.innerHTML =
          '<div class="module8-empty">No completed interviews yet. Finish an interview to create your first report.</div>';
        return;
      }

      host.innerHTML = rows
        .slice(0, 20)
        .map((x) => {
          const date = x.date ? new Date(x.date).toLocaleString() : '—';
          return `<article class="report-link-card report-archive-card">
            <div class="report-archive-head">
              <span class="setup-icon"><i class="fa-solid fa-file-waveform"></i></span>
              <div>
                <h3>${esc(x.jobRole || 'AI Mock Interview')}</h3>
                <p>${esc(x.interviewType || 'Interview')} • ${date}</p>
              </div>
              <strong>${pct(x.overallScore)}</strong>
            </div>
            <div class="report-archive-actions">
              <a class="primary" href="interview-report.html?interviewId=${encodeURIComponent(x.interviewId)}">View Report</a>
              <button type="button" class="table-btn report-pdf" data-id="${x.interviewId}">Download PDF</button>
              <button type="button" class="table-btn report-email" data-id="${x.interviewId}">Email Report</button>
            </div>
          </article>`;
        })
        .join('');

      host.querySelectorAll('.report-pdf').forEach((b) => {
        b.onclick = () => download(b.dataset.id, b);
      });
      host.querySelectorAll('.report-email').forEach((b) => {
        b.onclick = () => sendEmail(b.dataset.id);
      });
    } catch (e) {
      console.warn('Reports load failed', e);
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    load();
    const dlBtn = document.getElementById('downloadAnalyticsReport');
    if (dlBtn) {
      dlBtn.addEventListener('click', () => downloadAnalyticsPdf(dlBtn));
    }
  });
  })();
