(ns seminarski.getdatafromurl
  (:require [clj-http.client :as client]))

(defn get-data [url, params]
  (let [formattedparams {:query-params params}]
    (:body (client/get url formattedparams))  
  )
)
