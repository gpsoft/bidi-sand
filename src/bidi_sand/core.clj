(ns bidi-sand.core
  (:require [bidi.bidi :as bb]
            [bidi.ring :as br]))

(defn- try-match
  [rt urls]
  (let [res (for [url urls]
              [url (if (coll? url)
                     (apply bb/match-route (concat [rt] url))
                     (bb/match-route rt url))])]
    (clojure.pprint/pprint rt)
    (clojure.pprint/pprint res)))

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

(try-match
  ["/" {:get {"index.html" :index}
        {:request-method :post
         :server-name "mydom.com"} {"about.html" :about}
        {:request-method #{:post :put}
         :server-name #(> (count %) 10)} {"index.html" :long}}]
  [["/index.html" :request-method :get]
   ["/about.html" :request-method :post :server-name "mydom.com"]
   ["/index.html" :request-method :put :server-name "longlong.com"]
   ["/index.html" :request-method :put :server-name "short.com"]])

(try-match
  ["/" {["foo/" [keyword :db/ident] "/bar"] :index}]
  ["/foo/db.hoge%2FPiyo/bar"])

(bb/path-for ["/" {["foo/" [keyword :db/ident] "/bar"] :index}] :index :db/ident :hoge/Hoge)