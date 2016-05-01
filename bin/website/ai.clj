;; Copyright (c) Stuart Sierra, 2012. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns website.ai
  (:use [clojure.pprint]))
(require '[clojure.core.matrix :as m]
         '[clojure.core.matrix.protocols :as mp]
         '[clojure.java.shell :only [sh]]
         '[vizard [core :refer :all] [plot :as plot]])
(import '[java.util Random])
(use 'clojure.core.matrix)
(use 'clj.qrgen)
(use 'clojure.pprint)
(defn random []
  (reduce + (random-sample 0.5 (range 9))))
(defn sample-gaussian [n]
  (let [rng (Random.)]
    (repeatedly n #(.nextGaussian rng))))
(defn sigmod [v]
  (Math/pow (+ 1 (Math/exp (* -0.8 v))) -1))
(sample-gaussian 9)
(defn init [coll]
  (for [v (range (count coll))] (random)))
(defn fake [num]
  (for [v (clojure.string/split (Integer/toBinaryString num) #"")] (Integer. v)))
(defn ai-training-sample-maker[]
  (for [v (range 1000 9999)]
    [(fake v) [(if (> v 5000) 4 5)]])
  )
(defn ai-plus[a])

(def data (atom (ai-training-sample-maker)))
(def datum (first @data))
(def input (atom []))
(def x (atom []))
(def xh (atom (init datum)))
(def xhb (atom (init datum)))
(def h (atom []))
(def hy (atom (init datum)))
(def hyb (atom (init datum)))
(def y (atom []))
(def output (atom []))
(def step (atom 0.8))
(defn cal-next-layout [coll coll-h coll-b]
  (for [i (range (count coll))]
    (sigmod
     (+ (* (nth coll i 1) (nth coll-h i 1)) (nth coll-b i 0)))))
(let [datum (first @data)
      input (first datum)
      output (second datum)
      x input
      h (cal-next-layout x @xh @xhb)
      y (cal-next-layout h @hy @hyb)
      delta (for [i (range (count output))] (+ (nth @hy i 0) (* @step (- (nth output i 0) (nth y i 0)) (nth x i 0))))
      errors (for [i (range (count output))] (- (nth output i 0) (nth y i 0)))
      ]
  y
  )
(defn train
  "Training"
  ([datum]
   (let [input (first datum)
         output (second datum)
         x input
         h (cal-next-layout x @xh @xhb)
         y (cal-next-layout h @hy @hyb)
         delta (for [i (range (count output))] (+ (nth @hy i 0) (* @step (- (nth output i 0) (nth y i 0)) (nth x i 0))))
         errors (for [i (range (count output))] (- (nth output i 0) (nth y i 0)))
         ]
     (def xh (atom (for [v delta] (sigmod v))))
     (def hy (atom delta))
     errors)))
;; (last (for [d @data] (train d)))
(.getAbsolutePath (as-file (from "http://m.ly.com/")))
[
 (for [v (range 5 10)] (inc v))
 (for [v (range 5 10)] (dec v))
 ]
(for [n (for [v (range 5 10)] (dec v)) :when (= (mod n 2) 0)] n)
(* (+ 1 1) 2)
(defn f [a b]
  (println (str a " " b))
  (+ a b))
(f 1 2)
(type (Integer. 9))
(type (vector 1 3 4))
(conj [1 2 3] [4  5 6])
(into [1 2 3] [4  5 6])
(reduce into [[1 2 3] [4  5 6]])
(sort [3 4 5 1 2 4])
(def m {:a 1 :b 2 :c 3})
(:a m)
(:b m)
(clojure.string/join "," (clojure.string/split (str 1 2 3 \a \b) #""))
'(1 2 3 4)
[1 2 3 4]
(for [v (clojure.string/split (Integer/toBinaryString 56) #"")] (Integer. v))
(.toString (java.util.Date.))
(.getAbsolutePath (java.io.File. "~/branchs/ringfullsite/src/website/ai.clj"))
