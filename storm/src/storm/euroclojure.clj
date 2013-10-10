(ns storm.euroclojure
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [clojure.tools.logging :as log]
            [backtype.storm.clojure :refer :all]
            [backtype.storm.config :refer :all]
            [clojure.core.async]
            [storm.euroclojure.web :as web]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :refer :all]
            [clojure.string :refer [lower-case]]
            [clamq.activemq :refer :all]
            [clamq.protocol.producer :refer [publish]]
            [clamq.protocol.connection :refer [producer]]
            [clojure.string :refer [lower-case]])
  (:gen-class))

(def asset-url "http://localhost/article.json")
(def assets-url "http://localhost/articles.json")
(def activemq (producer (activemq-connection "tcp://127.0.0.1:61616") {}))
(def callbacks (atom {}))

(defspout web-spout ["event-doc"]
  [conf context collector]
  (spout
   (nextTuple
    []
    (let [message (<!! web/web-input)]
      (emit-spout! collector [message])))))

(defspout scheduler-spout ["events-doc"]
  [conf context collector]
  (spout
   (nextTuple
    []
    (let [message (<!! web/scheduler-input)]
      (emit-spout! collector [message])))))

(defbolt split-events ["event-doc" "group-by" "count"]
  [tuple collector]
  (let [json (:body (http/get assets-url))
        events (map json/encode (json/parse-string json true))
        uuid (java.util.UUID/randomUUID)]
    (swap! callbacks assoc uuid 0)
    (doseq [e events]
        (emit-bolt! collector [e uuid (count events)] :anchor tuple))))

(defbolt get-asset ["event-doc" "asset-doc" "group-by" "count"]
  [tuple collector]
  (let [event-doc (get tuple "event-doc")
        event (json/parse-string event-doc true)
        url (lower-case (format asset-url
                                (:assetType event)
                                (:assetType event)
                                (:assetId event)))
        asset-doc (:body (http/get url))]
    (println "GET ASS" tuple)
    (emit-bolt! collector [event-doc
                           asset-doc
                           (get tuple "group-by")
                           (get tuple "count")]
                :anchor tuple)))

(defbolt amq-publish ["group-by" "count" ]
  [tuple collector]
  (println "ENQUEUE" tuple)
  (let [event (json/parse-string (get tuple "event-doc") true)]
    (publish activemq (-> event :assetType lower-case) (get tuple "asset-doc"))
    (emit-bolt! collector tuple :anchor tuple)))

(defbolt acknowledge []
  [tuple collector]
  (let [uuid (get tuple "group-by")
        count (get tuple "count")]
    (swap! callbacks update-in [(get tuple "group-by")] inc)
    (when (= (get @callbacks uuid) count)
      (ack! collector tuple)
      (println "THAT'S A WRAP!!"))))

(defn submit-topology [t]
  (.submitTopology (LocalCluster.)
                   "wps-events"
                   {TOPOLOGY-DEBUG true}
                   t))

(defn run-local! []
  (submit-topology
   (topology
    {"web-spout" (spout-spec web-spout)
     "scheduler-spout" (spout-spec scheduler-spout)}
    {"split-events" (bolt-spec {"scheduler-spout" :shuffle}
                               split-events)
     "get-asset" (bolt-spec {"web-spout" :shuffle
                             "split-events" :shuffle}
                            get-asset)
     "enqueue" (bolt-spec {"get-asset" :shuffle}
                          amq-publish)
     "callback" (bolt-spec {"enqueue" ["group-by"]}
                          acknowledge)
})))

(defn -main
  []
  (web/init)
  (run-local!)
  (log/info "Started..."))
