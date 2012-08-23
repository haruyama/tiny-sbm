(ns sbm.views
  (:require sbm.settings)
  (:use [hiccup core page form element util def]))

(defn- u [s]
  (url-encode s))

(defelem safe-link-to
         [url & content]
         (try
           [:a {:href (to-uri url)} content]
           (catch Exception e
             [:a {:href ""} content]
             )))

(defn- param2str [n v]
  (if (instance? java.lang.String v)
    (str (u n) "=" (u v))
    (clojure.string/join "&" (map #(str (u n) "[]=" (u %)) v))))

(defn make-query-param [q]
  (clojure.string/join "&"
                       (map #(let [n (name (first %))
                                   v (second %)]
                               (param2str n v))
                            q)))

(defn- make-search-remove-link [q text param]
  (safe-link-to (str "/search?" (make-query-param q))
           [:span.label (h (str "remove " param ": " text))]))

(defn make-search-remove-tag-link [q text]
  (make-search-remove-link q text "tag"))

(defn make-search-remove-date-link [q text]
  (make-search-remove-link q text "date"))

(defn- navbar [title q]
  [:div.navbar
   [:div.navbar-inner
    [:div.container
     (safe-link-to {:class "brand"} "/" (h  title) )
     [:ul.nav
      (concat
        [[:li.offset3
          (form-to {:class "span4 navbar-from pull-right"} [:get "/search"]
                   (text-field {:value (get q :q)} "q"))
          ]]
        (if (contains? q :tags)
          (let [tags (get q :tags)]
            (map (fn [t]
                   (make-search-remove-tag-link
                     (dissoc
                       (let [new-tags (disj tags t)]
                         (if (empty? new-tags)
                           (dissoc q :tags)
                           (conj q [:tags new-tags]))
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
     (include-css "/css/sbm.css")
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

(defn make-search-link [q text]
  (safe-link-to (str "/search?" (make-query-param q)) (h text)))

(defn- make-tag-facet-link [count q]
  (let [
        n        (. count getName)
        c        (. count getCount)
        old-tags (get q :tags)
        tags     (if old-tags (conj old-tags n) n)
        ]
    (make-search-link (dissoc (conj q [:tags tags]) :p)
                      (str n "(" c ")"))))

(defn- make-tag-link [tag]
  [:li (make-search-link {:tags (set [tag])} tag)]
  )

(defn- make-date-facet-link [count q]
  (let [
        n (. count getName)
        d (subs n 0 (. n indexOf "-01T"))
        c (. count getCount)
        ]
    (make-search-link (dissoc (conj q [:date d]) :p)
                      (str d "(" c ")"))))

(defn make-mlt-unit [mlt]
  (let [
        url   (. mlt getFieldValue "url")
        title (. mlt getFieldValue "title")
        ]
    [:div.mlt.result-unit
     [:p (safe-link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
     [:p
      (if (re-find #"\Ahttps?://" url)
            (safe-link-to url (h url))
            (h url))]
     ]))


(defn- search-left [tags dates q]
  [:div.left
   (vector :ul.facets.tags
           (map #(vector :li.facet (make-tag-facet-link % q)) tags)
           )
   (vector :ul.facets.dates
           (map #(vector :li.facet (make-date-facet-link % q))
                (reverse dates)
                ))
   ])

(defn snippet-replace [str]
  (clojure.string/replace
    (clojure.string/replace str #"\t" "<strong>")
    #"\n" "</strong>"
  ))

(defn make-result-unit [result highlighting]
  (let [
        uuid (. result getFieldValue "uuid")
        url   (. result getFieldValue "url")
        title (. result getFieldValue "title")
        desc  (. result getFieldValue "desc")
        hi    (if highlighting (. highlighting get uuid))
        desc-snippets (if hi (. hi get "desc"))
        snippet (if desc-snippets (. desc-snippets get 0))
        timestamp (. result getFieldValue "timestamp")
        tags (. result getFieldValue "tag")
        ]
    [:div.result.result-unit
     [:h1 (safe-link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
     [:p
      (if (re-find #"\Ahttps?://" url)
            (safe-link-to url (h url))
            (h url))]
     (if timestamp
       [:p (h timestamp)]
       )
     (if tags
       [:ul
        (map make-tag-link (clojure.string/split tags #"\s+"))
        ]
       )
     (if snippet
       [:p (snippet-replace (h snippet)) ]
       (if desc [:p (h desc)]))
     ]))

(defn make-pagination [q page max-page range]
  (let [half-range (quot range 2)
        start      (if (> (- page half-range) 0) (- page half-range) 1)
        end        (if (< (+ start range -1) max-page) (+ start range -1) max-page)
        ]
    [:div.pagination
     (vector :ul
             (concat
               (if (not= page 1) [(vector :li (make-search-link (conj q [:p (str (- page 1))]) "Prev"))])
               (map
                 #(vector :li (if (= page %) {:class "active"}) (make-search-link (conj q [:p (str %)]) (str %)))
                 (clojure.core/range start (+ end 1)))
               (if (not= page end) [(vector :li (make-search-link (conj q [:p (str (+ page 1))]) "Next"))])
             ))
     ]))

(defn- search-right [results highlighting q]
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
             (map #(vector :li.result (make-result-unit % highlighting)) results)
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
                      (. response getHighlighting)
                      q)
        ))


(defn index [response]
  (search response {}))

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
             body  (. result getFieldValue "body")
             timestamp (. result getFieldValue "timestamp")
             tags (. result getFieldValue "tag")
             ]
         [:div.result.result-unit
          [:h1 (safe-link-to (str "/show/"(u url)) (if title (h title) "title not found"))]
          [:p (safe-link-to (if (re-find #"\Ahttps?://" url)  url) (h url))]
          (if timestamp
            [:p (h timestamp)]
            )
          (if tags
            [:ul
             (map make-tag-link (clojure.string/split tags #"\s+"))
             ]
            )
          (if desc [:p (h desc)])
          (if body [:p (h body)  ])
          ])
       )
     ]))

(defn- show-left [more-like-this]
  (vector :ul.mlts.unstyled
          (map #(vector :li.mlt (make-mlt-unit %)) more-like-this)
          ))

(defn show [response]
  (page "Tiny SBM"
        nil
        (show-left (take sbm.settings/more-like-this-rows  (. (.  (. response getResponse) get "moreLikeThis") getVal 0)))
        (show-right (. response getResults))
        ))
