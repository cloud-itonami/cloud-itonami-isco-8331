(ns transport.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [transport.store :as store]
            [transport.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Transport"})
    (store/register-vehicle! st {:vehicle-id "V-1" :client-id "client-1"
                                 :name "vehicle-042"
                                 :max-passenger-capacity 40
                                 :pre-trip-inspection-passed? true})
    st))

(defn- dispatch-op [count]
  {:op :approve-route-dispatch :effect :propose :vehicle-id "V-1"
   :passenger-count count :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-capacity-and-inspected
  (let [st (fresh-store)
        v (governor/check req {} (dispatch-op 30) st)]
    (is (:ok? v))))

(deftest ok-at-exact-capacity-boundary
  (testing "the passenger-capacity ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (dispatch-op 40) st)]
      (is (:ok? v)))))

(deftest hard-on-passenger-count-exceeds-capacity
  (testing "dispatching beyond the vehicle's registered passenger-capacity ceiling is an overcrowded dispatch, not efficient scheduling"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (dispatch-op 90) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :passenger-count-exceeds-capacity (:rule %)) (:violations v))))))

(deftest hard-on-pre-trip-inspection-not-passed
  (testing "dispatching a route without a passed pre-trip inspection is an unsafe departure, not efficient service"
    (let [st (store/mem-store)]
      (store/register-client! st {:client-id "client-1" :name "Kobo Transport"})
      (store/register-vehicle! st {:vehicle-id "V-1" :client-id "client-1"
                                   :name "vehicle-042"
                                   :max-passenger-capacity 40
                                   :pre-trip-inspection-passed? false})
      (let [v (governor/check req {} (assoc (dispatch-op 30) :confidence 0.99) st)]
        (is (:hard? v))
        (is (some #(= :pre-trip-inspection-not-passed (:rule %)) (:violations v)))))))

(deftest hard-on-unknown-vehicle
  (let [st (fresh-store)
        v (governor/check req {} (assoc (dispatch-op 30) :vehicle-id "V-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-vehicle (:rule %)) (:violations v)))))

(deftest hard-on-foreign-vehicle
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (dispatch-op 30) st)]
      (is (:hard? v))
      (is (some #(= :vehicle-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (dispatch-op 30) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (dispatch-op 30) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-over-capacity-dispatch-even-at-high-confidence
  (testing "no route dispatch above the vehicle's registered passenger-capacity ceiling without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-over-capacity-dispatch :effect :propose
                                    :vehicle-id "V-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-emergency-route-deviation-even-at-high-confidence
  (testing "deviating from a dispatched route during an emergency always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-emergency-route-deviation :effect :propose
                                    :vehicle-id "V-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (dispatch-op 30) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
