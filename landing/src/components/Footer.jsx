import { useLanguage } from '../i18n'

export default function Footer() {
  const { t } = useLanguage()

  return (
    <footer className="footer">
      <div className="footer__in">
        <div className="footer__brand">
          <span className="footer__mark">Ч</span>
          <span>Чатор</span>
        </div>
        <p className="footer__caption">{t.footer.caption}</p>
      </div>
    </footer>
  )
}
