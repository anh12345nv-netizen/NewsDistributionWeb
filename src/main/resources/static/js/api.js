// js/api.js — include trong mọi trang: <script src="/js/api.js"></script>

const API_BASE = '/api';

// Kiểm tra auth khi load trang
function requireAuth(requiredRole) {
  const token = localStorage.getItem('accessToken');
  const role = localStorage.getItem('role');
  if (!token) { 
    window.location.href = '/login.html'; 
    return false; 
  }
  if (requiredRole && role !== requiredRole) {
    alert('Bạn không có quyền truy cập trang này');
    window.location.href = '/login.html';
    return false;
  }
  return true;
}

async function apiGet(path) {
  const res = await fetch(API_BASE + path, {
    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('accessToken') }
  });
  if (res.status === 401) { 
    localStorage.clear(); 
    window.location.href = '/login.html'; 
    return null;
  }
  return res.json();
}

async function apiPost(path, body) {
  const res = await fetch(API_BASE + path, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (res.status === 401) { 
    localStorage.clear(); 
    window.location.href = '/login.html'; 
    return null;
  }
  return res.json();
}

async function apiPut(path, body) {
  const res = await fetch(API_BASE + path, {
    method: 'PUT',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (res.status === 401) { 
    localStorage.clear(); 
    window.location.href = '/login.html'; 
    return null;
  }
  return res.json();
}

// Log activity (Heatmap)
function logActivity(action, elementId, x, y) {
  const token = localStorage.getItem('accessToken');
  if (!token) return;
  fetch(API_BASE + '/activity/log', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      page: window.location.pathname,
      action, elementId, x, y
    })
  }).catch(() => {}); // silent fail
}

// Auto-log mọi click
document.addEventListener('click', (e) => {
  if (e && e.target) {
    const elementId = e.target.id || e.target.tagName.toLowerCase();
    logActivity('CLICK', elementId, e.clientX, e.clientY);
  }
});

// Đăng xuất
function logout() {
  localStorage.clear();
  window.location.href = '/login.html';
}
