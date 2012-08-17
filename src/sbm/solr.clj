(ns sbm.solr
  (:require sbm.settings)
  )

(import org.apache.solr.client.solrj.SolrQuery)
(import org.apache.solr.client.solrj.SolrQuery$ORDER)


(defn get-server []
  (new org.apache.solr.client.solrj.impl.CommonsHttpSolrServer sbm.settings/base-url))

(defn make-query [q start rows sort-params options]
  (let [
        query (if q
                (new SolrQuery q)
                (new SolrQuery "*:*")
                )
        ]
    (. query setStart (Integer. start))
    (. query setRows  (Integer. rows))
    (doseq [sort-param sort-params]
      (if (= (second sort-param) "asc")
        (. query setSortField (first sort-param) SolrQuery$ORDER/asc)
        (. query setSortField (first sort-param) SolrQuery$ORDER/desc)))
    (doseq [option options]
      ;      (. query setParam (first option) (second option)))
      (. query setParam (first option) (into-array [(second option)])))
    query
    ))
