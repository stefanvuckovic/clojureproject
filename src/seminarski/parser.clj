(ns seminarski.parser
  (:require [seminarski.getdatafromurl :as gd]
            [cheshire.core :refer :all :as ch]))

(defn get-url-listmoviesapi [] 
  (str "https://yts.to/api/v2/list_movies.json")
)

(defn get-limit-listmoviesapi [] 
  50
)

(defn get-url-moviedetailsapi [] 
  (str "https://yts.to/api/v2/movie_details.json")
)

(defn get-movies-from-page [page] 
  (let [apidata (ch/parse-string (gd/get-data (get-url-listmoviesapi) {"limit" (get-limit-listmoviesapi), "page" page, "quality" "3D" }) true)]
    (if(= (:status apidata) "ok")
       (:movies (:data apidata))
       []))
        
)






