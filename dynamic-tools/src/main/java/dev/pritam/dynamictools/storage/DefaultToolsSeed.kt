package dev.pritam.dynamictools.storage

import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolManifest

object DefaultToolsSeed {

    fun getSeedTools(): List<DynamicToolBundle> {
        return listOf(
            createCalculatorSeed(),
            createRegexLabSeed()
        )
    }

    private fun createCalculatorSeed(): DynamicToolBundle {
        val manifest = DynamicToolManifest(
            toolId = "scientific_calculator",
            displayName = "Cyber Calculator",
            description = "Sleek scientific calculator with trigonometric functions, history tape, and haptics.",
            iconName = "calculator",
            version = "1.0.0",
            accentColorHex = 0xFF38BDF8,
            author = "MOAA Dynamic Core",
            tags = listOf("Calculator", "Math", "Scientific")
        )

        val html = """
            <div class="calculator-app">
              <div class="display-container">
                <div class="history-tape" id="historyTape">Ready</div>
                <div class="main-display" id="mainDisplay">0</div>
              </div>
              <div class="mode-bar">
                <button class="mode-btn active" id="degRadBtn" onclick="toggleRadDeg()">DEG</button>
                <button class="mode-btn" onclick="copyResult()">📋 COPY</button>
                <button class="mode-btn" onclick="clearHistoryTape()">CLEAR TAPE</button>
              </div>
              <div class="keypad">
                <!-- Scientific Row 1 -->
                <button class="key sci" onclick="inputFunc('sin')">sin</button>
                <button class="key sci" onclick="inputFunc('cos')">cos</button>
                <button class="key sci" onclick="inputFunc('tan')">tan</button>
                <button class="key sci" onclick="inputFunc('log')">log</button>
                <button class="key sci" onclick="inputFunc('ln')">ln</button>

                <!-- Scientific Row 2 -->
                <button class="key sci" onclick="inputFunc('sqrt')">√</button>
                <button class="key sci" onclick="inputOp('^')">xʸ</button>
                <button class="key sci" onclick="inputConst('pi')">π</button>
                <button class="key sci" onclick="inputConst('e')">e</button>
                <button class="key sci" onclick="inputFact()">n!</button>

                <!-- Standard Row 1 -->
                <button class="key action" onclick="clearAll()">AC</button>
                <button class="key action" onclick="deleteChar()">⌫</button>
                <button class="key op" onclick="inputOp('%')">%</button>
                <button class="key op" onclick="inputOp('/')">÷</button>
                <button class="key sci" onclick="inputOp('(')">(</button>

                <!-- Standard Row 2 -->
                <button class="key num" onclick="inputNum('7')">7</button>
                <button class="key num" onclick="inputNum('8')">8</button>
                <button class="key num" onclick="inputNum('9')">9</button>
                <button class="key op" onclick="inputOp('*')">×</button>
                <button class="key sci" onclick="inputOp(')')">)</button>

                <!-- Standard Row 3 -->
                <button class="key num" onclick="inputNum('4')">4</button>
                <button class="key num" onclick="inputNum('5')">5</button>
                <button class="key num" onclick="inputNum('6')">6</button>
                <button class="key op" onclick="inputOp('-')">−</button>
                <button class="key sci" onclick="inputOp('1/x')">1/x</button>

                <!-- Standard Row 4 -->
                <button class="key num" onclick="inputNum('1')">1</button>
                <button class="key num" onclick="inputNum('2')">2</button>
                <button class="key num" onclick="inputNum('3')">3</button>
                <button class="key op" onclick="inputOp('+')">+</button>
                <button class="key num" onclick="toggleSign()">±</button>

                <!-- Standard Row 5 -->
                <button class="key num zero" onclick="inputNum('0')">0</button>
                <button class="key num" onclick="inputDot()">.</button>
                <button class="key equals" onclick="calculate()">=</button>
              </div>
            </div>
        """.trimIndent()

        val css = """
            :root {
              --bg: #090A0E;
              --card: #121319;
              --card-elevated: #181A22;
              --cyan: #38BDF8;
              --violet: #A855F7;
              --rose: #F43F5E;
              --emerald: #10B981;
              --text: #F4F4F6;
              --text-sec: #9CA3AF;
              --glass-border: #222531;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-tap-highlight-color: transparent; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
              background-color: var(--bg);
              color: var(--text);
              display: flex;
              flex-direction: column;
              height: 100vh;
              overflow: hidden;
              padding: 12px;
            }
            .calculator-app {
              display: flex;
              flex-direction: column;
              height: 100%;
              max-width: 480px;
              margin: 0 auto;
              width: 100%;
              gap: 8px;
            }
            .display-container {
              background: rgba(30, 41, 59, 0.7);
              backdrop-filter: blur(12px);
              border: 1px solid var(--glass-border);
              border-radius: 16px;
              padding: 16px;
              display: flex;
              flex-direction: column;
              align-items: flex-end;
              justify-content: flex-end;
              min-height: 110px;
              box-shadow: 0 4px 20px rgba(0,0,0,0.4);
            }
            .history-tape {
              color: var(--text-sec);
              font-size: 13px;
              font-family: monospace;
              word-break: break-all;
              text-align: right;
              min-height: 18px;
            }
            .main-display {
              color: var(--cyan);
              font-size: 36px;
              font-weight: 700;
              font-family: monospace;
              letter-spacing: -0.5px;
              word-break: break-all;
              text-align: right;
              line-height: 1.2;
              margin-top: 4px;
            }
            .mode-bar {
              display: flex;
              gap: 8px;
            }
            .mode-btn {
              background: rgba(30, 41, 59, 0.6);
              border: 1px solid var(--glass-border);
              color: var(--text-sec);
              font-size: 11px;
              font-weight: 600;
              padding: 6px 12px;
              border-radius: 8px;
              cursor: pointer;
            }
            .mode-btn.active {
              color: var(--cyan);
              border-color: var(--cyan);
              background: rgba(56, 189, 248, 0.15);
            }
            .keypad {
              display: grid;
              grid-template-columns: repeat(5, 1fr);
              gap: 6px;
              flex: 1;
            }
            .key {
              border: 1px solid var(--glass-border);
              border-radius: 12px;
              font-size: 16px;
              font-weight: 600;
              color: var(--text);
              background: rgba(30, 41, 59, 0.5);
              cursor: pointer;
              display: flex;
              align-items: center;
              justify-content: center;
              transition: all 0.1s ease;
            }
            .key:active {
              transform: scale(0.94);
              filter: brightness(1.3);
            }
            .key.sci {
              font-size: 12px;
              color: var(--violet);
              background: rgba(167, 139, 250, 0.1);
              border-color: rgba(167, 139, 250, 0.25);
            }
            .key.num {
              font-size: 18px;
              background: rgba(51, 65, 85, 0.5);
            }
            .key.zero {
              grid-column: span 2;
            }
            .key.op {
              color: var(--cyan);
              background: rgba(56, 189, 248, 0.15);
              border-color: rgba(56, 189, 248, 0.3);
            }
            .key.action {
              color: var(--rose);
              background: rgba(244, 114, 182, 0.15);
              border-color: rgba(244, 114, 182, 0.3);
            }
            .key.equals {
              grid-column: span 2;
              background: linear-gradient(135deg, var(--cyan), var(--violet));
              color: #000;
              font-weight: 800;
              font-size: 20px;
              border: none;
            }
        """.trimIndent()

        val js = """
            let currentInput = '0';
            let isRad = false;
            let lastCalculation = '';

            function updateDisplay() {
              document.getElementById('mainDisplay').innerText = currentInput;
              document.getElementById('historyTape').innerText = lastCalculation || 'Ready';
            }

            function haptic() {
              if (window.AndroidBridge && window.AndroidBridge.vibrate) {
                window.AndroidBridge.vibrate(20);
              }
            }

            function inputNum(n) {
              haptic();
              if (currentInput === '0' || currentInput === 'Error') {
                currentInput = n;
              } else {
                currentInput += n;
              }
              updateDisplay();
            }

            function inputDot() {
              haptic();
              if (!currentInput.includes('.')) {
                currentInput += '.';
                updateDisplay();
              }
            }

            function inputOp(op) {
              haptic();
              currentInput += ' ' + op + ' ';
              updateDisplay();
            }

            function inputConst(c) {
              haptic();
              const val = (c === 'pi') ? Math.PI.toString() : Math.E.toString();
              if (currentInput === '0') currentInput = val;
              else currentInput += val;
              updateDisplay();
            }

            function inputFunc(f) {
              haptic();
              try {
                let x = parseFloat(currentInput);
                let res = 0;
                if (f === 'sin') res = isRad ? Math.sin(x) : Math.sin(x * Math.PI / 180);
                else if (f === 'cos') res = isRad ? Math.cos(x) : Math.cos(x * Math.PI / 180);
                else if (f === 'tan') res = isRad ? Math.tan(x) : Math.tan(x * Math.PI / 180);
                else if (f === 'log') res = Math.log10(x);
                else if (f === 'ln') res = Math.log(x);
                else if (f === 'sqrt') res = Math.sqrt(x);

                lastCalculation = f + '(' + currentInput + ')';
                currentInput = Number(res.toFixed(8)).toString();
                updateDisplay();
              } catch(e) {
                currentInput = 'Error';
                updateDisplay();
              }
            }

            function inputFact() {
              haptic();
              try {
                let n = parseInt(currentInput);
                if (n < 0 || n > 170) throw new Error('Range');
                let r = 1;
                for (let i = 2; i <= n; i++) r *= i;
                lastCalculation = n + '!';
                currentInput = r.toString();
                updateDisplay();
              } catch(e) {
                currentInput = 'Error';
                updateDisplay();
              }
            }

            function toggleSign() {
              haptic();
              if (currentInput.startsWith('-')) currentInput = currentInput.substring(1);
              else if (currentInput !== '0') currentInput = '-' + currentInput;
              updateDisplay();
            }

            function deleteChar() {
              haptic();
              if (currentInput.length > 1) {
                currentInput = currentInput.trimEnd();
                currentInput = currentInput.substring(0, currentInput.length - 1).trimEnd();
                if (!currentInput) currentInput = '0';
              } else {
                currentInput = '0';
              }
              updateDisplay();
            }

            function clearAll() {
              haptic();
              currentInput = '0';
              updateDisplay();
            }

            function clearHistoryTape() {
              lastCalculation = '';
              updateDisplay();
            }

            function toggleRadDeg() {
              haptic();
              isRad = !isRad;
              document.getElementById('degRadBtn').innerText = isRad ? 'RAD' : 'DEG';
              document.getElementById('degRadBtn').classList.toggle('active', isRad);
            }

            function calculate() {
              haptic();
              try {
                let expr = currentInput.replace(/×/g, '*').replace(/÷/g, '/').replace(/−/g, '-').replace(/\^/g, '**');
                lastCalculation = currentInput + ' =';
                let result = Function('"use strict";return (' + expr + ')')();
                if (typeof result === 'number') {
                  currentInput = Number(result.toFixed(8)).toString();
                } else {
                  currentInput = 'Error';
                }
              } catch (err) {
                currentInput = 'Error';
              }
              updateDisplay();
            }

            function copyResult() {
              haptic();
              if (window.AndroidBridge && window.AndroidBridge.copyToClipboard) {
                window.AndroidBridge.copyToClipboard(currentInput);
                if (window.AndroidBridge.showToast) {
                  window.AndroidBridge.showToast('Copied ' + currentInput + ' to clipboard');
                }
              }
            }
        """.trimIndent()

        return DynamicToolBundle(manifest, html, css, js)
    }

    private fun createRegexLabSeed(): DynamicToolBundle {
        val manifest = DynamicToolManifest(
            toolId = "regex_text_lab",
            displayName = "Regex & Text Studio",
            description = "Interactive Regular Expression tester, capture group inspector, and text transformations.",
            iconName = "code",
            version = "1.0.0",
            accentColorHex = 0xFFA78BFA,
            author = "MOAA Dynamic Core",
            tags = listOf("Regex", "Developer", "Text")
        )

        val html = """
            <div class="regex-app">
              <header class="header">
                <h2>⚡ REGEX & TEXT STUDIO</h2>
                <div class="badges">
                  <span class="badge" id="matchBadge">0 Matches</span>
                </div>
              </header>

              <div class="section">
                <label>REGULAR EXPRESSION</label>
                <div class="pattern-input-row">
                  <span class="slash">/</span>
                  <input type="text" id="regexPattern" placeholder="e.g. ([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})" value="([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\.[A-Za-z]{2,})" oninput="testRegex()">
                  <span class="slash">/</span>
                  <input type="text" id="regexFlags" class="flags" value="g" oninput="testRegex()" title="Flags: g, i, m, s">
                </div>
              </div>

              <div class="presets-row">
                <button class="chip" onclick="loadPreset('email')">Email</button>
                <button class="chip" onclick="loadPreset('url')">URL</button>
                <button class="chip" onclick="loadPreset('ip')">IPv4</button>
                <button class="chip" onclick="loadPreset('phone')">Phone</button>
                <button class="chip" onclick="loadPreset('hex')">Hex Color</button>
              </div>

              <div class="section flex-1">
                <div class="label-row">
                  <label>TEST STRING</label>
                  <button class="link-btn" onclick="copyMatches()">Copy Matches</button>
                </div>
                <textarea id="testText" placeholder="Paste or type test string here..." oninput="testRegex()">Contact our dev team at admin@motherofallapps.dev or support@company.org for assistance. Visit https://motherofallapps.dev today.</textarea>
              </div>

              <div class="section flex-1">
                <label>MATCH HIGHLIGHTS & GROUPS</label>
                <div class="result-box" id="resultHighlights">No matches yet</div>
              </div>
            </div>
        """.trimIndent()

        val css = """
            :root {
              --bg: #090A0E;
              --card: #121319;
              --violet: #A855F7;
              --cyan: #38BDF8;
              --emerald: #10B981;
              --rose: #F43F5E;
              --text: #F4F4F6;
              --text-sec: #9CA3AF;
              --border: #222531;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              background-color: var(--bg);
              color: var(--text);
              padding: 12px;
              height: 100vh;
              overflow-y: auto;
            }
            .regex-app {
              display: flex;
              flex-direction: column;
              height: 100%;
              max-width: 600px;
              margin: 0 auto;
              gap: 12px;
            }
            .header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              border-bottom: 1px solid var(--border);
              padding-bottom: 8px;
            }
            .header h2 { font-size: 15px; color: var(--violet); font-weight: 700; letter-spacing: 0.5px; }
            .badge {
              background: rgba(167, 139, 250, 0.2);
              color: var(--violet);
              border: 1px solid var(--violet);
              padding: 3px 8px;
              border-radius: 6px;
              font-size: 11px;
              font-weight: 600;
            }
            .section { display: flex; flex-direction: column; gap: 6px; }
            .section.flex-1 { flex: 1; min-height: 100px; }
            label { font-size: 10px; font-weight: 700; color: var(--text-sec); letter-spacing: 0.5px; }
            .label-row { display: flex; justify-content: space-between; align-items: center; }
            .link-btn { background: none; border: none; color: var(--cyan); font-size: 11px; cursor: pointer; font-weight: 600; }
            .pattern-input-row {
              display: flex;
              align-items: center;
              background: rgba(30, 41, 59, 0.7);
              border: 1px solid var(--border);
              border-radius: 10px;
              padding: 4px 10px;
            }
            .slash { color: var(--violet); font-weight: 700; font-size: 16px; font-family: monospace; }
            #regexPattern {
              flex: 1;
              background: transparent;
              border: none;
              color: var(--text);
              font-family: monospace;
              font-size: 13px;
              padding: 6px;
              outline: none;
            }
            .flags {
              width: 38px;
              background: transparent;
              border: none;
              color: var(--emerald);
              font-family: monospace;
              font-weight: bold;
              font-size: 13px;
              outline: none;
            }
            .presets-row { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 2px; }
            .chip {
              background: rgba(30, 41, 59, 0.8);
              border: 1px solid var(--border);
              color: var(--text-sec);
              padding: 4px 10px;
              border-radius: 8px;
              font-size: 11px;
              cursor: pointer;
              white-space: nowrap;
            }
            .chip:active { border-color: var(--violet); color: var(--violet); }
            textarea {
              width: 100%;
              height: 100%;
              min-height: 80px;
              background: rgba(30, 41, 59, 0.5);
              border: 1px solid var(--border);
              border-radius: 10px;
              color: var(--text);
              font-family: monospace;
              font-size: 12px;
              padding: 10px;
              outline: none;
              resize: none;
            }
            textarea:focus { border-color: var(--violet); }
            .result-box {
              background: rgba(15, 23, 42, 0.8);
              border: 1px solid var(--border);
              border-radius: 10px;
              padding: 10px;
              font-family: monospace;
              font-size: 12px;
              overflow-y: auto;
              height: 100%;
              min-height: 90px;
              white-space: pre-wrap;
              line-height: 1.5;
            }
            .match-hl {
              background: rgba(167, 139, 250, 0.35);
              border-bottom: 2px solid var(--violet);
              color: #FFF;
              border-radius: 3px;
              padding: 1px 3px;
            }
            .group-tag {
              display: inline-block;
              font-size: 10px;
              background: rgba(56, 189, 248, 0.2);
              color: var(--cyan);
              border: 1px solid var(--cyan);
              border-radius: 4px;
              padding: 1px 4px;
              margin: 2px;
            }
        """.trimIndent()

        val js = """
            let capturedMatches = [];

            const PRESETS = {
              email: { pattern: '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}', flags: 'g' },
              url: { pattern: 'https?:\\/\\/[\\w\\-\\.]+(?:\\:[0-9]+)?(?:\\/[\\w\\-\\.\\/\\?%&=]*)?', flags: 'gi' },
              ip: { pattern: '\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b', flags: 'g' },
              phone: { pattern: '\\+?\\d{1,4}?[-.\\s]?\\(?\\d{1,3}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}', flags: 'g' },
              hex: { pattern: '#(?:[0-9a-fA-F]{3}){1,2}\\b', flags: 'g' }
            };

            function loadPreset(key) {
              if (PRESETS[key]) {
                document.getElementById('regexPattern').value = PRESETS[key].pattern;
                document.getElementById('regexFlags').value = PRESETS[key].flags;
                testRegex();
              }
            }

            function testRegex() {
              const patternStr = document.getElementById('regexPattern').value;
              const flagsStr = document.getElementById('regexFlags').value;
              const text = document.getElementById('testText').value;
              const badge = document.getElementById('matchBadge');
              const resBox = document.getElementById('resultHighlights');

              capturedMatches = [];

              if (!patternStr || !text) {
                badge.innerText = '0 Matches';
                resBox.innerText = 'Enter a pattern and test text.';
                return;
              }

              try {
                const regex = new RegExp(patternStr, flagsStr);
                const matches = [...text.matchAll(regex)];
                badge.innerText = matches.length + ' Match' + (matches.length === 1 ? '' : 'es');

                if (matches.length === 0) {
                  resBox.innerHTML = '<span style="color:#94A3B8">No matches found for this pattern.</span>';
                  return;
                }

                capturedMatches = matches.map(m => m[0]);

                let out = '<div style="margin-bottom:8px; color:#38BDF8; font-weight:bold;">Found ' + matches.length + ' match(es):</div>';
                matches.forEach((m, idx) => {
                  out += '<div style="margin-bottom:6px; padding:6px; background:rgba(30,41,59,0.5); border-radius:6px;">';
                  out += '<span style="color:#A78BFA; font-weight:bold;">#' + (idx + 1) + ':</span> ' + escapeHtml(m[0]);
                  if (m.length > 1) {
                    out += '<div style="margin-top:4px;">';
                    for (let g = 1; g < m.length; g++) {
                      out += '<span class="group-tag">Group ' + g + ': ' + escapeHtml(m[g] || '') + '</span>';
                    }
                    out += '</div>';
                  }
                  out += '</div>';
                });

                resBox.innerHTML = out;
              } catch (err) {
                badge.innerText = 'Syntax Error';
                resBox.innerHTML = '<span style="color:#F472B6">Invalid Regular Expression: ' + err.message + '</span>';
              }
            }

            function escapeHtml(str) {
              return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
            }

            function copyMatches() {
              if (capturedMatches.length === 0) return;
              const textToCopy = capturedMatches.join('\n');
              if (window.AndroidBridge && window.AndroidBridge.copyToClipboard) {
                window.AndroidBridge.copyToClipboard(textToCopy);
                if (window.AndroidBridge.showToast) {
                  window.AndroidBridge.showToast('Copied ' + capturedMatches.length + ' matches to clipboard');
                }
              }
            }

            window.onload = testRegex;
        """.trimIndent()

        return DynamicToolBundle(manifest, html, css, js)
    }
}
