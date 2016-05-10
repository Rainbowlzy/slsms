(ns website.gaussian)

(require '[clojure.core.matrix :as m]
         '[clojure.core.matrix.protocols :as mp]
         '[vizard [core :refer :all] [plot :as plot]])
(import '[java.util Random])

(m/set-current-implementation :vectorz)

(defn sample-gaussian [n]
  (let [rng (Random.)]
    (repeatedly n #(.nextGaussian rng))))

(defn sample-multivariate-gaussian [mean cov]
  (let [n (count mean)
        e (m/scale (m/identity-matrix n) 1e-8)
        L (:L (mp/cholesky (m/add (m/matrix cov) e)
                           {:results [:L]}))
        u (m/matrix (sample-gaussian n))
        samples (m/add (m/matrix mean)
                       (m/mmul L u))]
    (m/to-nested-vectors samples)))

(def test-xs (range -5 5 0.03))

(defn squared-exponential [sigma2 lambda x y]
  (* sigma2 (Math/exp (* -0.5 (Math/pow (/ (- x y) lambda) 2)))))

(defn covariance-mat [f xs ys]
  (let [rows (count xs)
        cols (count ys)]
    (partition cols
               (for [i (range rows) j (range cols)]
                 (f (nth xs i) (nth ys j))))))

(defn sq-exp-cov [s2 l xs ys]
  (covariance-mat (partial squared-exponential s2 l) xs ys))

(def prior-mean (repeat (count test-xs) 0.0))

(def prior-cov (sq-exp-cov 1 1 test-xs test-xs))

(sample-multivariate-gaussian prior-mean prior-cov)

(defn vizard-pts [xs ys col]
  (map (fn [x y] {:x x :y y :col col}) xs ys))

(defn line-data [xs mean cov num-samples label]
  (flatten
   (conj
    (for [i (range num-samples)]
      (vizard-pts xs
                  (sample-multivariate-gaussian mean cov)
                  (str label " sample " i)))
    (vizard-pts xs mean (str label " mean")))))

(defn conf-data [xs mean cov]
  (let [std-dev (map #(Math/sqrt %) (m/diagonal cov))]
    (map (fn [x m s]
           {:x x :y (+ m (* 2 s)) :y2 (- m (* 2 s))})
         xs mean std-dev)))

(start-plot-server!)

(defn plot-gp [line-data conf-data]
  (plot! (-> (plot/vizard {:mark-type :line
                           :color "category20b"}
                          line-data)
             (assoc-in [:data 1]
                       {:name :confidence
                        :values conf-data})
             (assoc-in [:marks 1]
                       {:type :area
                        :from {:data :confidence}
                        :properties
                        {:enter
                         {:x {:scale "x" :field :x}
                          :y {:scale "y" :field :y}
                          :y2 {:scale "y" :field :y2}
                          :interpolate {:value :monotone}
                          :fill {:value "#666"}}
                         :update {:fillOpacity {:value 0.25}}}}))))

(let [prior-data (line-data test-xs prior-mean prior-cov 3 "prior")
      prior-conf-data (conf-data test-xs prior-mean prior-cov)]
  (plot-gp prior-data prior-conf-data))

(defn abs-exponential [sigma2 lambda x y]
  (* sigma2 (Math/exp (* -0.5 (Math/abs (/ (- x y) lambda))))))

(defn abs-exp-cov [s2 l xs ys]
  (covariance-mat (partial abs-exponential s2 l) xs ys))

(def abs-prior-cov (abs-exp-cov 1 1 test-xs test-xs))

(let [prior-data (line-data test-xs prior-mean abs-prior-cov 3 "prior")
      prior-conf-data (conf-data test-xs prior-mean abs-prior-cov)]
  (plot-gp prior-data prior-conf-data))
