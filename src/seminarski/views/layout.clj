(ns seminarski.views.layout
  (:require [hiccup.page :refer [html5 include-css]]))

(defn template [& body]
  (html5
    [:head
     [:title "Movies"]]
    [:body body])
)

