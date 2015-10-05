(ns seminarski.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.core :refer [html]]))

(defn template [css js & body]
  (html5
    [:head
     [:title "Movies"]
     (let [css (or css [])
           csss (into ["/css/bootstrap.min.css"] css)]
    (apply include-css csss))]
    [:body body
    (let [js (or js [])
          jss (into ["/js/bootstrap.min.js" "/js/jquery-1.11.3.min.js"] js)]
      (apply include-js jss))]))

(defn to-html [& body]
  (html body))
