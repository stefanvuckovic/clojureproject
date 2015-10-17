(ns seminarski.data.scraping
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]))

(defn get-review-url [movie-id]
  (str "http://www.imdb.com/title/" movie-id "/reviews"))

(defn remove-stop-words [review]
  (string/replace review #"\*|\"|\.|spoilers|'" " "))
                          
(defn check-if-real-review [review]
  (.startsWith (string/trim review) "***"))


(defn parse-reviews [movie-id] 
  (try
    (remove check-if-real-review 
      (into [] 
            (drop-last 
             (reduce 
              #(conj %1 
                     (string/replace 
                      (html/text %2) #"\n" " ")) 
              [] 
              (html/select 
               (html/html-resource 
                (java.net.URL. 
                 (get-review-url movie-id))) 
               [:div#tn15content :p])))))
     (catch Exception e
       (.printStackTrace e))))


