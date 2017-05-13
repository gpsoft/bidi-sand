(ns bidi-sand.core
  (:require [bidi.bidi :as bb]
            [bidi.schema :as bs]
            [ring.mock.request :as mock]))

(defn- print-res
  [rt res]
  (println "ROUTES:")
  (clojure.pprint/pprint rt)
  (println "URL & RESULT:")
  (clojure.pprint/pprint res))

(defn- try-match
  [rt urls]
  (let [res (for [url urls]
              [url (bb/match-route rt url)])]
    (print-res rt res)))

(defn- try-match*
  [rt reqs]
  (let [res (for [req reqs
                  :let [url (:uri req)]]
              [(:request-method req)
               (:server-name req)
               url
               (:handler (bb/match-route* rt url req))])]
    (print-res rt res)))

(try-match
  ["/index.html" :index]
  ["/index.html"
   "/about.html"])

(try-match
  ["/" {"index.html" :index
        "about.html" :about}]
  ["/index.html"
   "/about.html"])

(try-match
  ["/" [["index.html" :index]
        ["about.html" :about]]]
  ["/index.html"
   "/about.html"])

(try-match
  ["/" {"index.html" :index
        "about.html" :about
        "articles/" {"index.html" :article-index
                     "article.html" :article}}]
  ["/index.html"
   "/about.html"
   "/articles/index.html"
   "/articles/article.html"])

(try-match
  ["/" {["hoge/" :id "/fuga/" :name "/index.html"] :index
        ["hoge/" [#"\d+" :id] "/fuga/" [#"(piyo)+" :name]] :about}]
  ["/hoge/12/fuga/piyo/index.html"
   "/hoge/30/fuga/piyopiyo"
   "/hoge/31/fuga/PiyoPiyo"])

(try-match*
  ["/" {:get {"index.html" :index}
        {:request-method :post
         :server-name "mydom.com"} {"about.html" :about}
        {:request-method #{:post :put}
         :server-name #(> (count %) 10)} {"index.html" :long}}]
  [(mock/request :get "/index.html")
   (mock/request :post "http://mydom.com/about.html")
   (mock/request :put "http://longlong.com/index.html")
   (mock/request :put "http://short.com/index.html")])

(try-match
  ["/" {["foo/" [keyword :db/ident] "/bar"] :index}]
  ["/foo/db.hoge%2FPiyo/bar"])

(bb/path-for ["/" {["foo/" [keyword :db/ident] "/bar"] :index}] :index :db/ident :hoge/Hoge)



(try-match
  ["/" {#{"" "index.html" "index"} :index}]
  ["/"
   "/index.html"
   "/index"])

(bb/path-for ["/" {#{"index.html" "index"} :index}] :index)

(defn index-handler [req] nil)
(defn about-handler [req] nil)
(try-match
  ["/" {"index.html" (bb/tag index-handler :index)
        "about.html" (bb/tag about-handler :about)}]
  ["/index.html"
   "/about.html"])

(bb/path-for
  ["/" {"index.html" (bb/tag index-handler :index)
        "about.html" (bb/tag about-handler :about)}]
  about-handler)

(bb/path-for
  ["/" {"index.html" (bb/tag index-handler :index)
        "about.html" (bb/tag about-handler :about)}]
  :about)

(bb/route-seq
  ["/" {"index.html" :index,
        "about.html" :about,
        "articles/" {"index.html"       :article-index,
                     "article.html"     :article}
        "misc/" {["hoge/" :id "/fuga/"
                  :name "/index.html"]  :hoge-index}}])

;; (s/check bs/RoutePair routes)
