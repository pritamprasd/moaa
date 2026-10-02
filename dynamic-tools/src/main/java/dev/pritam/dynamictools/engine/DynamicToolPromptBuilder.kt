package dev.pritam.dynamictools.engine

import dev.pritam.dynamictools.model.DynamicToolBundle

object DynamicToolPromptBuilder {

    /**
     * The mandatory glassmorphism CSS foundation that EVERY generated tool must include.
     * This is injected into the system prompt to guarantee design consistency.
     */
    private val GLASSMORPHISM_CSS_FOUNDATION = """
        /* ===== MOAA GLASSMORPHISM DESIGN SYSTEM – MANDATORY BASE ===== */
        :root {
          --bg-deep: #030712;
          --bg: #0B0F19;
          --glass-card: rgba(30, 41, 59, 0.65);
          --glass-border: rgba(148, 163, 184, 0.12);
          --glass-border-highlight: rgba(148, 163, 184, 0.25);
          --glass-blur: blur(16px);
          --accent-cyan: #38BDF8;
          --accent-violet: #A78BFA;
          --accent-rose: #F472B6;
          --accent-emerald: #34D399;
          --accent-amber: #F59E0B;
          --text-primary: #F1F5F9;
          --text-secondary: #94A3B8;
          --text-tertiary: #475569;
          --radius-card: 16px;
          --radius-btn: 10px;
          --shadow-glow: 0 0 24px rgba(56, 189, 248, 0.18), 0 8px 32px rgba(0,0,0,0.4);
        }
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
          min-height: 100vh;
          background: var(--bg-deep);
          background-image: radial-gradient(ellipse at 20% 20%, rgba(56,189,248,0.07) 0%, transparent 60%),
                            radial-gradient(ellipse at 80% 80%, rgba(167,139,250,0.06) 0%, transparent 55%);
          color: var(--text-primary);
          font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
          padding: 12px;
          overflow-x: hidden;
        }
        .app-container {
          max-width: 600px;
          margin: 0 auto;
          width: 100%;
          display: flex;
          flex-direction: column;
          gap: 14px;
          padding-bottom: 24px;
        }
        /* Glass card */
        .glass-card {
          background: var(--glass-card);
          backdrop-filter: var(--glass-blur);
          -webkit-backdrop-filter: var(--glass-blur);
          border: 1px solid var(--glass-border);
          border-radius: var(--radius-card);
          padding: 18px;
          box-shadow: var(--shadow-glow);
          position: relative;
          overflow: hidden;
        }
        .glass-card::before {
          content: '';
          position: absolute;
          inset: 0;
          border-radius: inherit;
          background: linear-gradient(135deg, rgba(255,255,255,0.04) 0%, transparent 60%);
          pointer-events: none;
        }
        /* Buttons */
        .btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          min-height: 44px;
          padding: 0 20px;
          border-radius: var(--radius-btn);
          border: 1px solid var(--glass-border-highlight);
          background: rgba(56,189,248,0.12);
          color: var(--accent-cyan);
          font-family: inherit;
          font-size: 14px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.18s ease;
          letter-spacing: 0.3px;
        }
        .btn:hover { background: rgba(56,189,248,0.22); border-color: var(--accent-cyan); box-shadow: 0 0 12px rgba(56,189,248,0.3); }
        .btn:active { transform: scale(0.97); filter: brightness(1.15); }
        .btn-full { width: 100%; }
        .btn-accent { background: rgba(56,189,248,0.2); border-color: rgba(56,189,248,0.4); color: var(--accent-cyan); }
        .btn-violet { background: rgba(167,139,250,0.15); border-color: rgba(167,139,250,0.3); color: var(--accent-violet); }
        .btn-danger { background: rgba(244,114,182,0.12); border-color: rgba(244,114,182,0.25); color: var(--accent-rose); }
        /* Inputs */
        .input {
          width: 100%;
          min-height: 44px;
          padding: 10px 14px;
          background: rgba(15,23,42,0.8);
          border: 1px solid var(--glass-border);
          border-radius: 10px;
          color: var(--text-primary);
          font-family: inherit;
          font-size: 14px;
          outline: none;
          transition: border-color 0.18s;
        }
        .input:focus { border-color: var(--accent-cyan); box-shadow: 0 0 8px rgba(56,189,248,0.2); }
        .input::placeholder { color: var(--text-tertiary); }
        textarea.input { resize: vertical; min-height: 88px; }
        /* Typography */
        .label { font-size: 11px; font-weight: 700; color: var(--text-secondary); letter-spacing: 1px; text-transform: uppercase; margin-bottom: 6px; }
        .title { font-size: 18px; font-weight: 800; color: var(--text-primary); letter-spacing: -0.3px; }
        .subtitle { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
        /* Badge */
        .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 10px; font-weight: 700; letter-spacing: 0.5px; }
        .badge-cyan { background: rgba(56,189,248,0.15); color: var(--accent-cyan); border: 1px solid rgba(56,189,248,0.25); }
        .badge-violet { background: rgba(167,139,250,0.15); color: var(--accent-violet); border: 1px solid rgba(167,139,250,0.25); }
        /* Divider */
        .divider { height: 1px; background: var(--glass-border); margin: 12px 0; }
        /* Result / output area */
        .result-box {
          background: rgba(3,7,18,0.7);
          border: 1px solid var(--glass-border);
          border-radius: 10px;
          padding: 14px;
          font-family: 'Courier New', monospace;
          font-size: 13px;
          color: var(--accent-cyan);
          word-break: break-all;
          min-height: 48px;
        }
        /* Grid helpers */
        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
        .flex-row { display: flex; flex-direction: row; gap: 10px; align-items: center; }
        .flex-col { display: flex; flex-direction: column; gap: 8px; }
        /* Animations */
        @keyframes fadeSlideIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:translateY(0); } }
        @keyframes pulse-glow { 0%,100% { box-shadow: 0 0 8px rgba(56,189,248,0.2); } 50% { box-shadow: 0 0 20px rgba(56,189,248,0.5); } }
        .animate-in { animation: fadeSlideIn 0.3s ease forwards; }
        /* ===== END MOAA DESIGN SYSTEM ===== */
    """.trimIndent()

    fun buildSystemPrompt(): String {
        return """
            You are the Master Web Tool Engineer for "MotherOfAllApps" (MOAA), an elite Android utility suite with a strict Cyberpunk-Dark-Glassmorphism visual identity.
            Your task is to generate complete, single-page, responsive, interactive web tools using HTML, CSS, and pure JavaScript.

            ### OUTPUT FORMAT (CRITICAL):
            You MUST reply ONLY with a raw, valid JSON object. No markdown fences, no commentary, no text before or after the JSON.
            Schema:
            {
              "tool_id": "snake_case_id_no_spaces",
              "display_name": "Human Readable Tool Name",
              "description": "1-2 sentence description.",
              "icon_name": "calculator | convert | code | timer | terminal | chart | tool | lock | wifi | globe | hash | zap",
              "accent_color_hex": 4280391672,
              "html": "...inner HTML only (no <html>/<head>/<body> tags)...",
              "css": "...complete CSS including the MANDATORY MOAA DESIGN SYSTEM base below plus tool-specific styles...",
              "js": "// JavaScript logic\nfunction init() { ... }\nwindow.onload = init;"
            }

            ### ⚠️ MANDATORY GLASSMORPHISM DESIGN SYSTEM (MUST INCLUDE VERBATIM IN CSS):
            Every tool MUST start its CSS with the following design system tokens and component styles.
            DO NOT skip or modify these core styles. Add tool-specific styles AFTER this block.

            ```css
$GLASSMORPHISM_CSS_FOUNDATION
            ```

            ### TOOL-SPECIFIC CSS RULES (apply on top of the design system):
            - Use `.glass-card` for all card containers.
            - Use `.btn`, `.btn-accent`, `.btn-violet`, `.btn-danger` for all interactive buttons.
            - Use `.input` for all form inputs, selects, and textareas.
            - Use `.result-box` for output/result display areas.
            - Use `.label`, `.title`, `.subtitle` for typography.
            - Use `.grid-2`, `.grid-3`, `.flex-row`, `.flex-col` for layouts.
            - Use `.badge-cyan`, `.badge-violet` for status badges.
            - All card headers must use: `<div class="flex-row" style="margin-bottom:12px"><span class="label">SECTION NAME</span><span class="badge badge-cyan">STATUS</span></div>`

            ### MOBILE UX RULES:
            - Touch targets: minimum 44px height. Use `.btn` class which enforces this.
            - Viewport: `<div class="app-container">` is the root. Everything inside it.
            - No horizontal scroll. No external CDN/scripts.
            - Add haptic feedback: `if(window.AndroidBridge?.vibrate) window.AndroidBridge.vibrate(20);` on button taps.
            - Add toast feedback: `if(window.AndroidBridge?.showToast) window.AndroidBridge.showToast("Done!");` for key actions.
            - Add clipboard: `if(window.AndroidBridge?.copyToClipboard) window.AndroidBridge.copyToClipboard(text);` for copy buttons.

            ### CONTENT RULES:
            - Generate ALL logic fully implemented — no placeholder functions, no TODO comments.
            - Include edge case handling (empty input, division by zero, invalid format, etc.).
            - Add at least one `.animate-in` animation on load for visual polish.
            - Use CSS `pulse-glow` animation on primary result elements.
        """.trimIndent()
    }

    fun buildUserPrompt(userRequest: String): String {
        return """
            Generate a complete MOAA dynamic web tool for:
            "$userRequest"

            CRITICAL REMINDERS:
            1. CSS must begin with the full MOAA Glassmorphism Design System base (including all :root variables, .glass-card, .btn, .input, .result-box, etc.).
            2. HTML must use `.app-container` as root and `.glass-card` for all panels.
            3. All buttons must use `.btn` class with the correct variant (`.btn-accent`, `.btn-violet`, `.btn-danger`).
            4. All logic must be fully implemented with no placeholders.
            5. Output ONLY the raw JSON object — no markdown, no explanation text.
        """.trimIndent()
    }

    fun buildRefinePrompt(existingBundle: DynamicToolBundle, refinementRequest: String): String {
        return """
            Refine and update the existing MOAA dynamic web tool based on this request:
            "$refinementRequest"

            ### Existing Tool Manifest:
            ${existingBundle.manifest.toJson().toString(2)}

            ### Existing HTML:
            ```html
            ${existingBundle.html}
            ```

            ### Existing CSS:
            ```css
            ${existingBundle.css}
            ```

            ### Existing JS:
            ```javascript
            ${existingBundle.js}
            ```

            ### IMPORTANT:
            - Preserve the MOAA Glassmorphism Design System CSS tokens if already present.
            - If the existing CSS lacks the design system base, ADD IT before the existing styles.
            - Return the COMPLETE updated tool in the required JSON schema.
            - Output ONLY the raw JSON object — no markdown, no explanation.
        """.trimIndent()
    }
}
