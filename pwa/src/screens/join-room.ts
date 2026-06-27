import { el, icon, btn, input, avatar, divider, listItem, sectionHeader, toggle, checkbox } from '../components/ui';
import { T, space, radius } from '../theme';

// Add CSS animations for spinner and feedback
const style = document.createElement('style');
style.textContent = `
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes joinRoomFadeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(-10px); }
  to { opacity: 1; transform: translateX(-50%); }
}

@keyframes joinRoomFadeOut {
  from { opacity: 1; transform: translateX(-50%); }
  to { opacity: 0; transform: translateX(-50%) translateY(-10px); }
}
`;
document.head.appendChild(style);

export function renderJoinRoom(root: HTMLElement, navigate: (path: string) => void) {
  const screen = el('div', {
    style: [
      `width:100%;height:100%;display:flex;flex-direction:column;`,
      `background:${T.bg};overflow:hidden;`,
    ].join(''),
  });

  // ─── Header ─────────────────────────────────────
  const header = el('div', {
    style: [
      `display:flex;align-items:center;gap:${space.md}px;`,
      `padding:${space.lg}px ${space.lg}px;`,
      `padding-top:max(${space.lg}px,env(safe-area-inset-top));`,
      `border-bottom:1px solid ${T.divider};`,
    ].join(''),
  });
  const backBtn = el('div', {
    style: `width:40px;height:40px;display:flex;align-items:center;justify-content:center;cursor:pointer;`,
  }, icon('arrow_back'));
  (backBtn.querySelector('.material-symbols-outlined') as HTMLElement).style.color = T.text;
  backBtn.addEventListener('click', () => navigate('/home'));
  header.appendChild(backBtn);
  header.appendChild(el('div', {
    style: `font-size:20px;font-weight:600;color:${T.text};flex:1;`,
  }, 'Присоединиться к комнате'));
  screen.appendChild(header);

  // ─── Content ───────────────────────────────────
  const content = el('div', {
    style: [
      `flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;`,
      `padding:${space.xl}px;`,
    ].join(''),
  });

  // Room avatar placeholder
  const avatar = el('div', {
    style: [
      `width:96px;height:96px;border-radius:50%;`,
      `background:${T.bgTertiary};`,
      `display:flex;align-items:center;justify-content:center;`,
      `margin-bottom:${space.xl}px;`,
    ].join(''),
  });
  const avatarIcon = icon('group', 48);
  (avatarIcon.querySelector('.material-symbols-outlined') as HTMLElement).style.color = T.textSecondary;
  avatar.appendChild(avatarIcon);
  content.appendChild(avatar);

  // Room address with copy icon
  const addressContainer = el('div', {
    style: [
      `display:flex;align-items:center;gap:${space.sm}px;`,
      `margin-bottom:${space.xl}px;`,
    ].join(''),
  });
  const addressText = el('div', {
    style: [
      `font-size:18px;font-weight:500;color:${T.text};`,
    ].join(''),
  }, '#exa:matrix.org');
  const copyBtn = el('div', {
    style: [
      `width:24px;height:24px;display:flex;align-items:center;justify-content:center;cursor:pointer;`,
    ].join(''),
    title: 'Копировать адрес',
  }, icon('content_copy', 20));
  (copyBtn.querySelector('.material-symbols-outlined') as HTMLElement).style.color = T.textSecondary;
  
  let copyFeedback: HTMLElement | null = null;
  
  copyBtn.addEventListener('click', () => {
    navigator.clipboard.writeText('#exa:matrix.org');
    
    if (!copyFeedback) {
      copyFeedback = el('div', {
        style: [
          `position:absolute;top:-32px;left:50%;transform:translateX(-50%);`,
          `background:${T.bgSecondary};color:${T.text};`,
          `padding:${space.xs}px ${space.sm}px;border-radius:${radius.sm}px;`,
          `font-size:12px;white-space:nowrap;`,
          `opacity:0;animation:joinRoomFadeIn 0.2s ease-out forwards;`,
        ].join(''),
      }, 'Скопировано');
      copyBtn.appendChild(copyFeedback);
      
      setTimeout(() => {
        if (copyFeedback) {
          copyFeedback.style.animation = 'joinRoomFadeOut 0.2s ease-in forwards';
          setTimeout(() => {
            if (copyFeedback && copyFeedback.parentNode) {
              copyFeedback.parentNode.removeChild(copyFeedback);
              copyFeedback = null;
            }
          }, 200);
        }
      }, 1500);
    }
  });
  
  addressContainer.appendChild(addressText);
  addressContainer.appendChild(copyBtn);
  content.appendChild(addressContainer);

  // Loading spinner
  const spinner = el('div', {
    style: [
      `width:32px;height:32px;border-radius:50%;`,
      `border:3px solid ${T.divider};`,
      `border-top-color:${T.accent};`,
      `animation:spin 1s linear infinite;`,
      `margin-bottom:${space.xl}px;`,
    ].join(''),
  });
  content.appendChild(spinner);

  screen.appendChild(content);

  // ─── Footer ───────────────────────────────────
  const footer = el('div', {
    style: [
      `padding:${space.lg}px;`,
      `padding-bottom:max(${space.lg}px,env(safe-area-inset-bottom));`,
    ].join(''),
  });

  // Primary button
  const joinButton = btn('Вступить', {
    variant: 'primary',
    block: true,
    onClick: () => {
      // TODO: Implement join logic
    },
  });
  footer.appendChild(joinButton);

  // Terms text
  const termsText = el('div', {
    style: [
      `margin-top:${space.md}px;text-align:center;`,
      `font-size:12px;line-height:1.4;color:${T.textTertiary};`,
    ].join(''),
  }, 'Нажимая «Вступить», вы принимаете правила сервера.');
  footer.appendChild(termsText);

  screen.appendChild(footer);

  root.appendChild(screen);
}
