(ns seminarski.data.getdatafromurl
  (:require [clj-http.client :as client]
            [org.httpkit.client :as http]))

(defn get-data2 [url, params]
  (try
    (let [formattedparams {:query-params params}]
      (:body (client/get url formattedparams)))
    (catch Exception e
      (do
        (Thread/sleep 300)
        (.printStackTrace e)))))

(defn get-data [url, params]
  (try
    (let [formattedparams {:query-params params}
          {:keys [status headers body error]} @(http/get url formattedparams)]
      (if (or error (not= status 200))
        nil
        body))
    (catch Exception e
      (.printStackTrace e))))
