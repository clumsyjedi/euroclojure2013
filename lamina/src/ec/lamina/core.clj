(ns ec.lamina.core
  (:require [lamina.core :refer :all]
            [lamina.viz :refer :all]
            [ec.lamina.bootstrap :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clamq.protocol.connection :refer [producer]]
            [clamq.protocol.producer :refer [publish]]
            [clamq.activemq :refer :all]
            [clojure.string :refer [lower-case]])
  (:gen-class))

(def asset-url "http://localhost/article.json?id=%s")
(def events-url "http://localhost/articles.json")
(def err-handler (fn [ex] (println "error:" ex)))
(def activemq (producer (activemq-connection "tcp://127.0.0.1:61616") {}))

(defn get-asset [e]
  (update-in e [:message]
             (fn [m] (:body (http/get (format asset-url (:assetId m)))))))

(def activemq-chain (pipeline
                     {:error-handler err-handler}
                     #(do (println %) %)
                     #(publish activemq (:type %) (:message %) {})))

(def event-chain (pipeline
                 {:error-handler err-handler}
                 #(json/parse-string % true)
                 #(hash-map :message % :type (-> % :assetType lower-case))
                 get-asset
                 #(enqueue output %)
                 ))

(def scheduler-chain (pipeline
                       {:error-handler err-handler}
                       (fn [_] (:body (http/get events-url)))
                       #(json/parse-string % true)
                       #(mapv json/encode %)
                       #(mapv event-chain %)))

(defn -main
  [& args]
  (init)
  (map* event-chain web-input)
  (map* scheduler-chain scheduler-input)
  (map* activemq-chain output)
  (println "Started..."))
