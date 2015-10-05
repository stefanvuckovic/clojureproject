(ns seminarski.tfidf
  (:require [opennlp.nlp :as nlp]
            [clj-fuzzy.stemmers :as stemmer]
            [clojure.string :as string]
            [clojure.set :as cljset]
            [seminarski.scraping :as scrape]
            [seminarski.settings :as settings]))

(def tokenizer (nlp/make-tokenizer "models/en-token.bin"))
(def name-finder (nlp/make-name-finder "models/en-ner-person.bin"))

(defn stem [word]
  (stemmer/porter word))

(defn get-stop-words [file] 
  (set (tokenizer (slurp file))))

(defn transform-to-lowercase [sentence] 
  (string/lower-case sentence))

(defn filter-stop-words [stop-word-list tokens]
  (remove stop-word-list tokens))

(defn filter-person-names [tokens]
  (remove 
    (set (seminarski.tfidf/name-finder tokens)) 
    tokens))

(defn process-single-document [doc]
  (->> doc
       (tokenizer)
       (filter-person-names)
       (map transform-to-lowercase)
       (filter-stop-words (get-stop-words "util/stopwords.txt"))
       (map stem)
       (frequencies)))

(defn partial-process [doc]
  (->> doc
       (map transform-to-lowercase)
       (map stem)
       (frequencies)))

(defn calculate-tf-classic [frequence scale]
  (let [frequence (or frequence 0)]
    (/ frequence scale)))

(defn calculate-tf-log [frequence scale]
  (let [frequence (or frequence 0)]
    (+ 1 (Math/log10 frequence))))

(defn calculate-tf-normalize-by-max [frequence scale]
  (let [frequence (or frequence 0)]
    (+ 0.4 (* 0.6 (/ frequence scale)))))

(defn calculate-idf [all-docs term]
  (Math/log10
    (/ (count all-docs) (count (filter #((:tokens %) term) all-docs)))))

(defn get-all-terms [all-docs]
  (reduce cljset/union #{} 
    (map #(set (keys (:tokens %))) all-docs)))

(defn get-all-terms-with-frequencies [all-docs]
  (reduce #(merge-with + %1 (:tokens %2)) {} all-docs))

(defn calculate-idf-key-value [all-docs result-map term]
  (assoc result-map term
    (calculate-idf all-docs term)))

(defn get-all-idfs "Get all terms with idf values bound to them" [all-docs all-terms]
  (reduce (partial calculate-idf-key-value all-docs) {} all-terms))

(defn calculate-tfidf [idf frequence scale tf-fn]
  (* (tf-fn frequence scale) idf))
 
(defn get-cast-list [casts]
  (reduce #(assoc %1 (:imdb_code %2) 1) {} casts))
 
 (defn add-key-value-pair [significance weight-by result-map key-value]
   (let [[k v] key-value] 
     (assoc result-map k (* significance weight-by v))))
 
 (defn get-weighted-attributes [weight-by terms significance]
   (reduce 
     (partial add-key-value-pair significance weight-by) {} terms))
 
 (defn merge-vectors [& maps]
   (apply (partial merge-with +) maps))

 
  (defn create-vector-for-tfidf [movie]
   (let [description (process-single-document (:description movie))
         reviews 
           (let [revs (:reviews movie)]
             (if (empty? revs)
               {}
               (process-single-document (scrape/remove-stop-words (string/join " " (take 2 (:reviews movie)))))))
         base (merge-vectors description reviews)
         sum (reduce + (vals base))
         actors (get-weighted-attributes sum (get-cast-list (:actors movie)) 0.05)
         directors (get-weighted-attributes sum (get-cast-list (:directors movie)) 0.05)
         title (get-weighted-attributes sum (process-single-document (:title movie)) 0.03)
         genres (get-weighted-attributes sum (partial-process (:genres movie)) 0.05)
         final-vector (merge-vectors base actors directors title genres)]
     {:_id (:_id movie) :tokens final-vector}))

(defn get-preprocessed-data [data]
  (map create-vector-for-tfidf data))


(defn tfidf-for-movie [tf-fn scale vocabulary movie]
  (try
      (assoc-in movie [:tokens] 
        (into {}
          (map 
            (fn [[term freq]] 
              [term (calculate-tfidf (vocabulary term) freq scale tf-fn)])
            (:tokens movie))))
   (catch Exception e
     (.printStackTrace e))))

(defn tfidf-scale-by [scale-fn tf-fn vocabulary movie]
  (let [scale (reduce scale-fn (vals (:tokens movie)))
        scale-final (if(= 0 scale) 1 scale)]
    (tfidf-for-movie tf-fn scale-final vocabulary movie)))
 

(defn tfidf-for-all-without-cutoff "Calculate tfidf values for all movies" [movies vocabulary tfidf-fn]
  (let [all-terms (get-all-terms vocabulary)
        idfs (get-all-idfs movies all-terms)
        tfidf-map (map (partial tfidf-fn idfs) movies)]
    (spit (str "conf/tfidf" settings/tfidf-variation ".edn") (prn-str tfidf-map))
    tfidf-map))

(defn remove-words [col words-to-remove]
     (apply dissoc col words-to-remove))

(defn sort-fn [map-to-sort]
  (into (sorted-map-by (fn [key1 key2]
                             (compare 
                               [(get map-to-sort key1) key1]
                               [(get map-to-sort key2) key2])))
         map-to-sort))

(defn tfidf-for-all-with-cutoff "Calculate tfidf values for all movies" [movies vocabulary tfidf-fn]
  (let [all-terms (sort-fn (get-all-terms-with-frequencies vocabulary))
        removal-percentage (Math/ceil (* settings/cutoff (count all-terms)))
        words-to-remove (set (into (keys (take removal-percentage all-terms)) (keys (take-last removal-percentage all-terms))))
        voc (cljset/difference (set (keys all-terms)) words-to-remove)
        movies-final (map #(update-in % [:tokens] remove-words words-to-remove) movies)
        idfs (get-all-idfs movies-final voc)
        tfidf-map (map (partial tfidf-fn idfs) movies-final)]
    (spit (str "conf/idf" settings/cutoff ".edn") (prn-str idfs))
    (spit (str "conf/tfidf" settings/tfidf-variation ".edn") (prn-str tfidf-map))
    tfidf-map))

(defmulti tfidf-for-all 
  (fn [movies vocabulary tfidf-fn cutoff]
    cutoff))

(defmethod tfidf-for-all 0.0
  [movies vocabulary tfidf-fn cutoff] (tfidf-for-all-without-cutoff movies vocabulary tfidf-fn))

(defmethod tfidf-for-all :default
  [movies vocabulary tfidf-fn cutoff] (tfidf-for-all-with-cutoff movies vocabulary tfidf-fn))

(defmulti calculate-tfidf-for-all 
  (fn [movies vocabulary variation]
    variation))

(defmethod calculate-tfidf-for-all :classic
  [movies vocabulary variation] (tfidf-for-all movies vocabulary (partial tfidf-scale-by + calculate-tf-classic) settings/cutoff))

(defmethod calculate-tfidf-for-all :aug
  [movies vocabulary variation] (tfidf-for-all movies vocabulary (partial tfidf-scale-by max calculate-tf-normalize-by-max) settings/cutoff))

(defmethod calculate-tfidf-for-all :log
  [movies vocabulary variation] (tfidf-for-all movies vocabulary (partial tfidf-for-movie calculate-tf-log 0) settings/cutoff))



