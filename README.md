# bidi-sand

`juxt/bidi`の練習。

# juxt/bidi

routingと逆引きに特化したライブラリ。

    https://github.com/juxt/bidi
    [bidi "2.1.0"]

- URLからハンドラへの対応
- ハンドラからURLの生成


# 基本

    (require '[bidi.bidi :as bb])
           ;; functions:
           ;;   bb/match-route
           ;;   bb/match-route*
           ;;   bb/path-for
           ;;   bb/tag
           ;;   bb/route-seq
           ;; protocols:
           ;;   bb/Pattern
           ;;   bb/Matched

ベクタ(やマップ)でroutesを定義。

    (def routes
      ;pattern               matched
      ["/index.html"         :index])

左をpattern、右をmatchedと呼ぶ。上記例ではmatchedにキーワードを使っているが、普通は関数(ハンドラ)を指定する。厳密には、patternとmatchedには、それぞれプロトコル`bb/Pattern`と`bb/Matched`をサポートする値を指定する必要がある。

`bb/match-route`で任意のURLを判定。マッチすれば、マップでハンドラが返る。このマップをmatch-contextと呼ぶ。マッチしなければ`nil`。

    (bb/match-route routes "/index.html")   ;;=> {:handler :index}
    (bb/match-route routes "/about.html")   ;;=> nil

逆引きは、`bb/path-for`。

    (bb/path-for routes :index)   ;;=> "/index.html"


# 例

複数のパターンをマップで定義。

    ROUTES:
      ["/" {"index.html" :index,
            "about.html" :about}]

    URL & RESULT:
      "/index.html"      {:handler :index}
      "/about.html"      {:handler :about}

順序が重要な場合は、マップの代わりにベクタで定義。

    ROUTES:
      ["/" [["index.html" :index]
            ["about.html" :about]]]

    URL & RESULT:
      "/index.html"      {:handler :index}
      "/about.html"      {:handler :about}

ネストOK。

    ROUTES:
      ["/" {"index.html" :index,
            "about.html" :about,
            "articles/" {"index.html"   :article-index,
                         "article.html" :article}}]
    URL & RESULT:
      "/index.html"            {:handler :index}
      "/about.html"            {:handler :about}
      "/articles/index.html"   {:handler :article-index}
      "/articles/article.html" {:handler :article}

パスからパラメータを抽出。正規表現でパターン指定可能。

    ROUTES:
      ["/" {["hoge/" :id "/fuga/" :name "/index.html"]         :index,
            ["hoge/" [#"\d+" :id] "/fuga/" [#"(piyo)+" :name]] :about}]

    URL & RESULT:
      "/hoge/12/fuga/piyo/index.html"  {:route-params {:id "12", :name "piyo"},
                                        :handler :index}
      "/hoge/30/fuga/piyopiyo"         {:route-params {:id "30", :name "piyopiyo"},
                                        :handler :about}
      "/hoge/31/fuga/PiyoPiyo"         nil

パラメータをキーワードで抽出。

    ROUTES:
      ["/" {["foo/" [keyword :db/ident] "/bar"]     :index}]

    URL & RESULT:
      "/foo/db.hoge%2FPiyo/bar"        {:route-params {:db/ident :db.hoge/Piyo},
                                        :handler :index}

少々複雑になるが、httpメソッドや様々なリクエスト属性を条件に加えることも可能。

    [:get              {"index.html" :index}]   ;; メソッドだけならキーワードで。
    [{:request-method :post                     ;; それ以外はRing形式のマップで。
      :server-name "mydom.com"
      :scheme "https"} {"about.html" :about}]
    ;; https://github.com/ring-clojure/ring/wiki/Concepts

この場合、`bb/match-url`の引数には、URL以外のリクエスト属性を指定する必要がある。あるいは`bb/match-url*`を使って、リクエストマップを引数に追加。

    (bb/match-url routes "/index.html" :request-method :get ....)
    (bb/match-url* routes "/index.html" req)

さらに例。

    ROUTES:
      ["/" {:get                              {"index.html" :index},
            {:request-method :post,
             :server-name "mydom.com"}        {"about.html" :about},
            {:request-method #{:post :put},
             :server-name #(> (count %) 10)}  {"index.html" :long}}]

    URL & RESULT:
      "/index.html" :request-method :get      {:handler :index,
                                               :request-method :get}
      "/about.html" :request-method :post
                    :server-name "mydom.com"  {:server-name "mydom.com",
                                               :handler :about,
                                               :request-method :post}
      "/index.html" :request-method :put
                    :server-name "longlong.com" {:server-name "longlong.com",
                                                 :handler :long,
                                                 :request-method :put}
      "/index.html" :request-method :put
                    :server-name "short.com"    nil

複数パターンを1つのハンドラへ。

    ROUTES:
      ["/" {#{"" "index" "index.html"} :index}]

    URL & RESULT:
      "/"           {:handler :index}
      "/index.html" {:handler :index}
      "/index"      {:handler :index}

逆引きのときは先頭のパターンを採用。といってもsetの先頭なので…。

    (bb/path-for
      ["/" {#{"index.html" "index"} :index}] :index)   ;;=> "/index"

ハンドラにタグ付けする。

    ROUTES:
      ["/" {"index.html" (bb/tag index-handler :index)
            "about.html" (bb/tag about-handler :about)}]
    URL & RESULT:
      "/index.html" {:tag :index,
                     :handler #function}
      "/about.html" {:tag :about,
                     :handler #function}

逆引きの時にタグが使える。

    (bb/path-for
      ["/" {"index.html" (bb/tag index-handler :index)
            "about.html" (bb/tag about-handler :about)}]
      :about)       ;;=> "/about.html"


# Ringで使う
## ハンドラ

    (require '[bidi.ring :as br])
           ;; functions
           ;;   br/make-handler
           ;; records
           ;;   br/Redirect

    (def routes
      ["/index.html" handler-index]
      ["/about.html" handler-about])

    (def app
      (-> (br/make-handler routes)
        wrap-a
        wrap-b))

## redirect

    ;; br/Redirectは、bb/Matchedプロトコル準拠。
    ["/articles" {"/new" my-handler
                  "/old" (->br/Redirect
                           307            ;; status
                           my-handler)}]  ;; target

## resources

    ;; br/Resources or br/ResourcesMaybe
    ["/resources" (->ResourcesMaybe
                    {:prefix "public/"
                     :mime-types {"pdf" "application/pdf"
                                  "txt" "text/plain"}})

    ;; br/Files
    ["pics/" (->Files {:dir "/tmp/pics"})]

## middleware

    ;; br/WrapMiddleware
    (bb/match-route
        ["/index.html" (->br/WrapMiddleware handler wrap-a)]
        "/index.html")

# routesの検証

全routesを列挙。

    (bb/route-seq
      ["/" {"index.html" :index,
            "about.html" :about,
            "articles/" {"index.html"       :article-index,
                         "article.html"     :article}
            "misc/" {["hoge/" :id "/fuga/"
                      :name "/index.html"]  :hoge-index}}])
    ;;=>
    (#bidi.bidi.Route{:handler :index,
                      :path ["/" "index.html"]}
     #bidi.bidi.Route{:handler :about,
                      :path ["/" "about.html"]}
     #bidi.bidi.Route{:handler :article-index,
                      :path ["/" "articles/" "index.html"]}
     #bidi.bidi.Route{:handler :article,
                       :path ["/" "articles/" "article.html"]}
     #bidi.bidi.Route{:handler :hoge-index,
                       :path ["/" "misc/" ["hoge/" :id "/fuga/" :name "/index.html"]]})


`plumatic/schema`を使って、routesのスキーマを定義済み(`bidi.schema/RoutePair`)。

    (require '[schema.core :as s])

    (s/check bidi.schema/RoutePair routes)


