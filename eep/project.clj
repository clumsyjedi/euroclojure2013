(defproject ec-eep "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojurewerkz/eep "1.0.0-alpha5"]
                 [compojure "1.1.5"] 
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 [ring "1.2.0"]
                 [clj-http "0.7.2"]
                 [cheshire "5.1.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clojurewerkz/quartzite "1.1.0"]
                 [clamq/clamq-activemq "0.4"]
  
             ]
  :main ec.eep.core
)
