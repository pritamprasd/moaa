package dev.pritam.host.tool.llmgateway.manager

import android.content.Context
import dev.pritam.host.tool.llmgateway.engine.LlmRouterEngine
import dev.pritam.host.tool.llmgateway.mcp.storage.McpServerRepository
import dev.pritam.host.tool.llmgateway.scanner.DesktopHostScanner
import dev.pritam.host.tool.llmgateway.server.LlmGatewayHttpServer
import dev.pritam.host.tool.llmgateway.storage.LlmProfileRepository

object LlmGatewayManager {

    private var _repository: LlmProfileRepository? = null
    private var _mcpRepository: McpServerRepository? = null
    private var _routerEngine: LlmRouterEngine? = null
    private var _scanner: DesktopHostScanner? = null
    private var _httpServer: LlmGatewayHttpServer? = null

    fun initialize(context: Context) {
        if (_repository == null) {
            val repo = LlmProfileRepository(context.applicationContext)
            val mcpRepo = McpServerRepository(context.applicationContext)
            val router = LlmRouterEngine(repo, mcpRepo)
            val scan = DesktopHostScanner(context.applicationContext)
            val server = LlmGatewayHttpServer(repo, router, mcpRepo, port = 8080)

            _repository = repo
            _mcpRepository = mcpRepo
            _routerEngine = router
            _scanner = scan
            _httpServer = server

            // Start loopback server by default
            server.start()
        }
    }

    fun getRepository(context: Context): LlmProfileRepository {
        initialize(context)
        return _repository!!
    }

    fun getMcpRepository(context: Context): McpServerRepository {
        initialize(context)
        return _mcpRepository!!
    }

    fun getRouterEngine(context: Context): LlmRouterEngine {
        initialize(context)
        return _routerEngine!!
    }

    fun getScanner(context: Context): DesktopHostScanner {
        initialize(context)
        return _scanner!!
    }

    fun getHttpServer(context: Context): LlmGatewayHttpServer {
        initialize(context)
        return _httpServer!!
    }
}
