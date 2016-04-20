;; Copyright (c) Stuart Sierra, 2012. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.


(ns website.core
  (:use ring.adapter.jetty)
  (:use ring.util.response))
;; (:use ring.middleware.reload)
;; (:use ring.middleware.stacktrace)
;; (:use [compojure.core :only (GET PUT POST defroutes)])
;; (:require (compojure handler route)
;;           [ring.util.response :as response])
;; (:require [shortener.db :as db]))

(defn handler [req]
  (->
   ;; (file-response "readme.html" {:root "public"})
   (response "hello")
   ;; (content-type "text/plain")
   ))




(defn boot []
  (run-jetty handler {:port 8080}))

;; (boot)



;; (ns website.gen)

(require '[clojure.data.json :as json])
(require '[clj-http.client :as client])
(require '[clj-http.cookies :as cookies])

(use '[clojure.java.io])
(use '[clojure.java.shell :only [sh]])
(use '[clojure.pprint :only [pprint]])
(use '[clojure.string :only [join]])
(use '[clojure.xml])
;; (use '[clojure.data.xml])
;; (use '[clojure.contrib.trace])

(def co #{})
(defn set-value[n]
  (when (not (contains? co n))
    (def co (conj co n))
    co))

(def branchs-path "/Volumes/U/branchs/")
(def data (clojure.xml/parse (str branchs-path "ringfullsite/src/website/tmp.xml")))

(defn append-file[path data]
  (with-open [wrtr (writer path :append true)]
    (.write wrtr data)))
(defn write-file[path data]
  (with-open [wrtr (writer path)]
    (.write wrtr data)))
(defn gen-cs-field-name [cs-name]
  (when (name cs-name)
    (str "_"
         (.toLowerCase (.substring (str (name cs-name)) 0 1))
         (.substring (str (name cs-name)) 1 (.length (str (name cs-name)))))))

(defn gen-cs-field [cs-name cs-type]
  (str "private " cs-type " " (gen-cs-field-name (name cs-name)) ";"))
(defn gen-first-upper [cs-name]
  (when cs-name (str (.toUpperCase (.substring (str (name cs-name)) 0 1)) (.substring (str (name  cs-name)) 1 (.length (name cs-name))))))
(defn gen-cs-prop [cs-name cs-type]
  (let [field-name (gen-cs-field-name cs-name)]
    (str "\n" (gen-cs-field cs-name cs-type)
         "\npublic " cs-type " " (name cs-name) " { get { return " field-name "; } set { " field-name " = value; } }")))
(def cs-clazzs #{})

(defn gen-cs-class [cs-name cs-body]
  (let[]
    (str "\npublic class " (name cs-name) " { " cs-body " } ")))

(defn str-left [x len] (when x (cond (> (.length (str x)) len) (.substring (str x) 0 len) :else (str x))))
(def ^{:static true} list-prop?
  #(let[]
     (and
      (map? %)
      (vector? (content %))
      (map? ((content %) 0))
      (=
       (count
        (for [node (content %)]
          node))
       (count
        (for [node (content %)
              :while (and
                      (map? node)
                      (= (tag node) (tag ((content %) 0))))]
          node))))))

(def ^{:static true} str-prop? #(and (map? %) (vector? (content %)) (string? ((content %) 0))))
(defn test-impl [x] (cond (string? x)  "It's a string."
                          (map? x)  (cond (list-prop? x) "It's a list property."
                                          (str-prop? x) "It's a string property."
                                          )
                          (vector? x)  "It's a vector."
                          (keyword? x)  "It's a keyword."
                          (nil? x) "It's nil."))

(defn test [x] (when x (str "\n" (test-impl x) " " (str-left x 80) "\n")))
(defn log[s] (append-file "/Volumes/U/branchs/ringfullsite/src/website/log" (str "\n" s)))
(write-file "/Volumes/U/branchs/ringfullsite/src/website/log" "")


(defn gen [x & p]
  (log (str (test x)))
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (cond
               (list-prop? x) (str
                               (gen-cs-prop
                                (tag x)
                                (str (name (tag ((content x) 0))) "[]"))
                               (gen (content x) (tag x))
                               ;; (gen-cs-class
                               ;;  (tag ((content x) 0))
                               ;;  (gen (content x)))
                               )
               (str-prop? x) (when (set-value (str (tag x) p))
                               (str (gen-cs-prop (name (tag x)) "string")))
               :else (cond (set-value (str (tag x) p))
                           (let[]
                             (gen-cs-class
                              (tag x)
                              (gen (content x) (tag x))))
                           :else (gen (content x) (tag x))))
    (vector? x) (and
                 (> (count x) 0)
                 (map? (x 0))
                 (join (for [o x] (gen o p))))))

;; (defn gen-entry[xml-data]
;;   (str "\nusing System;\nusing System.Collections.Generic;\nusing System.Linq;\nusing System.Text;\nusing System.Threading.Tasks;\nusing System.Xml.Serialization;\nnamespace Xml.Output.Entities\n{\n\tpublic class Entry{\n"
;;        (gen xml-data)
;;        "\n\t}\n}"))

(def xml-path "/Volumes/U/branchs/ringfullsite/src/website/test")

(write-file xml-path (gen data))

;; (pprint data)
;; (pprint (gen-entry data))

;; (def xml-data (parse xml-path))
;; (print
;;  (str "\n"
;;       (join "\n"
;;             (for [node (content xml-data)
;;                   :while (map? node)]
;;               (content node)))))
;; (def lst ((content xml-data) 0))






