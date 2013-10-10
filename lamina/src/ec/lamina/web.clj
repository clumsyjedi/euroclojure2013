(ns ec.lamina.web
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer :all]
            [clojure.core.async :refer :all]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]))

(def web-input (chan))
(def scheduler-input (chan))

(defroutes main-routes
 (POST "/" {body :body}
       (go (>! web-input (slurp body)))
       {:status 201}))

(def handler (handler/site main-routes))

(defjob PollWps
  [ctx]
  (go (>! scheduler-input (java.util.Date.))))

(defn init []
  (run-jetty handler {:port 3333 :join? false})

  (qs/initialize)
  (qs/start)


  (let [job (j/build
             (j/of-type PollWps)
             (j/with-identity (j/key "jobs.wps.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (cron-schedule "*/5 * * * * ?"))))]
    (qs/schedule job trigger)))
