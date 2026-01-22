package frb.axeron.server.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record AxWebLoader(Set<Route> routes) {

    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        for (Route route : routes) {
            if (route.matches(request)) {
                return route.handler.handle(view.getRootView().getContext(), view, request);
            }
        }
        return null;
    }

    public interface PathHandler {
        @WorkerThread
        @Nullable
        WebResourceResponse handle(Context context, WebView view, WebResourceRequest request);
    }

    public static class Builder {
        protected final Set<Route> routes = new HashSet<>();

        public TempRouteGroup addScheme(String scheme) {
            return new TempRouteGroup(this, new String[]{scheme});
        }

        public TempRouteGroup addScheme(String... scheme) {
            return new TempRouteGroup(this, scheme);
        }
    }

    public static class TempRouteGroup {
        protected final Builder parent;
        protected final Set<String> pendingSchemes = new HashSet<>();
        protected final Set<Route> groupRoutes = new HashSet<>();
        protected final Set<String> pendingDomains = new HashSet<>();

        TempRouteGroup(Builder parent, String[] scheme) {
            this.parent = parent;
            Collections.addAll(pendingSchemes, scheme);
        }

        public TempRouteGroup addScheme(String scheme) {
            pendingSchemes.add(scheme);
            return this;
        }

        public TempRouteGroup addScheme(String... scheme) {
            Collections.addAll(pendingSchemes, scheme);
            return this;
        }

        public TempRouteGroup addDomain(String domain) {
            pendingDomains.add(domain);
            return this;
        }

        public TempRouteGroup addDomain(String... domain) {
            Collections.addAll(pendingDomains, domain);
            return this;
        }

        public TempHandlerGroup addHandler(PathHandler handler) {
            return addPathHandler(null, handler) ;
        }

        public TempHandlerGroup addPathHandler(String path, PathHandler handler) {
            return new TempHandlerGroup(this, path, handler);
        }

        public AxWebLoader build() {
            parent.routes.addAll(groupRoutes);
            pendingSchemes.clear();
            return new AxWebLoader(parent.routes);
        }
    }

    public static class TempHandlerGroup {
        private final TempRouteGroup routeGroup;

        TempHandlerGroup(TempRouteGroup routeGroup, String path, PathHandler handler) {
            this.routeGroup = routeGroup;
            for (String scheme : routeGroup.pendingSchemes) {
                for (String domain : routeGroup.pendingDomains) {
                    routeGroup.groupRoutes.add(new Route(scheme, domain, path, handler));
                }
            }
            routeGroup.pendingDomains.clear();
        }

        public TempRouteGroup done() {
            return routeGroup;
        }
    }

    public record Route(String scheme, String domain, String pathPrefix, PathHandler handler) {
            public Route(String scheme, String domain, String pathPrefix, PathHandler handler) {
                this.scheme = scheme;
                this.domain = domain;
                this.pathPrefix = pathPrefix;
                this.handler = handler;
                Log.d("AxWebLoader", "Route added: " + scheme + "://" + domain + (pathPrefix == null ? "" : pathPrefix));
            }

            boolean matches(WebResourceRequest request) {
                Uri url = request.getUrl();
                if (url == null || url.getScheme() == null || url.getHost() == null) return false;

                if (!url.getScheme().equals(scheme)) return false;
                if (!url.getHost().equals(domain)) return false;

                String path = url.getPath();
                if (pathPrefix == null || pathPrefix.isEmpty()) {
                    return true; // kalau kosong, anggap semua path valid
                }
                return path != null && path.startsWith(pathPrefix);
            }
        }
}
