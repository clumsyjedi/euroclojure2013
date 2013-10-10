(ns ec.eep.core
  (:require [clojurewerkz.eep.emitter :refer :all]
            [ec.eep.web :as web]
            [clojure.core.async :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clamq.activemq :refer :all]
            [clamq.protocol.producer :refer [publish]]
            [clamq.protocol.connection :refer [producer]]
            [clojurewerkz.eep.visualization :refer [visualise-graph]]
            [clojure.string :refer [lower-case]])
  (:gen-class))



(def asset-url "http://localhost/article.json?id=%s")
(def assets-url "http://localhost/articles.json")
(def activemq (producer (activemq-connection "tcp://127.0.0.1:61616") {}))

(def event-chain (create))

(build-topology event-chain
                :scheduler-event (deftransformer identity [:asset-event])
                :web-event (deftransformer identity [:asset-event])
                :asset-event (deftransformer
                         (fn [e]
                           (json/parse-string e true))
                         [:fetch-asset])
                :fetch-asset (deftransformer
                         (fn [e]
                           {:message (:body (http/get (format asset-url (:assetId e))))
                            :type (:assetType e)})
                         [:asset-type-split])
                :asset-type-split (defsplitter
                           (fn [e]
                             (-> e :type lower-case keyword))
                        [:article :page])
                :article (defobserver
                           (fn [e] (publish activemq "article" (:message e))))
                :page (defobserver
                           (fn [e] (publish activemq "page" (:message e)))))

;; (clojurewerkz.eep.visualization/visualise-graph event-chain)

(defn poll-web-events []
  (notify event-chain :web-event (<!! web/web-input))
  (recur))

(defn poll-scheduler-events []
  (let [_ (<!! web/scheduler-input)]
   (->> (http/get assets-url)
        :body
        (json/parse-string)
        (map json/encode)
        (mapv (partial notify event-chain :scheduler-event))))
  (recur))

(defn -main
  [& args]
  (web/init)

  (future (poll-web-events))
  (future (poll-scheduler-events))
  (println "Started..."))
