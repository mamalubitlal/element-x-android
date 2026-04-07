import { useState, useEffect, useRef } from "react";
import logo from "./chator-logo.png";
import { translations } from "./translations";

/* ─── helpers ─── */
function useLang() {
  const [lang, setLang] = useState(() => localStorage.getItem("chator_lang") || "ru");
  const t = translations[lang] || translations.ru;
  const toggle = () => {
    const next = lang === "ru" ? "en" : "ru";
    setLang(next);
    localStorage.setItem("chator_lang", next);
  };
  return { lang, t, toggle };
}

function useReveal() {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      ([e]) => { if (e.isIntersecting) { e.target.classList.add("visible"); obs.unobserve(e.target); } },
      { threshold: 0.15 }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);
  return ref;
}

/* ─── Reveal wrapper ─── */
function Reveal({ children, className = "" }) {
  const ref = useReveal();
  return (
    <div ref={ref} className={`reveal ${className}`}>
      {children}
    </div>
  );
}

/* ─── NAV ─── */
function Nav({ t, lang, toggleLang }) {
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <nav className="nav">
      <div className="nav__inner">
        <a href="#" className="nav__logo" onClick={close}>
          <img src={logo} alt="Чатор" width="32" height="32" />
          <span>{t.logo_name}</span>
        </a>
        <button className="nav__burger" onClick={() => setOpen(!open)} aria-label="Menu">
          <span /><span /><span />
        </button>
        <ul className={`nav__links ${open ? "open" : ""}`}>
          <li><a href="#features" onClick={close}>{t.nav_features}</a></li>
          <li><a href="#about" onClick={close}>{t.nav_about}</a></li>
          <li><a href="#screenshots" onClick={close}>{t.nav_screenshots}</a></li>
          <li><a href="#install" onClick={close}>{t.nav_install}</a></li>
          <li><a href="#contribute" onClick={close}>{t.nav_contribute}</a></li>
          <li>
            <button className="lang-btn" onClick={toggleLang}>{t.lang_switch_label}</button>
          </li>
        </ul>
      </div>
    </nav>
  );
}

/* ─── HERO ─── */
function Hero({ t }) {
  return (
    <header className="hero">
      <div className="hero__bg">
        <div className="blob blob--blue" />
        <div className="blob blob--violet" />
        <div className="blob blob--cyan" />
      </div>
      <div className="hero__inner">
        <div className="hero__text">
          <h1 className="hero__title">
            {t.hero_title} <span className="accent">{t.hero_title_accent}</span>
          </h1>
          <p className="hero__subtitle">{t.hero_subtitle}</p>
          <div className="hero__actions">
            <a href="https://github.com/mamalubitlal/element-x-android/releases" className="btn btn--primary" target="_blank" rel="noopener">
              {t.hero_download}
            </a>
            <a href="https://github.com/mamalubitlal/element-x-android" className="btn btn--outline" target="_blank" rel="noopener">
              {t.hero_github}
            </a>
          </div>
        </div>
        <div className="phone">
          <div className="phone__screen">
            {/* Matrix-style top bar */}
            <div className="phone__topbar">
              <svg className="phone__back" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 18l-6-6 6-6" />
              </svg>
              <div className="phone__room-avatar">Ч</div>
              <div className="phone__room-info">
                <span className="phone__room-name">{t.logo_name}</span>
                <span className="phone__room-members">{t.phone_member_count || ""}</span>
              </div>
            </div>
            {/* Message bubbles (Matrix-style) */}
            <div className="phone__body">
              {/* Incoming: 1st msg (new thread) */}
              <div className="tl-row tl-row--other">
                <div className="tl-avatar" style={{ background: "linear-gradient(135deg,#6366f1,#4f46e5)" }}>А</div>
                <div className="tl-col">
                  <span className="tl-name">Алексей</span>
                  <div className="tl-body tl-body--first">{t.hero_chat_1}</div>
                </div>
              </div>
              {/* Outgoing: 1st msg */}
              <div className="tl-row tl-row--self tl-row--first">
                <div className="tl-body tl-body--sent">{t.hero_chat_2}</div>
              </div>
              {/* Incoming: new message with avatar */}
              <div className="tl-row tl-row--other">
                <div className="tl-avatar" style={{ background: "linear-gradient(135deg,#6366f1,#4f46e5)" }}>А</div>
                <div className="tl-col">
                  <div className="tl-body tl-body--first">{t.hero_chat_3}</div>
                </div>
              </div>
              {/* Outgoing: group start */}
              <div className="tl-row tl-row--self tl-row--first">
                <div className="tl-body tl-body--sent tl-body--group-start">{t.hero_chat_4}</div>
              </div>
              <div className="tl-row tl-row--self tl-row--same">
                <div className="tl-body tl-body--sent">{t.hero_chat_5}</div>
              </div>
              {/* Read receipts */}
              <div className="tl-receipts">
                <svg viewBox="0 0 16 12" width="14" height="11" fill="none" stroke="var(--primary-light)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" opacity=".6">
                  <path d="M1 6l3.5 4L13 1" /><path d="M5 6l3.5 4L13 1" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}

/* ─── FEATURES ─── */
function Features({ t }) {
  const feats = [
    { icon: "\uD83C\uDDE7", title: t.feature_russian_title, desc: t.feature_russian_desc },
    { icon: "\uD83D\uDD12", title: t.feature_crypto_title, desc: t.feature_crypto_desc },
    { icon: "\uD83D\uDCDE", title: t.feature_calls_title, desc: t.feature_calls_desc },
  ];
  return (
    <section className="features" id="features">
      <div className="container">
        <h2 className="section-title">{t.features_title}</h2>
        <div className="features__grid">
          {feats.map((f, i) => (
            <Reveal key={i}>
              <div className="feature">
                <div className="feature__icon">{f.icon}</div>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─── ABOUT ─── */
function About({ t }) {
  return (
    <section className="about" id="about">
      <div className="container">
        <h2 className="section-title">{t.about_title}</h2>
        <div className="about__grid">
          <Reveal>
            <div className="about__card">
              <h3>{t.about_matrix_title}</h3>
              <p>{t.about_matrix_desc}</p>
              <a href="https://matrix.org" className="about__link" target="_blank" rel="noopener">
                {t.about_matrix_link} →
              </a>
            </div>
          </Reveal>
          <Reveal>
            <div className="about__card">
              <h3>{t.about_tech_title}</h3>
              <ul className="about__tech">
                <li><strong>{t.about_tech_rust}</strong> {t.about_tech_rust_desc}</li>
                <li><strong>{t.about_tech_compose}</strong> {t.about_tech_compose_desc}</li>
                <li><strong>{t.about_tech_jitsi}</strong> {t.about_tech_jitsi_desc}</li>
                <li><strong>{t.about_tech_sso}</strong> {t.about_tech_sso_desc}</li>
              </ul>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

/* ─── SCREENSHOTS ─── */
function Screenshots({ t }) {
  const icons = ["\uD83D\uDCF1", "\uD83D\uDCAC", "\uD83D\uDCDE", "\u2699\uFE0F"];
  return (
    <section className="screenshots" id="screenshots">
      <div className="container">
        <h2 className="section-title">{t.screenshots_title}</h2>
        {t.screenshots_subtitle && <p className="section-subtitle">{t.screenshots_subtitle}</p>}
        <div className="screenshots__grid">
          {icons.map((ic, i) => (
            <Reveal key={i}>
              <div className="sshot">
                <div className="sshot__icon">{ic}</div>
                Screenshot
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─── INSTALL ─── */
function Install({ t }) {
  return (
    <section className="install" id="install">
      <div className="container">
        <h2 className="section-title">{t.install_title}</h2>
        <div className="install__grid">
          <Reveal>
            <div className="install__card">
              <h3>{t.install_apk_title}</h3>
              <p>{t.install_apk_desc}</p>
              <a href="https://github.com/mamalubitlal/element-x-android/releases" className="btn btn--primary" target="_blank" rel="noopener">
                {t.install_apk_btn}
              </a>
            </div>
          </Reveal>
          <Reveal>
            <div className="install__card">
              <h3>{t.install_build_title}</h3>
              <p>{t.install_build_desc}</p>
              <pre className="install__code"><code>{`git clone https://github.com/mamalubitlal/element-x-android.git
cd element-x-android
# Open in Android Studio → run "app" configuration`}</code></pre>
              <a href="https://github.com/mamalubitlal/element-x-android/blob/develop/docs/_developer_onboarding.md" className="btn btn--outline" target="_blank" rel="noopener">
                {t.install_build_btn}
              </a>
            </div>
          </Reveal>
        </div>
        <Reveal>
          <div className="install__reqs">
            <h4>{t.install_reqs_title}</h4>
            <ul>
              <li><strong>{t.install_req_1a}</strong>{t.install_req_1b}</li>
              <li><strong>{t.install_req_2a}</strong>{t.install_req_2b}</li>
            </ul>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

/* ─── CONTRIBUTE ─── */
function Contribute({ t }) {
  const steps = [
    <p key="1">
      {t.contribute_step_1_start}
      <a href="https://github.com/mamalubitlal/element-x-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22" target="_blank" rel="noopener">
        {t.contribute_step_1_link}
      </a>
    </p>,
    <p key="2">{t.contribute_step_2}</p>,
    <p key="3">
      {t.contribute_step_3_start}
      <a href="https://github.com/mamalubitlal/element-x-android/blob/develop/CONTRIBUTING.md" target="_blank" rel="noopener">
        {t.contribute_step_3_link}
      </a>
    </p>,
  ];
  return (
    <section className="contribute" id="contribute">
      <div className="container">
        <h2 className="section-title">{t.contribute_title}</h2>
        <div className="contribute__steps">
          {steps.map((s, i) => (
            <Reveal key={i}>
              <div className="contribute__step">
                <span className="step-num">{i + 1}</span>
                {s}
              </div>
            </Reveal>
          ))}
        </div>
        <a href="https://github.com/mamalubitlal/element-x-android/issues" className="btn btn--primary" target="_blank" rel="noopener">
          {t.contribute_btn}
        </a>
      </div>
    </section>
  );
}

/* ─── FOOTER ─── */
function Footer({ t }) {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer__inner">
          <div className="footer__brand">
            <img src={logo} alt="Чатор" width="26" height="26" />
            <span>{t.footer_name}</span>
          </div>
          <p className="footer__license">{t.footer_legal}</p>
        </div>
      </div>
    </footer>
  );
}

/* ─── APP ── */
export default function App() {
  const { lang, t, toggle } = useLang();
  useEffect(() => { document.documentElement.lang = lang; }, [lang]);
  return (
    <>
      <Nav t={t} lang={lang} toggleLang={toggle} />
      <Hero t={t} />
      <Features t={t} />
      <About t={t} />
      <Screenshots t={t} />
      <Install t={t} />
      <Contribute t={t} />
      <Footer t={t} />
    </>
  );
}
