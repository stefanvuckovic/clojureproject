(ns seminarski.recommendation.t_similarity
  (:require [midje.sweet :refer :all]
            [seminarski.recommendation.similarity :refer :all]))

(fact "Cosine similarity calculator tester"
  (calculate-cosine-similarity [1 2 3 4 5] [1 2 3 4 5]) => 1.0
  (calculate-cosine-similarity [1 1 1 1] [2 2 2 2]) => 1.0
  (calculate-cosine-similarity [1 2 3 4 5] [5 4 3 2 1]) => (roughly 0.636363 0.000001))

(fact "dot product check"
  (calculate-parameters-for-cosine-similarity [1 2 3 4] [4 3 2 1]) => (contains [[:dot 20]])
  (calculate-parameters-for-cosine-similarity [1 2 3 4] [0 0 0 0]) => (contains [[:dot 0]]))

(fact "magnitude check"
  (calculate-parameters-for-cosine-similarity [1 2 3 4] [2 3 4 5]) => (contains [[:mag1 30.0] [:mag2 54.0]]))