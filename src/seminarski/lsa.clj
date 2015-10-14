(ns seminarski.lsa
  (:require [incanter.core :refer :all]))

(defn mtrx [dimension cols]
  (matrix (take dimension (repeat 0)) cols))

(defn calc-svd [orig-matrix] 
  (decomp-svd orig-matrix))

(defn get-reduced-v-matrix [svd]
  (sel (:V svd) :cols (range (count (:S svd)))))


