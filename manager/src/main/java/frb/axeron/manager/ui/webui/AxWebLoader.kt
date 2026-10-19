package frb.axeron.manager.ui.webui

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.WorkerThread

class AxWebLoader(val routes: Set<Route>) {

    fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        for (route in routes) {
            if (route.matches(request)) {
                return route.handler.handle(view.rootView.context, view, request)
            }
        }
        return null
    }

    fun interface PathHandler {
        @WorkerThread
        fun handle(context: Context, view: WebView, request: WebResourceRequest): WebResourceResponse?
    }

    class Builder {
        internal val routes = HashSet<Route>()

        fun addScheme(scheme: String): TempRouteGroup = TempRouteGroup(this, arrayOf(scheme))

        fun addScheme(vararg scheme: String): TempRouteGroup = TempRouteGroup(this, arrayOf(*scheme))
    }

    class TempRouteGroup internal constructor(private val parent: Builder, scheme: Array<String>) {
        internal val pendingSchemes = HashSet<String>()
        internal val groupRoutes = HashSet<Route>()
        internal val pendingDomains = HashSet<String>()

        init {
            pendingSchemes.addAll(scheme)
        }

        fun addScheme(scheme: String): TempRouteGroup {
            pendingSchemes.add(scheme)
            return this
        }

        fun addScheme(vararg scheme: String): TempRouteGroup {
            pendingSchemes.addAll(scheme)
            return this
        }

        fun addDomain(domain: String): TempRouteGroup {
            pendingDomains.add(domain)
            return this
        }

        fun addDomain(vararg domain: String): TempRouteGroup {
            pendingDomains.addAll(domain)
            return this
        }

        fun addHandler(handler: PathHandler): TempHandlerGroup = addPathHandler(null, handler)

        fun addPathHandler(path: String?, handler: PathHandler): TempHandlerGroup =
            TempHandlerGroup(this, path, handler)

        fun build(): AxWebLoader {
            parent.routes.addAll(groupRoutes)
            pendingSchemes.clear()
            return AxWebLoader(parent.routes)
        }
    }

    class TempHandlerGroup internal constructor(
        private val routeGroup: TempRouteGroup,
        path: String?,
        handler: PathHandler
    ) {
        init {
            for (scheme in routeGroup.pendingSchemes) {
                for (domain in routeGroup.pendingDomains) {
                    routeGroup.groupRoutes.add(Route(scheme, domain, path, handler))
                }
            }
            routeGroup.pendingDomains.clear()
        }

        fun done(): TempRouteGroup = routeGroup
    }

    data class Route(val scheme: String, val domain: String, val pathPrefix: String?, val handler: PathHandler) {
        init {
            Log.d("AxWebLoader", "Route added: " + scheme + "://" + domain + (pathPrefix ?: ""))
        }

        fun matches(request: WebResourceRequest): Boolean {
            val url = request.url ?: return false
            if (url.scheme == null || url.host == null) return false

            if (url.scheme != scheme) return false
            if (url.host != domain) return false

            val path = url.path
            if (pathPrefix.isNullOrEmpty()) {
                return true // kalau kosong, anggap semua path valid
            }
            return path != null && path.startsWith(pathPrefix)
        }
    }
}
