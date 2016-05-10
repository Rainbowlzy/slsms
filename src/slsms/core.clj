(ns slsms.core
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use [ring.adapter.jetty]
        [ring.util.response]
        [website.encrypto]
        [clojure.pprint]
        [clojure.java.io :refer [copy delete-file file]]
        [clj.qrgen]
        ;; [net.cgrand.enlive-html :only [html-content]]
        [ring.middleware.multipart-params]
        [ring.middleware.session         :refer [wrap-session]]
        [ring.middleware.params         :refer [wrap-params]]
        [ring.middleware.cookies        :refer [wrap-cookies]]
        [ring.middleware.keyword-params :refer [wrap-keyword-params]]
        [hiccup.form :as form]
        [hiccup.core :refer [html]]
        [selmer.parser])
  (:import
   [javax.crypto Cipher KeyGenerator SecretKey]
   [javax.crypto.spec SecretKeySpec]
   [java.security SecureRandom]
   [org.apache.commons.codec.binary Base64]
   [com.mongodb MongoOptions ServerAddress]
   [org.bson.types ObjectId]
   [com.mongodb DB WriteConcern])
  (:require
   ;; [net.cgrand.enlive-html :as html]
   [clojure.data.json :as json]
   [clojure.string :as string]
   [clj-http.client :as client]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [compojure.core :refer [GET POST defroutes]]
   [monger.core :as mg]
   [monger.collection :as mc]
   ;; [clojure.contrib [duck-streams :as ds]]
   [selmer.parser :refer [render-file]]))

(def public-absolute-path (.getAbsolutePath (clojure.java.io/file "./public")))

(defn save-image
  ([tempfile image-abs-path]
   (copy (file tempfile) (file image-abs-path) :encoding "ASCII")
   (delete-file tempfile)
   image-abs-path))

(defn gen-image-path
  ([] (gen-image-path (str (ObjectId.)) ".jpg"))
  ([id] (gen-image-path ".jpg"))
  ([id extension]
   (let [product-id (ObjectId. id)
         image-path (str "/image-upload/product-image/" product-id extension)]
     {:product-id (ObjectId. id)
      :image-path image-path
      :image-abs-path (str public-absolute-path image-path)})))

(defn map-kv
  ([my-map]
   (into {} 
         (for [[k v] my-map] 
           [(keyword k) v]))))

(defn global-response [resp]
  (-> resp response (content-type "text/html; charset=UTF-8")))

(defn connect-db
  "mongodb://[username:password@]host1[:port1][,host2[:port2],…[,hostN[:portN]]][/[database][?options]]"
  ([] (:db (mg/connect-via-uri (str "mongodb://sls:666666@localhost/sls")))))

(defn get-product-list []
  (let [db (connect-db)
        coll "products"]
    (mc/find-maps db coll)))

(defn get-product-list-memo
  ([] ((memoize get-product-list))))

(defn home
  ([req]
   (render-file "home-ext.html"
                {:title "Home"
                 :user (str "Current User : " (-> req :session :user :username str))
                 :product-list-header ["image" "name" "count" "size" "label"  "color"  "" ""]
                 :product-list (get-product-list)
                 :nav-items [{:label "Home" :url "/"}
                             {:label "Login" :url "/login"}
                             {:label "New Product" :url "/create-product"}
                             {:label "About" :url "/about"}]
                 })
   ))

(defn update-product
  ([req]
   (let [{params :params} req
         {image :image} params
         {filename :filename}  params
         {tempfile :tempfile} image
         image-path-obj (gen-image-path (:_id params) (re-find #"\.\w+$" (str filename)))
         {product-id :product-id} image-path-obj
         {image-path :image-path} image-path-obj
         {image-abs-path :image-abs-path} image-path-obj
         id (:_id params)
         prod (assoc (dissoc params :_id :image) :image image-path)]
     (save-image tempfile image-abs-path)
     (mc/update-by-id (connect-db) "products" (ObjectId. id) prod {:upsert true})
     (redirect (str "/product-detail/" id))
     )))

(defn wrap-db-insert
  ([user ent]
   (let [u (:username user)
         d (java.util.Date.)]
     (conj ent {:created-by u
                :modified-by u
                :created-date d
                :modified-date d}))))

;; (mc/insert-and-return (connect-db) "products"
;;   (wrap-db-insert {:username "admin"}
;;     {:_id (ObjectId.)
;;      :image ""
;;      :qrcode ""
;;      }))

(defn create-product
  ([req]
   (let [{params :params} req
         {session :session} req
         {user :user} session
         {image :image} params
         {filename :filename} image
         {tempfile :tempfile} image
         image-path-obj (gen-image-path
                          (str (ObjectId.))
                          (re-find #"\.\w+$" (str filename)))
         {product-id :product-id} image-path-obj
         {image-path :image-path} image-path-obj
         {image-abs-path :image-abs-path} image-path-obj
         qrcode (save-image
                  (as-file
                    (from
                      "http://localhost:18080/product-detail/"
                      product-id))
                  (str public-absolute-path "/qrcode/" product-id ".jpg"))]
     
     (save-image tempfile image-abs-path)
     (let [prod (assoc (:params req) :_id product-id :image image-path :qrcode qrcode)
           db (connect-db)]
       (render-file
        "insert.html"
        {:title "Create Success!"
         :action "/create-product"
         :prod (mc/insert-and-return db "products" (wrap-db-insert user prod))})))))

(defn delete-product
  ([req]
   (response
    (str
     (let [id (ObjectId. (:_id (:params req)))]
       [(mc/remove-by-id (connect-db) "products" id) id]
       )))))

(defn query-product-by-id
  ([id] (map-kv (mc/find-by-id (connect-db) "products" (ObjectId. id)))))

(defn product-detail
  ([req]
   (let [{params :params} req]
     (let [{id :id} params
           prod (query-product-by-id id)]
       (render-file
        "insert.html"
        {:title (:name prod)
         :action "/update-product"
         :prod prod})))))




(defn login
  ([req]
   (let [{cookies :cookies} req
         {params :params} req
         {session :session} req
         {username :username} params
         {password :password} params]
     (let [user (mc/find-one-as-map (connect-db) "users" {:username (str username) :password (str (encry password))} [:username])
           ent (assoc session :user user)]
       (println (mc/find-one-as-map (connect-db) "users" {:username (str "admin")
                                                          :password (str (encry "admin"))
                                                          }))
       {:body
        (html
         [:script "window.location.href='/';"])
        :session ent}))))

(defn show-login-page
  ([req] (render-file "login.html" {:title "Sign In"
                                    :prod {:_id (ObjectId.)
                                           :username "admin"
                                           :password "admin"}})))

(defn wrap-exception
  ([handler]
   (try
     (fn [request]
       (try (handler request)
            (catch Exception exception (println-str exception))))
     (catch Exception exception (println-str exception)))))

(defn wrap-permission
  ([handler]
   (fn [request]
     (let [{cookies :cookies} request
           {params :params} request
           {session :session} request]
       (if (or session (= (count (:user session)) 0))
         (show-login-page request)
         (handler (assoc request :user (first (:user session)))))
       ))))

(defroutes main-routes
  (route/files "/")
  (GET "/create-product" [req] (fn [req] (render-file "insert.html" {:title "Create A New Product in This Page."})))
  (GET "/login" [req] show-login-page)
  (GET "/product-detail/:id" [id] product-detail)
  (GET "/" [req] home)
  (GET "/new-product" [req] (render-file "new-product.html" {:title (json/write-str req)}))
  (GET "/about" [req] (response "This is a private system."))
  (POST "/create-product" [req] create-product)
  (POST "/delete-product" [req] delete-product)
  (POST "/update-product" [req] update-product)
  (POST "/login" [req] login)
  (POST "/new-product" {params :params} (response (str "post as params" params)))
  (route/not-found "Page not found"))


;; (:body (client/post "http://localhost:18080/insert" {:name "hello"}))
;; (client/get "http://localhost:18080/insert" {:pname "hello" :pimage "img"})
;; (-main)

(defn wrap-pprintln
  ([handler]
   (fn [request]
     (pprint (type request))
     (let [rsp (handler request)]
       rsp))))

(def app (-> #'main-routes
             wrap-keyword-params
             wrap-params
             wrap-cookies
             wrap-session
             wrap-multipart-params
             wrap-exception
             ))


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
;; (defonce server (launch))
;; sudo mongod

;; (str (as-file (from "http://10.1.201.72:18080")))
;; /var/folders/j0/v760t6w94d50bql8hlh3rxfc0000gn/T/QRCode5030151942762847936.png
;; D:\MyConfiguration\lzy13870\AppData\Local\Temp\QRCode1100735308893157262.png

;; (defn url-attack [] "http://www.baidu.com/baidu.php?url=ssRK00jnh8orQ_g9YOLug-S1m1Sqa7NOHE5osXkcszJquhfwOT5FcREBamf9qfcKjyXkDjl0b7apsAgZY9oBZb65YK7bznpZW8qUHcHF3ccH74QOscntp1_6AJgKa4pKB9IYsUf.7D_iHF8xnhA94wEYL_SNK-deQbfHgI3ynDgg6msw5I7AMHdd_NR2A5jkq8ZFqTrHllgw_E9tGbSNK-deQbmTMdWi1PjNz8smX5dxAS2FnvZWtonrHGEsfq8QjkSyHjblubltXQjkSyMHz4rMG34nheuztIdMugbzTEZF83e5ZGzmTMHvGYTjGo_5Z4mThedlTrHIt5s3The3IhZF8qISZFY3tyZWtVrM-zI5HkzuPv1-3eorzEFb4XrHIkvX5HblqoAVPXzOk8_eAThqPvlZoWmYlXgFYq5ZFbLUrgW8_e2thH-34PLZu3qrHoXkvyNq-----xHEer1IvUdPHV2XgZJyAp7WFYvyu70.U1Yk0ZDqV_1c8fKY5UUnzQb0pyYqnW0Y0ATqmhwln0KdpHdBmy-bIfKspyfqnHb0mv-b5HR40AdY5HDsnHIxn10sn-tknjD1g1nsnW00pvbqn0KzIjY3njT0uy-b5HD3rj6sg1DYPH7xnH6zPj7xnHbdPH9xnH6kn1PxnHTsnj7xnHRYrj9xnHD4nHT0mhbqnW0Yg1DdPfKVm1Y3rjc4n103Pdtknj7xnHnvrjnsPHcvndts0Z7spyfqn0Kkmv-b5H00ThIYmyTqn0KEIhsq0A7B5HKxn0K-ThTqn0KsTjYknjRsnW03PWTv0A4vTjYsQW0snj0snj0s0AdYTjYs0AwbUL0qn0KzpWYs0Aw-IWdsmsKhIjYs0ZKC5H00ULnqn0KBI1YknfK8IjYs0ZPl5fKYIgnqnHc1PjR3n1R1n1m4P1nzP10zrHR0ThNkIjYkPjR4P1fLP16LrHfv0ZPGujdBuj9WPjDLPH0snWnvuHR40AP1UHYkwjm3n1wDnj0znYD1wW-j0A7W5HD0TA3qn0KkUgfqn0KkUgnqn0KlIjYs0AwYpyfqn0K9TLKWm1Ys0ZNspy4Wm1Ys0APzm1YdP1bdP6&us=0.0.0.0.0.0.0&us=0.0.0.0.0.0.13&ck=6203.20.1459747842434.0.0.516.212.0&shh=www.baidu.com&sht=baiduhome_pg")
;; (future (client/get (url-attack)))

;; (-main)
(println "loaded.")
