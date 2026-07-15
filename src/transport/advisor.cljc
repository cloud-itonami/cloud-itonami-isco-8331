(ns transport.advisor
  "Transport Advisor — the advisor named in this repository's README,
  proposing a passenger-transport operation (dispatch a route, approve
  an over-capacity dispatch, approve an emergency route deviation)
  from a route request, vehicle inspection and passenger manifest.
  Swappable mock/llm; the advisor ONLY proposes — `transport.governor`
  checks the passenger-capacity ceiling and pre-trip-inspection
  completion independently and always escalates over-capacity-
  dispatch and emergency-route-deviation decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-route-dispatch|:approve-over-capacity-dispatch|:approve-emergency-route-deviation
               :effect :propose :vehicle-id str :passenger-count
               number :stake kw :confidence n :rationale str}. The
  passenger-capacity ceiling and pre-trip-inspection state live on the
  registered vehicle record itself (see `transport.store`), not on
  the proposal.")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake vehicle-id passenger-count] :as request}]
  {:op op
   :effect :propose
   :vehicle-id vehicle-id
   :passenger-count passenger-count
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a passenger-transport advisor. Given a request, propose an
   :op, the :vehicle-id and :passenger-count, an honest :confidence
   and a :stake. Never propose a passenger count beyond the vehicle's
   registered capacity ceiling, or a route dispatch for a vehicle
   whose pre-trip inspection hasn't passed — the governor checks both
   against the registered vehicle record. Over-capacity dispatches and
   emergency route deviations always require human sign-off regardless
   of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
