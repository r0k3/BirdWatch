(ns birdwatch.charts.cloud-chart
  (:require [birdwatch.util :as util]
            [birdwatch.stats.wordcount :as wc]
            [matthiasn.systems-toolbox.component :as comp]))

;;; WordCloud element (implemented externally in JavaScript)
(def cloud-elem (util/by-id "wordCloud"))
(def w (util/elem-width cloud-elem))
(def n 250)

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [on-click #(put-fn [:append-search-text %])
        word-cloud (.WordCloud js/BirdWatch w (* w 0.7) n on-click cloud-elem)]
    (atom {:word-cloud word-cloud})))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-payload]}]
  (.redraw (:word-cloud @cmp-state) (clj->js (wc/get-words msg-payload n))))

(defn component
  [cmp-id throttle-ms]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :state-pub-handler state-pub-handler
                        :opts {:throttle-ms throttle-ms}}))
