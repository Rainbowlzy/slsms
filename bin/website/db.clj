(ns website.db
  (:use [clojure.java.jdbc])
  (:require [clojure.string :as string]
         [clojure.java.jdbc :as jdbc]
         [website.encrypto]
         [java-jdbc.ddl :as ddl]))



(def connection-str "" (website.encrypto/decrypt-with-hello "O5OWjjeLbcFGHallhgSHC1ejmdydziSJL9yVzCst8YE4cDCj1OUxySAcD26829wCFkQ4VorJQRcre6XSiEZSjALo2K3J740LI0td4eEvYgrgY8wewOKrWd076Gui6IR57JKI7WZyhsMVh9kq8RnvGsPStuasnL2vAYxSAx0lVxvBILj/CQzUvnuTAF1Pik8l"))

(for [pair (string/split connection-str #";")]
  (reduce #(hash-map (keyword (string/lower-case (string/replace %1 #"\s" "-"))) %2) (string/split pair #"=")))

(def db {:classname "sun.jdbc.odbc.JdbcOdbcDriver"
         :subprotocol "odbc"
         :subname (str
                   "Driver={Microsoft Excel Driver (*.xlsx)};DBQ="
                   "/Users/apple/Downloads/数据报表_20160329143013209.xlsx"
                   "DriverID=22;READONLY=false"
                   )}) ;; <- Put the DSN name here

(jdbc/query db ["select * from Sheet1"])



(jdbc/db-do-commands db-spec false
                     (ddl/create-table
                      :tags
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]))

(jdbc/query db-spec (sql/select * :table (sql/where {:name "Clojure"})))




