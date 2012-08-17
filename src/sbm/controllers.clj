(ns sbm.controllers
  (:require sbm.views
            sbm.settings
            [sbm.solr :as solr] ))

(def sort-order [["timestamp" "desc"]])
(def facet-condtions [["facet" "on"] ["facet.field" "tag"] ["facet.date" "timestamp"] ["facet.date.start" "NOW/MONTH-10YEARS"] ["facet.date.end" "NOW"] ["facet.date.gap" "+1MONTHS"] ["facet.mincount" "1"]])
(def mlt-condtions [["mlt" "on"] ["mlt.fl" "body"]])


(defn make-q-param [q]
  (if q
    (clojure.string/join " "
                         (filter #(not (empty? %))
                                 (concat
                                   [(if (and (contains? q :q) (not (re-matches #"\A\s*\z" (get q :q))))
                                      (str (org.apache.solr.client.solrj.util.ClientUtils/escapeQueryChars (get q :q)))
                                      "*:*"
                                      )]
                                   [(if (contains? q :date)
                                      (let
                                        [ym (get q :date)]
                                        (if (re-matches #"\A\d{4}-\d{2}\z" ym)
                                          (str "timestamp:[" ym "-01T00:00:00Z TO " ym "-01T00:00:00Z+1MONTH]")))
                                      )]
                                   (if (contains? q :tags)
                                     (let [tags (set (clojure.string/split (get q :tags) #"\s+"))]
                                       (map #(str "tag:\"" (org.apache.solr.client.solrj.util.ClientUtils/escapeQueryChars %) "\"" ) tags)))
                                   )))
    nil
    )
  )

(defn search [params]
  (let [
        q           (get params :params)
        start       (* sbm.settings/rows (- (Integer. (get q :p "1")) 1))
        solr-server (solr/get-server)
        solr-query  (solr/make-query (make-q-param q) start sbm.settings/rows sort-order facet-condtions)
        response    (. solr-server query solr-query)
        ]
    (sbm.views/search response q)))


(defn index []
  (let [
        solr-server (solr/get-server)
        solr-query  (solr/make-query nil 0 sbm.settings/rows sort-order facet-condtions)
        response    (. solr-server query solr-query)
        ]
    (sbm.views/index response))
  )

(defn show [url]
  (let [
        solr-server (solr/get-server)
        solr-query  (solr/make-query (str "url:" (org.apache.solr.client.solrj.util.ClientUtils/escapeQueryChars url)) 0 1  [] mlt-condtions)
        response    (. solr-server query solr-query)
        ]
    (sbm.views/show response))
  )
