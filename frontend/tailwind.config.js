// Builds a Tailwind color scale that reads from CSS variables defined in index.css, so
// `text-ink-900`, `border-ink-100/70`, etc. resolve to different values per [data-theme]
// without any component needing a dark: variant.
function shade(name) {
  const scale = {};
  for (const step of [50, 100, 200, 300, 400, 500, 600, 700, 800, 900]) {
    scale[step] = `rgb(var(--${name}-${step}) / <alpha-value>)`;
  }
  scale.soft = `var(--${name}-soft)`;
  scale.deep = `var(--${name}-deep)`;
  return scale;
}

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        // Every numbered scale reads from a CSS variable (see index.css :root /
        // [data-theme="dark"]) via the rgb(var(..) / <alpha-value>) pattern, so opacity
        // modifiers like `border-ink-100/70` keep working and light/dark is a single
        // attribute flip - no per-component dark: variants needed for these tokens.
        ink: { ...shade("ink"), 950: "rgb(var(--ink-950) / <alpha-value>)" },
        signal: shade("signal"),
        gold: shade("gold"),
        alert: shade("alert"),
        danger: shade("danger"),
        mint: shade("mint"),
        paper: {
          DEFAULT: "rgb(var(--paper-default) / <alpha-value>)",
          50: "rgb(var(--paper-50) / <alpha-value>)",
          100: "rgb(var(--paper-100) / <alpha-value>)",
          200: "rgb(var(--paper-200) / <alpha-value>)",
          300: "rgb(var(--paper-300) / <alpha-value>)",
        },
        // Glass/card fill roles: solid in light mode, translucent-on-black in dark mode.
        // The formula itself (not just the hue) differs per theme, so these hold whole
        // color values rather than triplets - see index.css.
        surface: {
          DEFAULT: "var(--surface)",
          hover: "var(--surface-hover)",
          input: "var(--surface-input)",
          border: "var(--surface-border)",
        },
      },
      fontFamily: {
        display: ["'Space Grotesk'", "sans-serif"],
        sans: ["'Inter'", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["'JetBrains Mono'", "ui-monospace", "monospace"],
      },
      boxShadow: {
        card: "var(--shadow-card)",
        "card-hover": "var(--shadow-card-hover)",
        pop: "var(--shadow-pop)",
      },
      borderRadius: {
        xl2: "1rem",
        avatar: "11px",
      },
      keyframes: {
        "fade-in": {
          "0%": { opacity: 0, transform: "translateY(4px)" },
          "100%": { opacity: 1, transform: "translateY(0)" },
        },
        "signal-ignite": {
          "0%": { transform: "scale(0)", opacity: 0 },
          "60%": { transform: "scale(1.08)", opacity: 1 },
          "100%": { transform: "scale(1)", opacity: 1 },
        },
        "signal-ring": {
          "0%": { transform: "scale(0.4)", opacity: 0.5 },
          "70%": { opacity: 0.12 },
          "100%": { transform: "scale(1)", opacity: 0 },
        },
        "signal-chime": {
          "0%": { transform: "scale(0.5)", opacity: 0 },
          "15%": { opacity: 0.9 },
          "100%": { transform: "scale(1.2)", opacity: 0 },
        },
        "wordmark-in": {
          "0%": { opacity: 0, transform: "translateX(-6px)" },
          "100%": { opacity: 1, transform: "translateX(0)" },
        },
        "lockup-settle": {
          "0%": { transform: "scale(1)" },
          "100%": { transform: "scale(0.8)" },
        },
        "splash-in": {
          "0%": { opacity: 0 },
          "100%": { opacity: 1 },
        },
        "splash-out": {
          "0%": { opacity: 1, transform: "scale(1)" },
          "100%": { opacity: 0, transform: "scale(1.04)" },
        },
        drift: {
          "0%": { transform: "translate(0, 0) scale(1)" },
          "100%": { transform: "translate(40px, 30px) scale(1.08)" },
        },
      },
      animation: {
        "fade-in": "fade-in 0.25s ease-out",
        "signal-ignite": "signal-ignite 0.8s cubic-bezier(0.34,1.56,0.64,1) both",
        "signal-ring": "signal-ring 2.8s cubic-bezier(0.16,1,0.3,1) infinite",
        "signal-chime": "signal-chime 0.8s cubic-bezier(0.16,1,0.3,1) both",
        "wordmark-in": "wordmark-in 0.9s cubic-bezier(0.16,1,0.3,1) both",
        "lockup-settle": "lockup-settle 0.8s cubic-bezier(0.65,0,0.35,1) both",
        "splash-in": "splash-in 0.25s ease-out both",
        "splash-out": "splash-out 0.42s cubic-bezier(0.4,0,1,1) both",
        drift: "drift 22s ease-in-out infinite alternate",
      },
    },
  },
  plugins: [],
};
