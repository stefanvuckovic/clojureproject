(ns seminarski.getdatafromurl
  (:require [clj-http.client :as client]))

(defn get-data [url, params]
  (try
    (let [formattedparams {:query-params params}]
      (:body (client/get url formattedparams))  
    )
    (catch Exception e
      (.printStackTrace e)))
)
