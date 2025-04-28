(ns dvb.server.system.log
  (:require
   [taoensso.telemere :as tel]))

(defn init
  "Initialize logging: set level to INFO and write all logs to `log-file-path`."
  [log-file-path]
  (tel/set-min-level! :log :info)
  (tel/add-handler!
   :file
   (tel/handler:file {:path             log-file-path
                      ;; output-fn          (utils/format-signal-fn)
                      ;; :interval          :monthly
                      ;; :max-file-size     (* 1024 1024 4)
                      ;; :max-num-parts     8
                      ;; :max-num-intervals 6
                      ;; :gzip-archives?    true
                      }))
  nil)
