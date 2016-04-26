(defproject website "0.1.0-SNAPSHOT"
  :main website.sls
  :encoding "UTF-8"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [
            ;; [lein-swank "1.4.5"]
            ;; [cider/cider-nrepl "0.11.0"]
            [cider/cider-nrepl "0.12.0-SNAPSHOT"]
            [refactor-nrepl "2.3.0-SNAPSHOT"]
            ;; [refactor-nrepl "2.3.0-SNAPSHOT"]
            ]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [enlive "1.1.6"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-core "1.4.0"]
                 [clj-http "0.3.1"]
                 [clj-time "0.6.0"]
                 [http.async.client "0.4.1"]
                 [dk.ative/docjure "1.10.0"]
                 [com.cemerick/pomegranate "0.3.0"]
                 [clj-sockets "0.1.0"]
                 [korma "0.3.0"]
                 [net.mikera/core.matrix "0.36.1"]
                 [net.mikera/vectorz-clj "0.29.0"]
                 [laczoka/clj.qrgen "0.1.2"]
                 [yieldbot/vizard "0.1.0"]
                 [com.microsoft.sqlserver/sqljdbc4 "4.0"]
                 [org.formcept/sqljdbc4 "4.0"]
                 [com.draines/postal "1.11.3"]
                 [java-jdbc/dsl "0.1.0"]
                 ;; [adzerk/boot-cljs-repl   "0.3.0"]
                 [com.cemerick/piggieback "0.2.1"]
                 [figwheel-sidecar "0.5.0-2"]
                 [com.cemerick/piggieback "0.2.1"  :scope "test"]
                 [weasel                  "0.7.0"  :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [org.clojure/clojurescript "0.0-2760"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [com.novemberain/monger "3.0.2"]
                 [org.omcljs/om "0.8.8"]
                 ;; [org.clojure/clojure-contrib "1.2.0-beta1"]
                 [racehub/om-bootstrap "0.5.0"]
                 [ring/ring-codec "1.0.0"]
                 [compojure "1.5.0"]
                 [selmer "1.0.2"]])

