(ns ctia.lib.metrics.console
  (:require [clj-momo.lib.metrics.console :as console]
            [puppetlabs.trapperkeeper.core :as tk]))

(defn start! [get-in-config]
  (let [{:keys [enabled interval]}
        (get-in-config [:ctia :metrics :console])]
    (when enabled
      (console/start interval))))

(defprotocol ConsoleMetricsService)

(tk/defservice console-metrics-service
  ConsoleMetricsService
  [[:ConfigService get-in-config]]
  (start [this context]
         (start! get-in-config)
         context))
