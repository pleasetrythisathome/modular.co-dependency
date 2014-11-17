(ns tangrammer.co-dependency-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [tangrammer.co-dependency :refer (co-using start-with-co-dependency)]))


(defrecord ComponentA [state])

(defn component-a [] (->ComponentA "state A"))

(defrecord ComponentB [state a])

(defn component-b [] (map->ComponentB {:state "state B"}))

(defrecord ComponentC [state a b])

(defn component-c [] (map->ComponentC {:state "state C"}))

(defrecord ComponentD [state my-c b])

(defn component-d [] (map->ComponentD {:state "state D"}))

(defrecord ComponentE [state])

(defn component-e [] (map->ComponentE {:state "state E"}))

(defrecord System1 [d a e c b]  ; deliberately scrambled order
  component/Lifecycle
  (start [this]
    (component/start-system this))
  (stop [this]
    (component/stop-system this)))

(defn system-1 []
  (map->System1 {:a (-> (component-a)
                        (co-using [:b])
                        (co-using [:c])
                        (co-using [:d]))
                 :b (-> (component-b)
                        (component/using [:a]))
                 :c (-> (component-c)
                        (component/using [:a :b]))
                 :d (-> (component-d)
                        (component/using {:b :b :my-c :c}))
                 :e (component-e)}))

(deftest basic-test
  (testing ":b depends on :a, :a co-depends on :b"
    (let [co-s (start-with-co-dependency (system-1))]
      (is (= (:state (:a co-s)) (:state (:a ((-> co-s :a :b)))) "state A"))
      (is (= (:state (:a co-s)) (:state (:a ((-> co-s :a :c )))) "state A"))
      (is (= (:state (:a co-s)) (:state (:a (:my-c ((-> co-s :a :d))))) "state A")))))