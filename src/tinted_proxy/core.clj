(ns tinted-proxy.core
  (:import [java.net URI])
  (:require [org.httpkit.client :as http])
  )

(defn default-convertion-fn [req local-path remote-base-uri]
  (let [remote-base   (URI. (str remote-base-uri "/"))
        remote-path   (URI. (.getScheme    remote-base)
                            (.getAuthority remote-base)
                            (.getPath      remote-base) nil nil)
        local-path   (URI. (subs (:uri req) (.length local-path)))
        remote-uri (.resolve remote-path local-path)]
    {:url (str remote-uri)
     :method (:request-method req)
     :headers (dissoc (:headers req) "host" "content-length")
     :body  (:body req)
     }
    )
  )
;; TODO: add a handler for maps and take local-path remote-uri and headers and inject headers
(defn wrap-tinted-proxy
  "Proxies a request by using a fn to convert from local to remote uri, using info extracted from the request.
  In case of exception returns a 403 status"
  [handler base-path uri-converter & [http-opts]]
  (fn [req]
    (if (.startsWith ^String (:uri req) (str base-path "/"))
      (try
        (let [request (if (string? uri-converter)
                        (default-convertion-fn req base-path uri-converter)
                        (uri-converter req base-path))
              ]
          ;; TODO: use a cleaner way instead of deconstructing and constructing
          (println request)
          (let [{:keys [status headers body error] :as resp} @(http/request (merge request http-opts))]
            (if error
              {:status 405} ;; TODO: make a 403 on prod to avoid leaking info about the endpoint
              (do
                ;(println resp)
                {:status status
                 ;; we have to put headers manually or it fails!
                 :headers{
                          "Content-Type" "application/json"
                          }
                 :body body
                 }
                )
              ))
          )
        (catch Exception e
          {:status 404}
          ))
      ;; add try/catch and return 403 on exception or maybe go to next handler?
      (handler req)
      )
    )
  )
