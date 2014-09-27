(ns birdwatch.main
  (:gen-class)
  (:require
   [birdwatch.twitterclient.core :as tc]
   [birdwatch.communicator :as comm]
   [birdwatch.persistence :as p]
   [birdwatch.percolator :as perc]
   [birdwatch.http :as http]
   [birdwatch.switchboard :as sw]
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [clj-pid.core :as pid]
   [com.stuartsierra.component :as component]))

(def conf (edn/read-string (slurp "twitterconf.edn")))

(defn get-system [conf]
  "Create system by wiring individual components so that component/start
  will bring up the individual components in the correct order."
  (component/system-map
   :communicator-channels (comm/new-communicator-channels)
   :communicator  (component/using (comm/new-communicator) {:channels :communicator-channels})
   :twitterclient-channels (tc/new-twitterclient-channels)
   :twitterclient (component/using (tc/new-twitterclient conf) {:channels :twitterclient-channels})
   :persistence-channels (p/new-persistence-channels)
   :persistence   (component/using (p/new-persistence conf) {:channels :persistence-channels})
   :percolation-channels (perc/new-percolation-channels)
   :percolator    (component/using (perc/new-percolator conf) {:channels :percolation-channels})
   :http          (component/using (http/new-http-server conf) {:communicator :communicator})
   :switchboard   (component/using (sw/new-switchboard) {:comm-chans :communicator-channels
                                                         :tc-chans :twitterclient-channels
                                                         :pers-chans :persistence-channels
                                                         :perc-chans :percolation-channels})))
(def system (get-system conf))

(defn -main [& args]
  (pid/save (:pidfile-name conf))
  (pid/delete-on-shutdown! (:pidfile-name conf))
  (log/info "Application started, PID" (pid/current))
  (alter-var-root #'system component/start))
