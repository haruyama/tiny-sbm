(defproject compojure-test "0.1.0"
            :description "Compojure test"
            :dependencies [
                           [org.clojure/clojure "1.4.0"]
                           [compojure "1.1.1"]
                           [hiccup "1.0.0"]
                           [ring/ring-jetty-adapter "1.1.2"]
                           [org.apache.solr/solr-core "3.6.1"]
                           [org.apache.solr/solr-solrj "3.6.1"]
                           ]
            :plugins [[lein-ring "0.7.1"]]
            :dev-dependencies  [[ring/ring-devel "1.1.2"]]
            :ring {:handler sbm.routes/app}
            )
