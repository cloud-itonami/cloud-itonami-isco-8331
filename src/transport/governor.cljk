(ns transport.governor
  "PassengerTransportGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every route dispatch an advisor may propose for a vehicle. The
  governor never dispatches hardware itself and never dispatches a
  route above the vehicle's registered passenger-capacity ceiling.
  Modeled on cloud-itonami-isco-4311's bookkeeping.governor. Task
  twist: a proposed passenger count is an arithmetic ceiling against
  the vehicle's registered passenger-capacity ceiling, and a route
  cannot be dispatched until the vehicle's pre-trip inspection has
  passed.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the charter group/school/event
                                organizer must be registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never dispatches a route above the
                                registered passenger-capacity ceiling;
                                it only gates what the advisor may
                                dispatch).
    3. vehicle basis          — a route-dispatch proposal must cite a
                                REGISTERED vehicle belonging to this
                                client.
    4. passenger-capacity ceiling — the proposed passenger count must
                                not exceed the vehicle's registered
                                `:max-passenger-capacity` (dispatching
                                beyond the vehicle's registered
                                capacity ceiling is an overcrowded
                                dispatch, not efficient scheduling).
    5. pre-trip-inspection passed — the vehicle must have
                                `:pre-trip-inspection-passed?` true
                                before any route can be dispatched
                                (dispatching without a passed
                                inspection is an unsafe departure, not
                                efficient service).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-over-capacity-dispatch (no route dispatch above
                                the vehicle's registered passenger-
                                capacity ceiling without the governor
                                gate).
    7. :op :approve-emergency-route-deviation (deviating from a
                                dispatched route during an emergency
                                always requires human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [transport.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-over-capacity-dispatch
                                     :approve-emergency-route-deviation})

(defn- hard-violations [{:keys [request proposal]} client-record v]
  (let [{:keys [op passenger-count]} proposal
        dispatch? (= :approve-route-dispatch op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は登録定員超過の配車を直接実行しない）"})

      (and dispatch? (nil? v))
      (conj {:rule :unknown-vehicle :detail "未登録 vehicle への配車提案は不可"})

      (and dispatch? v (not= (:client-id v) (:client-id request)))
      (conj {:rule :vehicle-wrong-client :detail "vehicle が別 client のもの"})

      (and dispatch? v (number? passenger-count) (> passenger-count (:max-passenger-capacity v)))
      (conj {:rule :passenger-count-exceeds-capacity
             :detail (str "乗客数 " passenger-count " > 登録済み定員 "
                          (:max-passenger-capacity v) "（登録定員を超える配車は過密配車であって効率的な運行計画ではない）")})

      (and dispatch? v (not (:pre-trip-inspection-passed? v)))
      (conj {:rule :pre-trip-inspection-not-passed
             :detail "出発前点検未合格の vehicle の配車は安全でない出発であって効率的サービスではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `transport.store/Store`. Pure — never mutates
  the store, never dispatches a route above the registered passenger-
  capacity ceiling."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        v (some->> (:vehicle-id proposal) (store/vehicle store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record v)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
