(ns transport.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [transport.actor :as actor]
            [transport.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Transport"})
    (store/register-vehicle! st {:vehicle-id "V-1" :client-id "client-1"
                                 :name "vehicle-042"
                                 :max-passenger-capacity 40
                                 :pre-trip-inspection-passed? true})
    st))

(deftest commits-a-within-capacity-inspected-dispatch
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-route-dispatch :stake :low
                 :vehicle-id "V-1" :passenger-count 30}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-capacity-dispatch
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-route-dispatch :stake :low
                 :vehicle-id "V-1" :passenger-count 90}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-over-capacity-dispatch-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-over-capacity-dispatch :stake :low
                 :vehicle-id "V-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
