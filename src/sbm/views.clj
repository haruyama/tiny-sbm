(ns sbm.views
  (:require sbm.settings)
  (:use [hiccup core page form element]))

(defn- urlencode [s]
  (. java.net.URLEncoder encode s "UTF-8")
  )

(defn- u [s]
  (urlencode s)
  )

(defn- make-query-param [q]
  (clojure.string/join "&"
                       (map #(let [n (subs (str (first %)) 1)
                                   v (second %)
                                   ]
                               (str (u n) "=" (u (second %))))
                            q)))

(defn- make-search-remove-tag-link [q text]
  (if (not (re-matches #"\A\s*\z" text ))
    (link-to (str "/search?" (make-query-param q))
             [:span.label (str "remove tag: \"" (h text) "\"")])))

(defn- make-search-remove-date-link [q text]
  (link-to (str "/search?" (make-query-param q))
           [:span.label (str "remove date\"" (h text) "\"")]))


(defn- navbar [title q]
  [:div.navbar
   [:div.navbar-inner
    [:div.container
     (link-to {:class "brand"} "/" (h  title) )
     [:ul.nav
      (concat
        [[:li.offset3
          (form-to {:class "span4 navbar-from pull-right"} [:get "/search"]
                   (text-field {:value (get q :q)} "q"))
          ]]
        (if (contains? q :tags)
          (let [tags (set (clojure.string/split (get q :tags) #"\s+"))]
            (map (fn [t]
                   (make-search-remove-tag-link
                     (dissoc
                       (let [new-tags (set (disj tags t))]
                         (if (empty? new-tags)
                           (dissoc q :tags)
                           (conj q [:tags (clojure.string/join " " new-tags)]))
                         )
                       :p)
                     t))
                 tags)))
        [
         (if (contains? q :date)
           (make-search-remove-date-link (dissoc q :date) (get q :date))
           )
         ]
        )
      ]]]])

(defn- page [title q left right]
  (html5
    [:head
     [:title (h title)]
     (include-css "/css/bootstrap.css")
     (include-js "/js/jquery-1.8.0.min.js")
     (include-js "/js/bootstrap.js")
     ]
    [:body
     [:div.container-fluid
      (navbar title q)
      [:div.row-fluid
       [:div.span3
        left
        ]
       [:div.span9
        right
        ]]]]))



(defn- make-search-link [q text]
  (link-to (str "/search?" (make-query-param q))
           (h text)))

(defn- make-tag-link [count q]
  (let [n (. count getName)
        c (. count getCount)
        old-tags (get q :tags)
        tags     (if old-tags (str old-tags  " " n)  n)
        ]
    (make-search-link (dissoc (conj q [:tags tags]) :p)
                      (str n "(" c ")"))
    ))

(defn- make-date-link [count q]
  (let [n (. count getName)
        d (subs n 0 (. n indexOf "-01T"))
        c (. count getCount)
        ]
    (make-search-link (dissoc (conj q [:date d]) :p)
                      (str d "(" c ")")))
  )

(defn- make-mlt-link [mlt]
  (let [
        url   (. mlt getFieldValue "url")
        title (. mlt getFieldValue "title")
        ]
    [:div.mlt.hero-unit
     [:p (link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
     [:p (link-to (if (re-find #"\Ahttps?://" url) url nil) (h url))]
     ])
  )


(defn- search-left [tags dates q]
  [:div.left
   (vector :ul.facets.tags
           (map #(vector :li.facet (make-tag-link % q)) tags)
           )
   (vector :ul.facets.dates
           (map #(vector :li.facet (make-date-link % q))
                (reverse dates)
                ))
   ])

(defn- make-result-view [result]
  (let [
        url   (. result getFieldValue "url")
        title (. result getFieldValue "title")
        desc  (. result getFieldValue "desc")
        ]
    [:div.result.hero-unit
     [:h1 (link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
     [:p (link-to (if (re-find #"\Ahttps?://" url)  url) (h url))]
     (if desc [:p (h desc)])
     ])
  )

(defn- make-pagination [q page max-page range]
  (let [half-range (quot range 2)
        start      (if (> (- page half-range) 0) (- page half-range) 1)
        end        (if (< (+ start range -1) max-page) (+ start range -1) max-page)
        ]
    [:div.pagination
     (vector :ul
             (concat
               [ (if (not= page 1) (vector :li (make-search-link (conj q [:p (str (- page 1))]) "Prev")))]
               (map
                 #(vector :li (make-search-link (conj q [:p (str %)]) (h (str %))))
                 (clojure.core/range start (+ end 1)))
               [(if (not= page end) (vector :li (make-search-link (conj q [:p (str (+ page 1))]) "Next")))])

             )
     ])
  )


(defn- search-right [results q]
  (let [
        num-found (. results getNumFound)
        start     (. results getStart)
        ]
    [:div.right
     (if (> num-found 0)
       [:div.alert.alert-success (h (str num-found " results"))]
       [:div.alert.alert-error   "Not Found"]
       )
     (vector :ul.results.unstyled
             (map #(vector :li.result (make-result-view %)) results)
             )
     (let [
           page      (+ 1 (quot start sbm.settings/rows))
           max-page  (+ 1 (quot (- num-found 1) sbm.settings/rows))
           ]
       (make-pagination q page max-page sbm.settings/pagination-range)
       )
     ]))


(defn search [response q]
  (page "Tiny SBM"
        q
        (search-left (take sbm.settings/tag-rows (. (. (. response getFacetFields) get 0) getValues))
                     (. (. (. response getFacetDates) get 0) getValues)
                     q)
        (search-right (. response getResults)
                      q)
        )
  )


(defn index [response]
  (search response {})
  )

(defn- show-right [results]
  (let [
        num-found (. results getNumFound)
        ]
    [:div.right
     (if (= num-found 0)
       [:div.alert.alert-error   "Not Found"]
       (let [
             result (. results get 0)
             url   (. result getFieldValue "url")
             title (. result getFieldValue "title")
             desc  (. result getFieldValue "desc")
             ]
         [:div.result.hero-unit
          [:h1 (link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
          [:p (link-to (if (re-find #"\Ahttps?://" url)  url) (h url))]
          (if desc [:p (h desc)])
          ])
       )
     ]))

(defn- show-left [more-like-this]
  (vector :ul.mlts.unstyled
          (map #(vector :li.mlt (make-mlt-link %)) more-like-this)
          )
  )

(defn show [response]
  (page "Tiny SBM"
        nil
        (show-left (take sbm.settings/more-like-this-rows  (. (.  (. response getResponse) get "moreLikeThis") getVal 0)))
        (show-right (. response getResults))
        )
  )
