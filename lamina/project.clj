(defproject ec-lamina "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lamina "0.5.0"]
                 [compojure "1.1.5"] 
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 [ring "1.2.0"]
                 [clj-http "0.7.2"]
                 [cheshire "5.1.1"]
                 [clojurewerkz/quartzite "1.1.0"]
                 [clamq/clamq-activemq "0.4"]
             ]
  :main ec.lamina.core
)
