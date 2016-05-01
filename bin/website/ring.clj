(ns ringtest
  (:require
   [ring.adapter.jetty :as jetty]))

(defn user-agent-as-json
  "A handler that returns the User-Agent header as a JSON
       response with an appropriate Content-Type"
  [req]
  {:body (str "{\"user-agent\": \"" (get-in req [:headers "user-agent"]) "\"}")
   :headers {"Content-Type" "application/json"}
   :status 200})
(defn -main []
  ;; Run the server on port 3000
  (jetty/run-jetty user-agent-as-json {:port 3000})
  )

