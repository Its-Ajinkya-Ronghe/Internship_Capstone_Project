package com.expandtesting.notes.utils;

import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import java.util.HashMap;
import java.util.Map;

public class McpClientManager {
    private static final Logger log = LogManager.getLogger(McpClientManager.class);
    private final WebDriver driver;
    private final Map<String, String> toolRegistry = new HashMap<>();

    public McpClientManager(WebDriver driver) {
        this.driver = driver;
        // 🔒 Register tools conforming to the standard Model Context Protocol schema bindings
        toolRegistry.put("mcp_login_action", "LoginPage#login");
        toolRegistry.put("mcp_create_note_action", "NoteModalPage#createNewNote");
    }

    /**
     * Publishes the formal JSON-like schema definitions for discovery by an AI model or inspector client.
     */
    public void discoverMcpCapabilities() {
        log.info("====================================================================================");
        log.info("🌐 MCP PROTOCOL SCHEMAS DISCOVERED — PUBLISHING REGISTERED CAPABILITIES");
        log.info("Tool: mcp_login_action -> Params: {email: String, password: String}");
        log.info("Tool: mcp_create_note_action -> Params: {category: String, title: String, description: String}");
        log.info("====================================================================================");
    }

    /**
     * Processes an incoming protocol context execution block and routes parameters to page object methods.
     */
    public void processMcpExecution(String toolName, Map<String, String> arguments) {
        log.info("📥 MCP Protocol Signal Inbound: Received command execution block for tool: '{}'", toolName);

        if (!toolRegistry.containsKey(toolName)) {
            log.error("🛑 MCP Execution Exception: Requested tool interaction schema '{}' is not registered.", toolName);
            throw new IllegalArgumentException("Target tool context not supported by MCP profile binding: " + toolName);
        }

        switch (toolName) {
            case "mcp_login_action":
                String email = arguments.get("email");
                String pwd = arguments.get("password");
                log.info("🤖 MCP Routing -> Triggering LoginPage authentication sequence via context client logic.");
                new LoginPage(driver).login(email, pwd);
                break;

            case "mcp_create_note_action":
                String category = arguments.get("category");
                String title = arguments.get("title");
                String desc = arguments.get("description");
                log.info("🤖 MCP Routing -> Triggering NoteModalPage record creation via client mapping contexts.");
                new NoteModalPage(driver).createNewNote(category, title, desc);
                break;

            default:
                throw new IllegalStateException("Unexpected automation routing scenario reached inside the protocol dispatcher.");
        }

        log.info("✅ MCP Action Pipeline Complete: Tool execution context completed completely.");
    }
}