# tinted-proxy

A non-transparent proxy ring middleware. 
It accept a conversion-fn to generate a request map (see http://www.http-kit.org/client.html#options)
It comes with a default conversion function to act as a transparent proxy (see https://github.com/tailrecursion/ring-proxy for an alternative). 

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
      (wrap-tinted-proxy "/proxy" conversion-fn)
      (wrap-tinted-proxy "/google" "http://www.google.com"))
      )
```

### Example of conversion-fn
```clojure

(defn conversion-fn [req base-path]
    {:url (my-url-conversion-fn (:url req))
     :method (:request-method req)
     :headers (dissoc (:headers req) "host" "content-length")
     :body  (:body req)
     }
  )
```

