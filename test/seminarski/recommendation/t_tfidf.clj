(ns seminarski.recommendation.t_tfidf
   (:require [midje.sweet :refer :all]
            [seminarski.recommendation.tfidf :refer :all]))

(fact "Stemmer test"
  (stem "movies") => "movi"
  (stem "working") => "work"
  (stem "watches") => "watch")

(fact "Lower case test"
  (transform-to-lowercase "The Lord of the Rings is a film series") 
  => "the lord of the rings is a film series")

(fact "Stop word filter test"
  (filter-stop-words 
   (get-stop-words "util/stopwords.txt") 
   ["the" "lord" "of" "the" "rings" "is" "a" "film" "series"]) 
  => ["lord" "rings" "film" "series"])

(fact "Tfidf preprocessing test"
  (process-single-document "This movie is the best movie") 
  => {"movi" 2 "best" 1}
  (process-single-document "This movie is the best movie of all movies. Movie, movies") 
  => (contains [["movi" 5]]))

(fact "classic tf formula test"
  (calculate-tf-classic 10 20) => 1/2
  (calculate-tf-classic 0 10) => 0
  (calculate-tf-classic nil 10) => 0)

(fact "log tf formula test"
  (calculate-tf-log 10 20) => 2.0
  (calculate-tf-log 1 10) => 1.0
  (calculate-tf-log nil 10) => 0)

(fact "augmentative tf formula test"
  (calculate-tf-aug 10 20) => 0.7
  (calculate-tf-aug  1 10) => 0.46
  (calculate-tf-aug  nil 10) => 0)

(fact "idf calculation test"
  (calculate-idf 
   [{:_id 1 :tokens {"movie" 2 "lord" 1 "rings" 4}} 
    {:_id 2 :tokens {"movie" 4}} 
    {:_id 3 :tokens {"lord" 2 "rings" 5}}]
  "movie") => (roughly 0.17609125 0.00000001))

(fact "build map with imdb code as key and frequency 1 as value from actor or director collection"
  (get-cast-list 
   [{:imdb_code "code1" :name "Name1"} 
    {:imdb_code "code2" :name "Name2"}
    {:imdb_code "code3" :name "Name3"}]) 
  => {"code1" 1 "code2" 1 "code3" 1})

(fact "term weighting test"
  (get-weighted-attributes 
   100 
   {"term1" 10 "term2" 20 "term3" 30} 
   0.1) => {"term1" 100.0 "term2" 200.0 "term3" 300.0})

(fact "merge maps while summing values with the same key"
  (merge-vectors 
   {"term1" 2 "term2" 4 "term3" 5} 
   {"term2" 3 "term3" 5 "term4" 6}) 
  => {"term1" 2 "term2" 7 "term3" 10 "term4" 6})

(fact "testing preprocessing function for tfidf"
  (unfinished get-mv)
  (create-vector-for-tfidf 
   (get-mv)) 
  => (just 
      {:_id ..id.. 
       :tokens 
        (just {"term1" 3 
               "term2" 3 
               "title1"(roughly 0.18 0.000001) 
               "code1" (roughly 0.3 0.000001) 
               "code2" (roughly 0.3 0.000001) 
               "code3" (roughly 0.3 0.000001) 
               "code4" (roughly 0.3 0.000001) 
               "genre1" (roughly 0.3 0.000001) 
               "genre2" (roughly 0.3 0.000001)})})
  (provided 
   (get-mv) 
   => {:_id ..id.. 
       :title "title1" 
       :description 
        (str "term1" " " "term2") 
       :genres ["genre1" "genre2"]
       :actors [{:imdb_code "code1" :name anything} 
                {:imdb_code "code2" :name anything}]
       :directors [{:imdb_code "code3" :name anything} 
                   {:imdb_code "code4" :name anything}]
       :reviews [(str "term1" " " "term2") 
                 (str "term1" " " "term2")]}))
