(ns seminarski.config.config
  (:require [cheshire.core :refer :all :as ch]
            [seminarski.db.db :as db]
            [seminarski.data.parser :as parser]
            [seminarski.recommendation.calculations :as calc]
            [seminarski.config.settings :as settings]
            [seminarski.data.scraping :as scraper]))

(defn get-movies-backup-file []
  "conf/data.edn")

(defn get-similarities-backup-file []
  "conf/similarity.edn")

(defn get-config-file []
  "conf/config.json")

(defn get-db-field-similarity [config]
  (:database-field-for-similarity config))

(defn get-tfidf-variation [config]
  (:tfidf-variation config))

(defn get-cutoff [config]
  (:cutoff config))

(defn set-global-settings [config]
  (alter-var-root #'settings/similarity-db-field 
                  (constantly (get-db-field-similarity config)))
  (alter-var-root #'settings/tfidf-variation 
                  (constantly (get-tfidf-variation config)))
  (alter-var-root #'settings/cutoff 
                  (constantly (double (get-cutoff config))))
  (println settings/cutoff)
  nil)

(defn get-all-data []
  (db/get-data "movies"))

(defn get-all-data-projection [projection]
  (db/get-data "movies" :projection projection))

(defn insert-movies [movies]
  (db/insert-batch "movies" movies))

(defn get-movies-from-api []
  (try
    (let [movies (parser/get-movies)]
      (do
        (insert-movies movies)
        (str "success")))
    (catch Exception e
      (.printStackTrace e))))

(defn save-reviews [movie reviews]
  (try
    (db/update "movies" (:_id movie) {:reviews reviews})
  (catch Exception e
    (.printStackTrace e))))

(defn get-reviews-from-imdb [] 
  (dorun 
   (pmap 
    #(doseq [movie %]
       (save-reviews movie 
        (scraper/parse-reviews (:imdb_code movie))))
     (partition-all 200 
       (get-all-data-projection ["_id" "imdb_code"])))))

(defn get-reviews-from-api []
  (doseq [movie (get-all-data-projection ["_id"])]
    (save-reviews movie (parser/get-movie-reviews (:_id movie)))))

(defn calculate-recommendations []
  (calc/calculate-similarities 
   (get-all-data-projection 
    ["_id" "description" "genres" "title" "actors" "directors" "reviews"])))

(defn backup-data [data file]
  (spit file (prn-str data)))

(defn save-similarities-in-file []
  (let [movies (get-all-data)
        filtered-movies (map #(select-keys % [:_id :similar]) movies)]
    (backup-data filtered-movies 
     (get-similarities-backup-file))))
    
(defn get-json-from-file [file]
  (ch/parse-string (slurp file) true))

(defn get-data-from-file [file]
  (read-string (slurp file)))

(defn save-recommendations [movie]
  (try
    (db/update "movies" 
               (:_id movie) 
               {:similar-max (:similar movie)})
  (catch Exception e
    (.printStackTrace e))))

(defn import-movies []
  (insert-movies 
   (get-data-from-file 
    (get-movies-backup-file))))

(defn import-similarities []
  (let [movies 
        (get-data-from-file 
         (get-similarities-backup-file))]
    (doseq [movie movies]
      (save-recommendations movie))))

(defn populate-db-movies [config]
  (let [action (:movies config)]
    (cond 
      (= 1 action) (import-movies)
      (= 2 action) (get-movies-from-api))))


(defn populate-db-similarities [config]
  (let [action (:similarities config)]
    (cond 
      (= 1 action) (import-similarities)
      (= 2 action) (calculate-recommendations))))

(defn init []
  (let [config (get-json-from-file (get-config-file))]
    (do
      (set-global-settings config)
      (populate-db-movies config)
      (populate-db-similarities config)
      (println "success"))))