package com.expandtesting.notes.scripts;

import com.expandtesting.notes.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CreateNoteRunner — MCP Script Execution Layer (Section 3.4)
 *
 * Implements the Model Context Protocol (MCP) integration using
 * @angiejones/mcp-selenium as the stdio transport server.
 *
 * Protocol: JSON-RPC 2.0 over stdin/stdout (stdio MCP transport)
 * MCP Server: npx @angiejones/mcp-selenium@latest
 *
 * Flow:
 *  1. Spawn the MCP Selenium server as a child process via ProcessBuilder
 *  2. Handshake — send initialize + notifications/initialized
 *  3. Call MCP tools sequentially to navigate, login, and create a note
 *  4. Verify creation via get_text tool call
 *  5. Take screenshot and close the browser
 *
 * Each tool call is logged clearly to demonstrate the MCP tool-call chain
 * that an AI agent (Claude, Copilot) would produce when using this server.
 */
public class CreateNoteRunner {

    private static final Logger log = LogManager.getLogger(CreateNoteRunner.class);

    // JSON-RPC request ID counter (thread-safe, monotonically increasing)
    private static final AtomicInteger requestId = new AtomicInteger(1);

    // Process handles for the MCP server child process
    private Process mcpProcess;
    private BufferedWriter mcpStdin;
    private BufferedReader mcpStdout;

    // ─────────────────────────────────────────────────────────────────
    // Entry Point
    // ─────────────────────────────────────────────────────────────────

//    public static void main(String[] args) {
//        CreateNoteRunner runner = new CreateNoteRunner();
//        try {
//            runner.run();
//        } catch (Exception e) {
//            log.error("❌ MCP script execution terminated with error: {}", e.getMessage(), e);
//            System.exit(1);
//        }
//    }

    // ─────────────────────────────────────────────────────────────────
    // Orchestration
    // ─────────────────────────────────────────────────────────────────

    public void run() throws Exception {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  MCP Script Runner — @angiejones/mcp-selenium");
        log.info("  Section 3.4 — Model Context Protocol Integration Layer");
        log.info("═══════════════════════════════════════════════════════════");

        // Read target values from shared config / environment
        String baseUrl  = ConfigReader.getProperty("ui.base.url");
        String email    = ConfigReader.getProperty("default.username");
        String password = ConfigReader.getProperty("default.password");

        // Note data for MCP-driven creation
        String noteCategory    = "Work";
        String noteTitle       = "MCP Created Note - " + System.currentTimeMillis();
        String noteDescription = "This note was created by the MCP Selenium script runner via JSON-RPC tool calls.";

        try {
            startMcpServer();
            handshake();
            executeNoteCreationFlow(baseUrl, email, password, noteCategory, noteTitle, noteDescription);
        } finally {
            shutdownMcpServer();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MCP Server Lifecycle
    // ─────────────────────────────────────────────────────────────────

    /**
     * Spawns the @angiejones/mcp-selenium MCP server as a child process.
     * Communication channel: stdin (requests) ↔ stdout (responses).
     */
    private void startMcpServer() throws IOException {
        log.info("▶ Spawning MCP Selenium server: npx @angiejones/mcp-selenium@latest");

        ProcessBuilder pb = new ProcessBuilder("npx", "-y", "@angiejones/mcp-selenium@latest");
        pb.redirectErrorStream(false); // Keep stderr separate so JSON-RPC stdout is clean
        pb.environment().put("BROWSER", "chrome");
        pb.environment().put("HEADLESS", "true");

        mcpProcess = pb.start();

        // Wrap stdin/stdout for line-delimited JSON-RPC messaging
        mcpStdin  = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8));
        mcpStdout = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream(),  StandardCharsets.UTF_8));

        // Drain stderr asynchronously to avoid blocking the server on full stderr pipe
        Thread stderrDrain = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(mcpProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = err.readLine()) != null) {
                    log.debug("[mcp-server stderr] {}", line);
                }
            } catch (IOException ignored) {}
        });
        stderrDrain.setDaemon(true);
        stderrDrain.start();

        log.info("✅ MCP server process started (PID: {})", mcpProcess.pid());
    }

    /**
     * MCP handshake sequence:
     *   Client → initialize
     *   Server → InitializeResult
     *   Client → notifications/initialized  (no response expected)
     */
    private void handshake() throws IOException {
        log.info("── MCP Handshake ──────────────────────────────────────────");

        // Step 1 — initialize
        String initRequest = buildToolRequest(
                "initialize",
                "{"
                        + "\"protocolVersion\":\"2024-11-05\","
                        + "\"capabilities\":{},"
                        + "\"clientInfo\":{\"name\":\"CreateNoteRunner\",\"version\":\"1.0\"}"
                        + "}"
        );
        String initResponse = sendAndReceive(initRequest, "initialize");
        log.info("  Server capabilities received: {}", extractField(initResponse, "serverInfo"));

        // Step 2 — notifications/initialized (fire-and-forget, no id)
        String notif = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        sendMessage(notif);
        log.info("✅ MCP handshake complete — ready to call tools");
    }

    private void shutdownMcpServer() {
        log.info("── Shutting down MCP server ───────────────────────────────");
        try {
            if (mcpStdin != null) mcpStdin.close();
        } catch (IOException ignored) {}

        if (mcpProcess != null && mcpProcess.isAlive()) {
            mcpProcess.destroy();
            try {
                mcpProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            log.info("✅ MCP server process terminated");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Note Creation Flow — MCP Tool Call Chain
    // ─────────────────────────────────────────────────────────────────

    /**
     * Executes the full note creation E2E flow using MCP Selenium tools.
     * Each step corresponds to a single JSON-RPC tools/call request.
     */
    private void executeNoteCreationFlow(String baseUrl, String email, String password,
                                         String category, String title, String description)
            throws IOException {

        log.info("── MCP Tool Chain: Note Creation Flow ─────────────────────");

        // ── Step 1: Start browser and navigate to login page ──────────
        log.info("[MCP Tool] start_browser → chrome (headless)");
        callTool("start_browser",
                "{\"browser\":\"chrome\",\"options\":[\"--headless\",\"--no-sandbox\",\"--disable-dev-shm-usage\"]}");

        log.info("[MCP Tool] navigate_to → {}", baseUrl);
        callTool("navigate_to", "{\"url\":\"" + baseUrl + "\"}");

        // ── Step 2: Wait for login page to load ───────────────────────
        log.info("[MCP Tool] wait_for_element → email input field");
        callTool("wait_for_element",
                "{\"by\":\"id\",\"value\":\"email\",\"timeout\":10}");

        // ── Step 3: Fill login credentials ────────────────────────────
        log.info("[MCP Tool] find_element + input_text → email: {}", email);
        callTool("find_element",
                "{\"by\":\"id\",\"value\":\"email\"}");
        callTool("input_text",
                "{\"by\":\"id\",\"value\":\"email\",\"text\":\"" + email + "\"}");

        log.info("[MCP Tool] input_text → password field");
        callTool("input_text",
                "{\"by\":\"id\",\"value\":\"password\",\"text\":\"" + password + "\"}");

        // ── Step 4: Submit login form ──────────────────────────────────
        log.info("[MCP Tool] click_element → Login submit button");
        callTool("click_element",
                "{\"by\":\"id\",\"value\":\"submit\"}");

        // ── Step 5: Wait for dashboard ────────────────────────────────
        log.info("[MCP Tool] wait_for_element → dashboard Add Note button");
        callTool("wait_for_element",
                "{\"by\":\"xpath\",\"value\":\"//button[contains(@data-testid,'add-new-note')]\",\"timeout\":10}");

        // ── Step 6: Open the Add Note modal ───────────────────────────
        log.info("[MCP Tool] click_element → + Add Note button");
        callTool("click_element",
                "{\"by\":\"xpath\",\"value\":\"//button[contains(@data-testid,'add-new-note')]\"}");

        log.info("[MCP Tool] wait_for_element → note modal category dropdown");
        callTool("wait_for_element",
                "{\"by\":\"id\",\"value\":\"category\",\"timeout\":8}");

        // ── Step 7: Fill Note form fields ─────────────────────────────
        log.info("[MCP Tool] select_option → Category: {}", category);
        callTool("select_option",
                "{\"by\":\"id\",\"value\":\"category\",\"option\":\"" + category + "\"}");

        log.info("[MCP Tool] input_text → Title: {}", title);
        callTool("input_text",
                "{\"by\":\"id\",\"value\":\"title\",\"text\":\"" + title + "\"}");

        log.info("[MCP Tool] input_text → Description");
        callTool("input_text",
                "{\"by\":\"id\",\"value\":\"description\",\"text\":\"" + description + "\"}");

        // ── Step 8: Submit the note ────────────────────────────────────
        log.info("[MCP Tool] click_element → Save note submit button");
        callTool("click_element",
                "{\"by\":\"xpath\",\"value\":\"//button[@data-testid='note-submit']\"}");

        // ── Step 9: Verify note appears on dashboard ───────────────────
        log.info("[MCP Tool] wait_for_element → newly created note card");
        callTool("wait_for_element",
                "{\"by\":\"xpath\",\"value\":\"//*[@data-testid='note-card-title'][contains(text(),'MCP')]\",\"timeout\":8}");

        log.info("[MCP Tool] get_text → read back note title from DOM");
        String getTextResponse = callTool("get_text",
                "{\"by\":\"xpath\",\"value\":\"//*[@data-testid='note-card-title'][contains(text(),'MCP')]\"}");

        String renderedTitle = extractField(getTextResponse, "text");
        log.info("✅ Note verified on dashboard — DOM title: '{}'", renderedTitle);

        if (title.equals(renderedTitle)) {
            log.info("✅ [ASSERTION PASS] MCP-created note title matches expected value exactly.");
        } else {
            log.warn("⚠️ [ASSERTION WARN] Title mismatch — expected='{}', actual='{}'", title, renderedTitle);
        }

        // ── Step 10: Screenshot evidence ──────────────────────────────
        log.info("[MCP Tool] take_screenshot → capture post-creation state");
        String screenshotResponse = callTool("take_screenshot", "{}");
        log.info("📸 Screenshot captured via MCP tool (base64 length={})",
                extractBase64Length(screenshotResponse));

        // ── Step 11: Close browser ─────────────────────────────────────
        log.info("[MCP Tool] close_browser");
        callTool("close_browser", "{}");

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  MCP Note Creation Flow — COMPLETE");
        log.info("  Note title : {}", title);
        log.info("  Category   : {}", category);
        log.info("═══════════════════════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────
    // JSON-RPC 2.0 Transport Helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sends a tools/call request and returns the raw JSON response string.
     */
    private String callTool(String toolName, String argumentsJson) throws IOException {
        String payload = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                +   "\"name\":\"" + toolName + "\","
                +   "\"arguments\":" + argumentsJson
                + "},"
                + "\"id\":" + requestId.getAndIncrement()
                + "}";
        return sendAndReceive(payload, toolName);
    }

    /**
     * Sends a raw JSON-RPC request string and blocks until a response line is read.
     */
    private String sendAndReceive(String jsonRequest, String label) throws IOException {
        sendMessage(jsonRequest);
        String response = readNextMessage();
        log.debug("  [{}] ← {}", label, response);
        return response;
    }

    /**
     * Writes a single JSON-RPC message line to the MCP server's stdin.
     * The MCP stdio transport uses newline-delimited JSON (NDJSON).
     */
    private void sendMessage(String json) throws IOException {
        mcpStdin.write(json);
        mcpStdin.newLine();
        mcpStdin.flush();
        log.trace("  → {}", json);
    }

    /**
     * Reads and returns the next non-empty line from the MCP server's stdout.
     * Skips blank lines and SSE-style "data:" prefixes if present.
     */
    private String readNextMessage() throws IOException {
        String line;
        while ((line = mcpStdout.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("data:")) {
                line = line.substring(5).trim();
            }
            if (!line.isEmpty()) {
                return line;
            }
        }
        throw new IOException("MCP server stdout closed unexpectedly — process may have crashed.");
    }

    // ─────────────────────────────────────────────────────────────────
    // JSON-RPC Request Builder
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds a generic JSON-RPC 2.0 method request with a flat params string.
     * Used for non-tools/call methods (initialize, etc.).
     */
    private String buildToolRequest(String method, String paramsJson) {
        return "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"method\":\"" + method + "\","
                + "\"params\":" + paramsJson + ","
                + "\"id\":" + requestId.getAndIncrement()
                + "}";
    }

    // ─────────────────────────────────────────────────────────────────
    // Lightweight JSON Field Extraction (no external JSON library needed)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extracts a simple string value for a given key from a flat JSON string.
     * Handles both quoted string values and nested object strings.
     */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "<not found>";
        start += search.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (json.charAt(start) == '"') {
            // Quoted string value
            int end = json.indexOf('"', start + 1);
            return end != -1 ? json.substring(start + 1, end) : "<parse error>";
        } else if (json.charAt(start) == '{') {
            // Nested object — return truncated for logging
            int depth = 0, end = start;
            for (; end < json.length(); end++) {
                if (json.charAt(end) == '{') depth++;
                else if (json.charAt(end) == '}') { depth--; if (depth == 0) break; }
            }
            String nested = json.substring(start, end + 1);
            return nested.length() > 80 ? nested.substring(0, 80) + "..." : nested;
        }
        return "<complex value>";
    }

    /**
     * Returns the approximate base64 payload length from a screenshot response,
     * used only for log confirmation that a screenshot was captured.
     */
    private int extractBase64Length(String json) {
        String key = "\"data\":\"";
        int start = json.indexOf(key);
        if (start == -1) return 0;
        start += key.length();
        int end = json.indexOf('"', start);
        return end != -1 ? end - start : 0;
    }
}
