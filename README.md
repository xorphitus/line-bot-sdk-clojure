# line-bot-sdk-clojure

LINE BOT SDK for Clojure.

[![Clojars Project](https://img.shields.io/clojars/v/line-bot-sdk-clojure.svg)](https://clojars.org/line-bot-sdk-clojure)

## Usage

* `deflineevents`: LINE Event handlers dispatcher DSL
* `validate-signature`: Validate a request wheather it is sent by LINE or not
* `reply`: Reply messages to a user

### Simple example for Ring/Compojure

```
(ns clojure-line-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre :refer [error info]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :as jetty]
            [line-bot-sdk-clojure.core :refer :all]))

(def line-channel-token (env :line-channel-token))
(def line-channel-secret (env :line-channel-secret))

(deflineevents app-lineevents
   (MESSAGE [event]
            (reply {:to (get-in event [:source :userId])
                    :reply-token (:replyToken event)
                    :messages [{:type "text"
                                :text (get-in event [:message :text]}]
                    :channel-token line-channel-token})))
   (ELSE [event]
         (info (str "unknown event: " event))))

(defroutes app-routes
  (POST "/linebot/callback" {body :body headers :headers}
    (let [content (slurp body)]
      (if (validate-signature content (get headers "x-line-signature") line-channel-secret)
        (app-lineevents content)
        {:status 400
         :headers {}
         :body "bad request"}))))

(def app
  (wrap-defaults app-routes (assoc-in api-defaults
                                      [:params :urlencoded] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))

```

## License

Copyright © 2016 xorphitus

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
