(ns website.sls
  (:use [ring.adapter.jetty]
        [ring.util.response]
        [clojure.pprint]
        [clj.qrgen]
        [net.cgrand.enlive-html]
        [ring.middleware.params         :refer [wrap-params]]
        [ring.middleware.cookies        :only [wrap-cookies]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [selmer.parser])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clj-http.client :as client]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [selmer.parser :refer [render-file]]))

(defn map-keywords
  ([my-map] (into {} 
              (for [[k v] my-map] 
                [(keyword k) v]))))

(defn global-response [resp]
  (-> resp response (content-type "text/html; charset=UTF-8")))

(defn connect-db
  "mongodb://[username:password@]host1[:port1][,host2[:port2],…[,hostN[:portN]]][/[database][?options]]"
  ([](:db (mg/connect-via-uri (str "mongodb://sls:666666@localhost/sls")))))
(defn get-product-list []
  (let [db (connect-db)
        coll "products"]
    (mc/find-maps db coll)))

(defn get-product-list-memo
  ([] ((memoize get-product-list))))


(render "{{title}}" {:title (render-file "insert.html" {})})

(defn home
  ([req]
   (pprint (str (:body req)))
   (render-file "home-ext.html"
     {:title "Home"
      :product-list-header ["image" "name" "count" "size" "label"  "color"  "" ""]
      :product-list (get-product-list)
      :nav-items [{:label "Home" :url "/"}
                  {:label "New Product" :url "/create-product"}
                  {:label "About" :url "/about"}]
      })
   ))




(defn create-product
  ([req]
   (render-file "insert.html" {:response (str
      (let [db (connect-db)]
        ;; (mg/authenticate db u p)
        (mc/insert-and-return db "products" (map-keywords (:params req)))
        ))})))

(defn delete-product
  ([req]
   (response
     (str
       (let [id (ObjectId. (:_id (map-keywords (:params req))))]
         [(mc/remove-by-id (connect-db) "products" id) id]
         )))))

;; (mc/update db coll {:count {"$gt" 0}} {:count 20 :owner "高洪"} {:upsert true})

(defroutes main-routes
  (route/files "/")
  (POST "/create-product" {params :params cookies :cookies} create-product)
  (POST "/delete-product" {params :params} delete-product)
  (GET "/" [req] home)
  (GET "/new-product" [req] (render-file "new-product.html" {:title (json/write-str req)}))
  (GET "/about" [req] (response "This is a private system."))
  (GET "/create-product" [req] (fn [req] (render-file "insert.html" {:title "Template insert page."})))
  (route/not-found "Page not found"))


;; (:body (client/post "http://localhost:18080/insert" {:name "hello"}))
;; (client/get "http://localhost:18080/insert" {:pname "hello" :pimage "img"})
;; (-main)

(def app (-> #'main-routes wrap-params wrap-cookies wrap-keyword-params))

;; (defn handler [req]
;;   (->
;;    ;; (response (reduce str (post-page sample-post)))
;;    ;; (file-response "readme.html" {:root "public"})
;;    ;; (response "hello")
;;    (response (main-page req))
;;    (content-type "text/html; charset=UTF-8")))

(defn launch []
  (run-jetty app {:port 18080 :join? false :route "public"}))

(defn -main
  "启动web应用程序"
  ^{:static false
    :dynamic true}
  [& args]
  (def server (launch)))

;; sudo mongod

(let [conn (mg/connect)
      ;; mongodb://[username:password@]host1[:port1][,host2[:port2],…[,hostN[:portN]]][/[database][?options]]
      db   (mg/connect-via-uri (str "mongodb://sls:666666@localhost/sls"))
      coll "products"]
  ;; (mg/authenticate db u p)
  ;; (mc/insert-and-return db "products" {:name "ksj" :color "黄" :count 50 :size 40 :label "default"})
  ;; (mc/find db coll {:count {"$gt" 10 "$lt" 99}})
  ;; (mc/remove db coll {:count {"$lt" 20}})
  ;; (mc/remove db coll)
  ;; (mc/update db coll {:count {"$gt" 0}} {:count 20 :owner "高洪"} {:upsert true})
  ;; (pprint (mc/find-maps db coll))
  )

;; (str (as-file (from "http://10.1.201.72:18080")))
;; /var/folders/j0/v760t6w94d50bql8hlh3rxfc0000gn/T/QRCode5030151942762847936.png
;; D:\MyConfiguration\lzy13870\AppData\Local\Temp\QRCode1100735308893157262.png

;; (defn url-attack [] "http://www.baidu.com/baidu.php?url=ssRK00jnh8orQ_g9YOLug-S1m1Sqa7NOHE5osXkcszJquhfwOT5FcREBamf9qfcKjyXkDjl0b7apsAgZY9oBZb65YK7bznpZW8qUHcHF3ccH74QOscntp1_6AJgKa4pKB9IYsUf.7D_iHF8xnhA94wEYL_SNK-deQbfHgI3ynDgg6msw5I7AMHdd_NR2A5jkq8ZFqTrHllgw_E9tGbSNK-deQbmTMdWi1PjNz8smX5dxAS2FnvZWtonrHGEsfq8QjkSyHjblubltXQjkSyMHz4rMG34nheuztIdMugbzTEZF83e5ZGzmTMHvGYTjGo_5Z4mThedlTrHIt5s3The3IhZF8qISZFY3tyZWtVrM-zI5HkzuPv1-3eorzEFb4XrHIkvX5HblqoAVPXzOk8_eAThqPvlZoWmYlXgFYq5ZFbLUrgW8_e2thH-34PLZu3qrHoXkvyNq-----xHEer1IvUdPHV2XgZJyAp7WFYvyu70.U1Yk0ZDqV_1c8fKY5UUnzQb0pyYqnW0Y0ATqmhwln0KdpHdBmy-bIfKspyfqnHb0mv-b5HR40AdY5HDsnHIxn10sn-tknjD1g1nsnW00pvbqn0KzIjY3njT0uy-b5HD3rj6sg1DYPH7xnH6zPj7xnHbdPH9xnH6kn1PxnHTsnj7xnHRYrj9xnHD4nHT0mhbqnW0Yg1DdPfKVm1Y3rjc4n103Pdtknj7xnHnvrjnsPHcvndts0Z7spyfqn0Kkmv-b5H00ThIYmyTqn0KEIhsq0A7B5HKxn0K-ThTqn0KsTjYknjRsnW03PWTv0A4vTjYsQW0snj0snj0s0AdYTjYs0AwbUL0qn0KzpWYs0Aw-IWdsmsKhIjYs0ZKC5H00ULnqn0KBI1YknfK8IjYs0ZPl5fKYIgnqnHc1PjR3n1R1n1m4P1nzP10zrHR0ThNkIjYkPjR4P1fLP16LrHfv0ZPGujdBuj9WPjDLPH0snWnvuHR40AP1UHYkwjm3n1wDnj0znYD1wW-j0A7W5HD0TA3qn0KkUgfqn0KkUgnqn0KlIjYs0AwYpyfqn0K9TLKWm1Ys0ZNspy4Wm1Ys0APzm1YdP1bdP6&us=0.0.0.0.0.0.0&us=0.0.0.0.0.0.13&ck=6203.20.1459747842434.0.0.516.212.0&shh=www.baidu.com&sht=baiduhome_pg")
;; (future (client/get (url-attack)))

;; (-main)
(println "loaded.")
