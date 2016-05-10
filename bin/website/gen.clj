(ns website.gen)

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

(def branchs-path "/Volumes/U/branchs/")
;; (def data (clojure.xml/parse (str branchs-path "ringfullsite/src/website/tmp1.xml")))

(defn keyword-to-str [x] (if (and (= (.substring (str x) 0 1) ":") (keyword? x)) (.substring (str x) 1)))
(defn gen[x] (keyword-to-str x))
(defn gen-cs-field-name [name]
  (when name (str "_" (.toLowerCase (.substring (str name) 0 1)) (.substring (str name) 1 (.length (str name))))))

(defn gen-cs-field [cs-name cs-type]
  (str "private " cs-type " " (gen-cs-field-name cs-name) ";"))
(defn gen-first-upper [cs-name] (when cs-name (str (.toUpperCase (.substring (str cs-name) 0 1)) (.substring (str cs-name) 1 (.length cs-name)))))

(defn gen-cs-prop [cs-name cs-type]
  (let [field-name (gen-cs-field-name cs-name)]
    (str "\n" (gen-cs-field cs-name cs-type)
         "\npublic " cs-type " " cs-name " { get { return " field-name " ; } set { " field-name " = value ; } }")))

(defn gen-cs-class [cs-name cs-body]
  (str
   ;; "\n[XmlRoot(ElementName = \"" cs-name "\")]"
   "\npublic class " cs-name " { " cs-body " } "))

(defn test-impl [x] (cond (string? x)  "It's a string."
                          (map? x)  "It's a map."
                          (vector? x)  "It's a vector."
                          (keyword? x)  "It's a keyword."))
(defn str-left [x len] (when x (cond (> (.length (str x)) len) (.substring (str x) 0 len) :else (str x))))

(def ^{:static true} list-prop? #(= (count (for [node (content %)] node)) (count (for [node (content %) :while (map? node)] node))))
(def ^{:static true} str-prop? #(and (map? %) (vector? (content %)) (string? ((content %) 0))))

(defn test [x] (when x (str "\n" (test-impl x) " " (str-left x 80) "\n")))
(defn gen [x]
  (cond
    (string? x) x
    (keyword? x) (keyword-to-str x)
    (map? x) (cond
               (list-prop? x) (str (gen-cs-prop (gen (tag x)) (str (gen (tag ((content x) 0))) "[]"))
                                   (gen-cs-class (gen (tag ((content x) 0))) (gen (content x))))
               (str-prop? x) (str (gen-cs-prop (gen (tag x)) "string")))
    (vector? x) (let [h (x 0)]
                  (cond (map? h) (clojure.string/join (for [o x :while o] (gen o)))
                        :else (print-str x)))))

;; (defn gen-entry[xml-data]
;;   (str "\nusing System;\nusing System.Collections.Generic;\nusing System.Linq;\nusing System.Text;\nusing System.Threading.Tasks;\nusing System.Xml.Serialization;\nnamespace Xml.Output.Entities\n{\n\tpublic class Entry{\n"
;;        (gen xml-data)
;;        "\n\t}\n}"))

(def xml-path "/Volumes/U/branchs/ringfullsite/src/website/test")
(with-open [wrtr (writer xml-path)] (.write wrtr (gen data)))
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

(json/write-str {:request {:header {:clientInfo {:deviceId "abc"}}
                           :body {:memberId "asdflkj;lkajsdfalskdjf"}}})









