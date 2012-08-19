(ns sbm.views-test
  (:use clojure.test
        hiccup.core))

(require 'sbm.views)

(deftest test-make-query-param
         (are [x y] (= x y)
              "q=a&tags=b"  (sbm.views/make-query-param {:q "a" :tags "b"})
              "q=a&tags[]=c&tags[]=%3D"  (sbm.views/make-query-param {:q "a" :tags #{"c" "="}})
              "q=+%3D&tags=%26"  (sbm.views/make-query-param {:q " =" :tags "&"})
         ))

(deftest test-make-search-remove-tag-link
         (are [x y] (= x y)
              [:a {:href (java.net.URI. "/search?q=a&tags=b")} '([:span.label "remove tag: &lt;&amp;&gt;"])] (sbm.views/make-search-remove-tag-link {:q "a" :tags "b"} "<&>")
              "<a href=\"/search?q=%26&amp;tags=b\"><span class=\"label\">remove tag: &lt;&amp;&gt;</span></a>" (html (sbm.views/make-search-remove-tag-link {:q "&" :tags "b"} "<&>"))
         ))

(deftest test-make-search-remove-date-link
         (are [x y] (= x y)
              [:a {:href (java.net.URI. "/search?date=2012-08&q=a")} '([:span.label "remove date: &lt;&amp;&gt;"])] (sbm.views/make-search-remove-date-link {:q "a" :date "2012-08"} "<&>")
         ))

(deftest test-make-search-link
         (are [x y] (= x y)
              [:a {:href (java.net.URI. "/search?date=2012-08&q=a")} '("&lt;&amp;&gt;")] (sbm.views/make-search-link {:q "a" :date "2012-08"} "<&>")
         ))

(deftest test-make-mlt-unit
         (are [x y] (= x y)
              [:div.mlt.hero-unit [:p [:a {:href (java.net.URI. "/show/http%3A%2F%2Fmixi.jp%2F")} '("mixi")]] [:p [:a {:href (java.net.URI. "http://mixi.jp/")} '("http://mixi.jp/")]]]
              (sbm.views/make-mlt-unit (doto (org.apache.solr.common.SolrDocument.) (. put "url" "http://mixi.jp/") (. put "title" "mixi")))

              [:div.mlt.hero-unit [:p [:a {:href (java.net.URI. "/show/javascript%3Ahoge")} '("&lt;&amp;&gt;")]] [:p "javascript:hoge"]]
              (sbm.views/make-mlt-unit (doto (org.apache.solr.common.SolrDocument.) (. put "url" "javascript:hoge") (. put "title" "<&>")))
         ))

(deftest test-make-result-unit
         (are [x y] (= x y)
              [:div.result.hero-unit [:h1 [:a {:href (java.net.URI. "/show/http%3A%2F%2Fmixi.jp%2F")} '("mixi")]] [:p [:a {:href (java.net.URI. "http://mixi.jp/")} '("http://mixi.jp/")]]
               [:p "description"]
               ]
              (sbm.views/make-result-unit (doto (org.apache.solr.common.SolrDocument.) (. put "url" "http://mixi.jp/") (. put "title" "mixi") (.put "desc" "description")))

              [:div.result.hero-unit [:h1 [:a {:href (java.net.URI. "/show/javascript%3Ahoge")} '("&lt;&amp;&gt;")]] [:p "javascript:hoge"]
               [:p "&quot;&lt;&amp;&gt;"]
               ]
              (sbm.views/make-result-unit (doto (org.apache.solr.common.SolrDocument.) (. put "url" "javascript:hoge") (. put "title" "<&>") (.put "desc" "\"<&>")))
         ))
