(ns website.sls
  (:use [ring.adapter.jetty]
        [ring.util.response]
        [website.encrypto]
        [clojure.pprint]
        [clj.qrgen]
        [net.cgrand.enlive-html]
        [ring.middleware.session         :refer [wrap-session]]
        [ring.middleware.params         :refer [wrap-params]]
        [ring.middleware.cookies        :refer [wrap-cookies]]
        [ring.middleware.keyword-params :refer [wrap-keyword-params]]
        [selmer.parser])
  
  (:import
   [javax.crypto Cipher KeyGenerator SecretKey]
   [javax.crypto.spec SecretKeySpec]
   [java.security SecureRandom]
   [org.apache.commons.codec.binary Base64]
   [com.mongodb MongoOptions ServerAddress]
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
