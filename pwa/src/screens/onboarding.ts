import { el, icon } from '../components/ui';
import { T, isDark, space, radius } from '../theme';

export function renderOnboarding(root: HTMLElement, navigate: (path: string) => void) {
  const dark = isDark();

  const screen = el('div', {
    style: [
      `width:100%;height:100%;display:flex;flex-direction:column;align-items:stretch;`,
      `position:relative;overflow:hidden;`,
    ].join(''),
  });

  // Gradient background
  const bg = el('div', {
    style: [
      `position:absolute;inset:0;z-index:0;`,
      dark
        ? `background:linear-gradient(180deg, #0D2137 0%, #0A1628 40%, #111111 100%);`
        : `background:linear-gradient(180deg, #D4F0FF 0%, #B8E6FF 25%, #E8F4F8 50%, #FFFFFF 100%);`,
    ].join(''),
  });
  screen.appendChild(bg);

  // Content
  const content = el('div', {
    className: 'auth-screen__content',
  });
  content.setAttribute('style', [
      `z-index:1;flex:1;display:flex;flex-direction:column;width:100%;`,
      `align-items:center;justify-content:center;padding:${space.xxxl}px ${space.xl}px;box-sizing:border-box;`,
    ].join(''));

  // Logo in rounded square
  const logoContainer = el('div', {
    style: [
      `width:128px;height:128px;border-radius:32px;`,
      `background:${dark ? 'rgba(255,255,255,0.1)' : 'rgba(255,255,255,0.7)'};`,
      `backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px);`,
      `border:1px solid ${dark ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.8)'};`,
      `display:flex;align-items:center;justify-content:center;`,
      `margin-bottom:${space.xxl}px;box-shadow:0 8px 32px rgba(0,0,0,0.1);`,
    ].join(''),
  });

  // Logo image
  const logoImg = el('img', {
    src: '/onboarding_logo.png',
    alt: 'Chator',
    style: `width:80px;height:80px;object-fit:contain;`,
  }) as HTMLImageElement;
  logoContainer.appendChild(logoImg);
  content.appendChild(logoContainer);

  // Title
  content.appendChild(el('h1', {
    style: [
      `font-size:28px;font-weight:700;text-align:center;`,
      `color:${T.accent};margin-bottom:${space.md}px;`,
    ].join(''),
  }, 'Общайся свободно.'));

  // Subtitle
  content.appendChild(el('p', {
    style: [
      `font-size:16px;color:${dark ? 'rgba(255,255,255,0.7)' : T.textSecondary};`,
      `text-align:center;line-height:1.5;max-width:280px;`,
    ].join(''),
  }, 'Добро пожаловать в быстрый и простой чатор.'));

  screen.appendChild(content);

  // Bottom section
  const bottom = el('div', {
    style: [
      `position:relative;z-index:1;padding:${space.xl}px ${space.xl}px;width:100%;box-sizing:border-box;`,
      `padding-bottom:max(${space.xl}px, env(safe-area-inset-bottom));`,
      `display:flex;flex-direction:column;align-items:center;gap:${space.lg}px;`,
    ].join(''),
  });

  // Continue button
  const continueBtn = el('button', {
    style: [
      `width:100%;height:56px;border-radius:${radius.pill}px;`,
      `background:${T.primary};color:${T.primaryText};`,
      `font-size:16px;font-weight:600;font-family:inherit;`,
      `border:none;cursor:pointer;transition:opacity 0.15s;`,
    ].join(''),
  }, 'Продолжить');
  continueBtn.addEventListener('click', () => navigate('/qr-login'));
  continueBtn.addEventListener('pointerdown', () => { continueBtn.style.opacity = '0.8'; });
  continueBtn.addEventListener('pointerup', () => { continueBtn.style.opacity = '1'; });
  bottom.appendChild(continueBtn);

  // Version
  bottom.appendChild(el('div', {
    style: `font-size:13px;color:${dark ? 'rgba(255,255,255,0.4)' : T.textTertiary};`,
  }, 'Версия 1.0.0'));

  screen.appendChild(bottom);
  root.appendChild(screen);
}
