# CachingHttpProxy
###### What is a Caching HTTP Proxy Server?
A proxy server acts as an intermediary for requests from clients seeking resources from other servers. A client connects to the proxy server requesting some service from a different server and the proxy server evaluates the request as a way to simplify and control its complexity. A caching proxy server accelerates service requests by retrieving content saved from a previous request made by the same client or even other clients

###### Features
* Handles concurrent connections from multiple clients to multiple origins (downstream servers). Avoid blocking, locking, synchronized code blocks or methods.
* Objects in the cache have default time to live (system wide)
* Relays headers, response code, and other needed HTTP information from the origin server to the clients.
* Works like a REAL HTTP proxy (not web server) with commands like this: http_proxy='http://localhost:3128' wget -S http://letitcrash.com (Such that your proxy is running on localhost port 3128).
* Provides X-CACHE-HIT or X-CACHE-MISS header in the response to the client if the object was a cache hit or a cache miss (first time fetch from the origin).