// Inject UI into body
document.addEventListener('DOMContentLoaded', () => {
  const cssLink = document.createElement('link');
  cssLink.rel = 'stylesheet';
  cssLink.href = '/css/chatbot.css';
  document.head.appendChild(cssLink);

  const chatbotHtml = `
    <div id="ai-chat-widget">
      <button id="ai-chat-btn" onclick="toggleChat()" style="padding: 0; background: transparent; overflow: hidden; border: 2px solid white; display: block;">
        <img src="/images/logo.png" alt="AI" style="width: 100%; height: 100%; object-fit: cover; background: white;">
      </button>
      
      <div id="ai-chat-window">
        <div id="ai-chat-header">
          <div id="ai-chat-header-title">
            <img src="/images/logo.png" alt="Logo" style="width: 20px; height: 20px; border-radius: 50%; background: white; padding: 1px;"> Trợ lí thông minh
          </div>
          <button id="ai-chat-close" onclick="toggleChat()">
            <i data-lucide="x"></i>
          </button>
        </div>
        
        <div id="ai-chat-messages">
          <div class="chat-msg ai">Xin chào! Tôi là Trợ lí thông minh. Bạn cần hỗ trợ gì về số liệu hôm nay?</div>
        </div>
        
        <div id="ai-chat-input-area">
          <input type="text" id="ai-chat-input" placeholder="Hỏi AI về doanh thu, đơn hàng..." onkeypress="handleEnter(event)">
          <button id="ai-chat-send" onclick="sendChat()">
            <i data-lucide="send" style="width:16px;height:16px;"></i>
          </button>
        </div>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML('beforeend', chatbotHtml);
  if (window.lucide) {
    lucide.createIcons();
  }
});

function toggleChat() {
  const win = document.getElementById('ai-chat-window');
  if (win.style.display === 'flex') {
    win.style.display = 'none';
  } else {
    win.style.display = 'flex';
    document.getElementById('ai-chat-input').focus();
  }
}

function handleEnter(e) {
  if (e.key === 'Enter') {
    sendChat();
  }
}

async function sendChat() {
  const input = document.getElementById('ai-chat-input');
  const msg = input.value.trim();
  if (!msg) return;

  // Add user msg
  const msgs = document.getElementById('ai-chat-messages');
  msgs.insertAdjacentHTML('beforeend', `<div class="chat-msg user">${escapeHtml(msg)}</div>`);
  input.value = '';
  msgs.scrollTop = msgs.scrollHeight;

  // Add typing
  const typingId = 'typing-' + Date.now();
  msgs.insertAdjacentHTML('beforeend', `
    <div id="${typingId}" class="chat-msg ai">
      <div class="typing-indicator"><span></span><span></span><span></span></div>
    </div>
  `);
  msgs.scrollTop = msgs.scrollHeight;

  try {
    const token = localStorage.getItem('accessToken');
    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({ message: msg })
    });
    const data = await res.json();
    
    document.getElementById(typingId).remove();
    
    if (res.ok && data.reply) {
      msgs.insertAdjacentHTML('beforeend', `<div class="chat-msg ai">${renderAiText(data.reply)}</div>`);
    } else {
      msgs.insertAdjacentHTML('beforeend', `<div class="chat-msg ai" style="color:#ef4444;">${escapeHtml(data.error || 'Có lỗi xảy ra.')}</div>`);
    }
  } catch (e) {
    document.getElementById(typingId).remove();
    msgs.insertAdjacentHTML('beforeend', `<div class="chat-msg ai" style="color:#ef4444;">Lỗi kết nối.</div>`);
  }
  
  msgs.scrollTop = msgs.scrollHeight;
}

function escapeHtml(unsafe) {
    return unsafe
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

function renderAiText(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\*\*(.*?)\*\*/g, '$1')   // remove bold markdown
        .replace(/\*(.*?)\*/g, '$1')        // remove italic markdown
        .replace(/#{1,6}\s?/g, '')          // remove headers
        .replace(/\n/g, '<br>');            // newlines to <br>
}
