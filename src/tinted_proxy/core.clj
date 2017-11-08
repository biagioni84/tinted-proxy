(ns tinted-proxy.core
  (:import [java.net URI])
  (:require [org.httpkit.client :as http]
            [clojure.string :as str])
  )

(defn default-conversion-fn [req local-path remote-base-uri]
  (let [remote-base   (URI. (str remote-base-uri "/"))
        remote-path   (URI. (.getScheme    remote-base)
                            (.getAuthority remote-base)
                            (.getPath      remote-base) nil nil)
        local-path   (URI. (subs (:uri req) (.length local-path)))
        remote-uri (.resolve remote-path local-path)]
    (str remote-uri)
    )
  )
;; TODO: add a handler for maps and take local-path remote-uri and headers and inject headers
;; TODO: add an error/exception-handler-fn (returns response on error)

(defn camelize [input-string]
  (let [words (str/split input-string #"[\s_-]+")]
    (str/join "-"  (map str/capitalize words))))

(defn wrap-tinted-proxy
  "Proxies a request by using a fn to convert from local to remote uri, using info extracted from the request.
  In case of exception returns a 403 status"
  [handler base-path uri-build-fn & {:keys [update-req-body-fn update-req-headers-fn
                                            update-resp-body-fn update-resp-headers-fn]}]
  (fn [req]
    (if (.startsWith ^String (:uri req) (str base-path "/"))
      (try
        (let [req-uri (if (string? uri-build-fn)
                        (default-conversion-fn req base-path uri-build-fn)
                        (uri-build-fn req base-path))
              clean-headers  (dissoc (:headers req) "host" "content-length")
              req-headers (if update-req-headers-fn (update-req-headers-fn (assoc req :headers clean-headers)) clean-headers)
              req-body (if update-req-body-fn (update-req-body-fn req) (:body req))
              ]
          ;; TODO: use a cleaner way instead of deconstructing and constructing
          ;(println clean-headers)
          ;(println req-headers)
          (let [{:keys [status headers body error] :as resp} @(http/request {:url req-uri
                                                                             :method (:request-method req)
                                                                             :headers req-headers
                                                                             :body  req-body
                                                                             })]
            (if error
              {:status 405} ;; TODO: make a 403 on prod to avoid leaking info about the endpoint
              (let [resp-body (if update-resp-body-fn  (update-resp-body-fn resp) body)
                    resp-headers (if update-resp-headers-fn  (update-resp-headers-fn resp) headers)
                    ccase (apply conj (map #(into {} {(camelize (name (key %))) (val %)}) (dissoc headers :transfer-encoding)))
                    ]
                (assoc resp :body resp-body :headers ccase)
                )
              ))
          )
        (catch Exception e
          ;{:status 404 ;; TODO: make a 403 on prod to avoid leaking info about the exception
          ; :body e}
          {:status 403}
          ))
      ;; add try/catch and return 403 on exception or maybe go to next handler?
      (handler req)
      )
    )
  )

