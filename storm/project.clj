(defproject ec-storm "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 [clj-http "0.7.2"]
                 [cheshire "5.1.1"]
                 [compojure "1.1.5"]
                 [clj-camel-holygrail "0.3.1"]
                 [clojurewerkz/quartzite "1.1.0"]
                 [clamq/clamq-activemq "0.4"]
                 ]
  :profiles {:dev {:dependencies [[storm "0.8.2"]]}}
  :ring {:handler storm.euroclojure/handler}
  :main storm.euroclojure)
