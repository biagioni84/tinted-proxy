# tinted-proxy

A non-transparent proxy for ring. Uses [http-kit client](http://www.http-kit.org/client.html) to make requests. 
It can act as a transparent proxy (see https://github.com/tailrecursion/ring-proxy for an alternative). 

### Dependency

```clojure
[tinted-proxy "0.1.0-SNAPSHOT"]
```
### Usage

```clojure
(ns your-ns
  (:require [tinted-proxy.core :refer [wrap-tinted-proxy]]))
(def app
  (-> routes
      (wrap-tinted-proxy  "/postman" "http://www.postman-echo.com"
                    :update-req-body-fn (fn [req] (str (:body req) "append to body" ) )
                    :update-req-headers-fn #(:headers (assoc-in % [:headers "token"] "t"))
                    :update-resp-body-fn #(str (:body %) "append to response")
                    :update-resp-headers-fn #(dissoc (:headers %) :content-type)
                          )
      (wrap-tinted-proxy "/google" "http://www.google.com") ;; transparent proxy
      )
      )
```

### Signatures

```clojure
(defn wrap-tinted-proxy
  [handler base-path uri-build-fn & {:keys [update-req-body-fn update-req-headers-fn
                                            update-resp-body-fn update-resp-headers-fn]}]
                                            )
```
```
base-path string
uri-build-fn remote-url(string) | req->url(string)
 
optionals
---------
:update-req-body-fn     request->body(string)
:update-req-headers-fn  request->headers(map)
:update-resp-body-fn    response->body(string)
:update-resp-headers-fn response->headers(map)
```

