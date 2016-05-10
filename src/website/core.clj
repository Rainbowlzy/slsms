(ns website.core
  (:use [ring.adapter.jetty]
        [ring.util.response]
        [website.encrypto :refer [encry decry]]
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
   [java.net InetAddress]
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
(def local-port 18080)

(defn save-image
  ([tempfile image-abs-path]
   (copy (file tempfile) (file image-abs-path) :encoding "ASCII")
   (delete-file tempfile)))

(defn gen-image-path
  ([id] (gen-image-path ".jpg"))
  ([id extension]
   (let [product-id (ObjectId. id)
         image-path (str "/image-upload/product-image/" product-id extension)]
     {:product-id (ObjectId. id)
      :image-path image-path
      :image-abs-path (str (.getAbsolutePath(java.io.File. ".")) "/public" image-path)}))
  ([] (gen-image-path (str (ObjectId.)) ".jpg")))


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
                  ;; :user (str "Current User : " (-> req :session :user :username str))
                  :product-list-header ["image" "qrcode" "name" "count" "size" "label"  "color"]
                  :product-list (get-product-list)
                  :nav-items [{:label "Home" :url "/"}
                              {:label "Login" :url "/login"}
                              {:label "New Product" :url "/create-product"}
                              {:label "About" :url "/about"}]})))

(defn update-product
  ([req]
   (let []
     (let [{params :params} req
           {image :image}params
           {filename :filename} params
           {tempfile :tempfile} image
           image-path-obj (gen-image-path (:_id params) (re-find #"\.\w+$" (str filename)))
           {product-id :product-id}image-path-obj
           {image-path :image-path}image-path-obj
           {image-abs-path :image-abs-path}image-path-obj]
       (let [id (:_id params)
             prod (assoc (dissoc params :_id :image) :image image-path)]
         (save-image tempfile image-abs-path)
         (mc/update-by-id (connect-db) "products" (ObjectId. id) prod {:upsert true})
         (redirect "/"))))))

(defn create-product
  ([req]
   (let [{params :params} req
         {image :image} params
         {filename :filename} image
         {tempfile :tempfile} image
         id (str (ObjectId.))
         image-path-obj (gen-image-path id (re-find #"\.\w+$" (str filename)))
         {product-id :product-id} image-path-obj
         {image-path :image-path} image-path-obj
         {image-abs-path :image-abs-path} image-path-obj
         host (.getHostAddress (InetAddress/getLocalHost))
         uri (str "http://" host ":" local-port "/product-detail/" id)
         qrcode-image-url (str "http://" host ":" local-port "/image-upload/qrcode/" id ".png")
         ]
     (save-image (as-file (from uri)) (str (.getAbsolutePath (java.io.File. ".")) "/public/image-upload/qrcode/" id ".png"))     
     (save-image tempfile image-abs-path)
     (let [prod (assoc (:params req) :_id product-id :image image-path :qrcode qrcode-image-url)
           db (connect-db)]
       (render-file
        "insert.html"
        {:title "Create Success!"
         :action "/create-product"
         :prod (mc/insert-and-return db "products" prod)})))))

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
  (GET "/" [req] home)
  (GET "/create-product" [req] (fn [req] (render-file "insert.html" {:title "Create A New Product in This Page."})))
  (GET "/login" [req] show-login-page)
  (GET "/product-detail/:id" [id] product-detail)
  (GET "/new-product" [req] (render-file "new-product.html" {:title (json/write-str req)}))
  (GET "/about" [req] (response "This is a private system."))
  (POST "/create-product" [req] create-product)
  (POST "/delete-product" [req] delete-product)
  (POST "/update-product" [req] update-product)
  (POST "/login" [req] login)
  (POST "/new-product" {params :params} (response (str "post as params" params)))
  (route/not-found "Page not found"))

(defn wrap-pprintln
  ([handler]
   (fn [request]
     (pprint request)
     (let [rsp (handler request)]
       (pprint rsp)
       rsp))))

(def app (-> #'main-routes
             wrap-keyword-params
             wrap-params
             wrap-cookies
             wrap-session
             wrap-multipart-params
             wrap-pprintln
             ))

(defn launch []
  (run-jetty app {:port local-port :join? false}))

(defn -main
  ^{:static false
    :dynamic true}
  [& args]
  (def server (launch)))

(defonce server (launch))












