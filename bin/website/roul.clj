(ns website.roul)

(require '[roul.random :as rr])

; Get a random integer between 0 and 10
(rand-int 10)    ; Default Clojure
(rr/rand-int 10) ; Roul

; Get a random integer between 10 and 20
(+ 10 (rand-int 10)) ; Default Clojure
(rr/rand-int 10 20)  ; Roul

; Get a random element from a weighted collection

; Returns coffee roughly 80% of the time, tea 15%, and soda 5%.
(rr/rand-nth-weighted {:coffee 0.80 :tea 0.15 :soda 0.05})

; Returns cats roughly twice as often as boots.
(rr/rand-nth-weighted [[:boots 14] [:cats 28]])
