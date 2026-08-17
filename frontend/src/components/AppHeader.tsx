import type { ThemeId } from '../types/chat'
import { themes } from '../types/chat'

export function AppHeader({
  selectedThemeId,
  onThemeChange,
}: {
  selectedThemeId: ThemeId
  onThemeChange: (themeId: ThemeId) => void
}) {
  return (
    <header className="theme-bar">
      <p className="eyebrow">AI RAG Subject Matter Expert</p>
      <div className="theme-actions">
        <label className="field theme-field" htmlFor="theme">
          <span>Theme</span>
          <select
            id="theme"
            value={selectedThemeId}
            onChange={(event) => onThemeChange(event.target.value as ThemeId)}
          >
            {themes.map((theme) => (
              <option key={theme.id} value={theme.id}>
                {theme.label}
              </option>
            ))}
          </select>
        </label>
        <a
          className="repository-link"
          href="https://github.com/jojczykp/ai-rag-subject-matter-expert"
          target="_blank"
          rel="noreferrer"
          aria-label="View application source code on GitHub"
        >
          <svg
            aria-hidden="true"
            className="repository-link-icon"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M12 2.25c-5.37 0-9.75 4.38-9.75 9.78 0 4.32 2.79 7.98 6.66 9.27.48.09.66-.21.66-.48 0-.24-.03-1.05-.03-1.92-2.46.45-3.09-.6-3.3-1.14-.12-.27-.63-1.14-1.08-1.38-.36-.18-.87-.63-.03-.66.81-.03 1.38.75 1.56 1.05.93 1.56 2.4 1.11 3 .84.09-.66.36-1.11.66-1.38-2.16-.24-4.41-1.08-4.41-4.8 0-1.05.39-1.92 1.02-2.61-.09-.24-.45-1.23.12-2.58 0 0 .84-.27 2.7 1.02.78-.21 1.62-.33 2.46-.33s1.68.12 2.46.33c1.86-1.26 2.7-1.02 2.7-1.02.57 1.35.21 2.34.12 2.58.63.69 1.02 1.56 1.02 2.61 0 3.72-2.25 4.56-4.41 4.8.36.3.69.9.69 1.83 0 1.32-.03 2.37-.03 2.7 0 .27.18.57.66.48a9.77 9.77 0 0 0 6.6-9.24c0-5.4-4.38-9.78-9.75-9.78Z"
            />
          </svg>
          <span>Source code</span>
          <svg
            aria-hidden="true"
            className="repository-link-icon"
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M15 3h6v6"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
            />
            <path
              d="M10 14 21 3"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
            />
            <path
              d="M21 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h6"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
            />
          </svg>
        </a>
      </div>
    </header>
  )
}
