(defproject seminarski "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"] 
                 [com.novemberain/monger "3.0.1"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [http-kit "2.1.19"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [clojure-opennlp "0.3.3"]
                 [clj-fuzzy "0.1.8"]
                 [enlive "1.1.6"]
                 [incanter "1.5.6"]
                 [criterium "0.4.3"]]
  :plugins [[lein-ring "0.9.6"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.3"]]
                   :dependencies [[midje "1.7.0"]
                                  [javax.servlet/servlet-api "2.5"]]}}
  :ring {:init seminarski.handler/init
         :destroy seminarski.handler/destroy
         :handler seminarski.handler/app}
  :jvm-opts ["-Xmx3G"])
